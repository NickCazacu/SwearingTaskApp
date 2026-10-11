package com.nichita.swearingtaskapp.data.roast

import com.nichita.swearingtaskapp.data.remote.ProxyConfig
import com.nichita.swearingtaskapp.data.remote.RoastRequestDto
import com.nichita.swearingtaskapp.data.remote.RoastResponseDto
import com.nichita.swearingtaskapp.domain.model.Roast
import com.nichita.swearingtaskapp.domain.model.RoastContext
import com.nichita.swearingtaskapp.domain.model.RoastSource
import com.nichita.swearingtaskapp.domain.roast.RoastGenerator
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject

/** Phrases from Claude through the proxy. Throws on any network/HTTP failure (see Fallback). */
class LlmRoastGenerator @Inject constructor(
    private val client: HttpClient,
    private val config: ProxyConfig,
) : RoastGenerator {

    val isConfigured: Boolean get() = config.isConfigured

    override suspend fun roast(context: RoastContext): Roast =
        request(level = context.level.name, tasks = context.pendingTasks, context = context)

    override suspend fun praise(context: RoastContext): Roast =
        request(level = PRAISE_LEVEL, tasks = context.doneTasks, context = context)

    private suspend fun request(level: String, tasks: List<String>, context: RoastContext): Roast {
        check(isConfigured) { "Proxy is not configured" }
        val response: RoastResponseDto = client.post("${config.baseUrl}/roast") {
            header(APP_TOKEN_HEADER, config.appToken)
            contentType(ContentType.Application.Json)
            setBody(
                RoastRequestDto(
                    level = level,
                    tasks = tasks,
                    idleHours = context.idleHours,
                    doneCount = context.doneCount,
                    totalCount = context.totalCount,
                    explicit = context.vulgarity.allowsProfanity,
                    personal = context.vulgarity.allowsPersonal,
                ),
            )
        }.body()
        val text = response.text.trim()
        check(text.isNotEmpty()) { "Empty roast from proxy" }
        return Roast(text = text, source = RoastSource.AI)
    }

    private companion object {
        const val APP_TOKEN_HEADER = "X-App-Token"
        const val PRAISE_LEVEL = "PRAISE"
    }
}
