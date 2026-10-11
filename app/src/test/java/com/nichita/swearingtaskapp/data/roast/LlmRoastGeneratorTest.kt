package com.nichita.swearingtaskapp.data.roast

import com.nichita.swearingtaskapp.data.remote.ProxyConfig
import com.nichita.swearingtaskapp.data.remote.RoastRequestDto
import com.nichita.swearingtaskapp.domain.model.Roast
import com.nichita.swearingtaskapp.domain.model.RoastContext
import com.nichita.swearingtaskapp.domain.model.RoastLevel
import com.nichita.swearingtaskapp.domain.model.RoastSource
import com.nichita.swearingtaskapp.domain.model.RoastState
import com.nichita.swearingtaskapp.domain.model.UserSettings
import com.nichita.swearingtaskapp.domain.model.Vulgarity
import com.nichita.swearingtaskapp.domain.repository.SettingsRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlin.random.Random

class LlmRoastGeneratorTest {

    private val config = ProxyConfig(baseUrl = "https://proxy.example", appToken = "secret")
    private val context = RoastContext(
        level = RoastLevel.HARD,
        pendingTasks = listOf("диплом"),
        doneTasks = listOf("спорт"),
        idleHours = 7,
        doneCount = 1,
        totalCount = 2,
    )

    private fun client(status: HttpStatusCode, body: String, onRequest: (String, String?, String) -> Unit = { _, _, _ -> }) =
        HttpClient(
            MockEngine { request ->
                onRequest(request.url.toString(), request.headers["X-App-Token"], String(request.body.toByteArray()))
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            },
        ) {
            expectSuccess = true
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

    @Test
    fun `sends contract body and token, parses text`() = runTest {
        var url = ""
        var token: String? = null
        var sent = ""
        val generator = LlmRoastGenerator(
            client(HttpStatusCode.OK, """{"text":"  Хватит рофлить  "}""") { u, t, b -> url = u; token = t; sent = b },
            config,
        )

        val roast = generator.roast(context)

        assertEquals(Roast("Хватит рофлить", RoastSource.AI), roast)
        assertEquals("https://proxy.example/roast", url)
        assertEquals("secret", token)
        assertEquals(
            RoastRequestDto(level = "HARD", tasks = listOf("диплом"), idleHours = 7, doneCount = 1, totalCount = 2),
            Json.decodeFromString<RoastRequestDto>(sent),
        )
    }

    @Test
    fun `sends explicit and personal flags`() = runTest {
        var sent = ""
        val generator = LlmRoastGenerator(client(HttpStatusCode.OK, """{"text":"бля"}""") { _, _, b -> sent = b }, config)

        generator.roast(context.copy(vulgarity = Vulgarity.PROFANITY))
        var dto = Json.decodeFromString<RoastRequestDto>(sent)
        assertEquals(true to false, dto.explicit to dto.personal)

        generator.roast(context.copy(vulgarity = Vulgarity.PERSONAL))
        dto = Json.decodeFromString<RoastRequestDto>(sent)
        assertEquals(true to true, dto.explicit to dto.personal)
    }

    @Test
    fun `praise sends PRAISE level with done tasks`() = runTest {
        var sent = ""
        val generator = LlmRoastGenerator(client(HttpStatusCode.OK, """{"text":"GG"}""") { _, _, b -> sent = b }, config)
        generator.praise(context)
        val dto = Json.decodeFromString<RoastRequestDto>(sent)
        assertEquals("PRAISE", dto.level)
        assertEquals(listOf("спорт"), dto.tasks)
    }

    @Test
    fun `fallback uses templates when proxy returns 502`() = runTest {
        val fallback = FallbackRoastGenerator(
            llm = LlmRoastGenerator(client(HttpStatusCode.BadGateway, """{"error":"upstream"}"""), config),
            templates = templateGenerator(),
            settingsRepository = FakeSettings(UserSettings(useAi = true)),
        )
        assertEquals(Roast("шаблон", RoastSource.TEMPLATE), fallback.roast(context))
    }

    @Test
    fun `fallback uses templates when AI is disabled or not configured`() = runTest {
        var calls = 0
        val okClient = client(HttpStatusCode.OK, """{"text":"ИИ"}""") { _, _, _ -> calls++ }

        val disabled = FallbackRoastGenerator(
            llm = LlmRoastGenerator(okClient, config),
            templates = templateGenerator(),
            settingsRepository = FakeSettings(UserSettings(useAi = false)),
        )
        assertEquals(RoastSource.TEMPLATE, disabled.roast(context).source)

        val notConfigured = FallbackRoastGenerator(
            llm = LlmRoastGenerator(okClient, ProxyConfig(baseUrl = "", appToken = "")),
            templates = templateGenerator(),
            settingsRepository = FakeSettings(UserSettings(useAi = true)),
        )
        assertEquals(RoastSource.TEMPLATE, notConfigured.roast(context).source)
        assertEquals(0, calls)

        val enabled = FallbackRoastGenerator(
            llm = LlmRoastGenerator(okClient, config),
            templates = templateGenerator(),
            settingsRepository = FakeSettings(UserSettings(useAi = true)),
        )
        assertEquals(Roast("ИИ", RoastSource.AI), enabled.roast(context))
    }

    private fun templateGenerator() = TemplateRoastGenerator(
        lazyOf(
            RoastTemplates(
                byLevel = RoastLevel.entries.associateWith { listOf("шаблон") },
                praise = listOf("молодец"),
            ),
        ),
        Random(0),
    )

    private class FakeSettings(settings: UserSettings) : SettingsRepository {
        override val settings: Flow<UserSettings> = MutableStateFlow(settings)
        override val roastState: Flow<RoastState> = flowOf(RoastState())
        override suspend fun setBaseLevel(level: RoastLevel) = Unit
        override suspend fun setCheckIntervalHours(hours: Int) = Unit
        override suspend fun setQuietHours(start: LocalTime, end: LocalTime) = Unit
        override suspend fun setUseAi(enabled: Boolean) = Unit
        override suspend fun setVulgarity(vulgarity: Vulgarity) = Unit
        override suspend fun setPausedOn(date: LocalDate?) = Unit
        override suspend fun markRoasted(at: Instant) = Unit
        override suspend fun markPraised(date: LocalDate) = Unit
        override suspend fun markCarryOverPrompted(date: LocalDate) = Unit
    }
}
