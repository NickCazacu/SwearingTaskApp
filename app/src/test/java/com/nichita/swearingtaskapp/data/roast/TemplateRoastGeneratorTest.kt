package com.nichita.swearingtaskapp.data.roast

import com.nichita.swearingtaskapp.domain.model.RoastContext
import com.nichita.swearingtaskapp.domain.model.RoastLevel
import com.nichita.swearingtaskapp.domain.model.RoastSource
import com.nichita.swearingtaskapp.domain.model.Vulgarity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class TemplateRoastGeneratorTest {

    // Unit tests run with the module directory as working directory.
    private val assetTemplates by lazy {
        RoastTemplates.parse(File("src/main/assets/${RoastTemplates.ASSET_NAME}").readText())
    }

    private fun context(
        level: RoastLevel = RoastLevel.MEDIUM,
        pending: List<String> = listOf("диплом"),
        done: List<String> = emptyList(),
        idleHours: Int = 4,
        vulgarity: Vulgarity = Vulgarity.CLEAN,
    ) = RoastContext(
        level = level,
        pendingTasks = pending,
        doneTasks = done,
        idleHours = idleHours,
        doneCount = done.size,
        totalCount = done.size + pending.size,
        vulgarity = vulgarity,
    )

    @Test
    fun `asset has enough phrases per level and praise`() {
        RoastLevel.entries.forEach { level ->
            val count = assetTemplates.byLevel.getValue(level).size
            assertTrue("$level has only $count phrases", count >= 25)
        }
        assertTrue(assetTemplates.praise.size >= 10)
        RoastLevel.entries.forEach { level ->
            val count = assetTemplates.explicitByLevel[level].orEmpty().size
            assertTrue("$level explicit has only $count phrases", count >= 20)
        }
        assertTrue(assetTemplates.explicitPraise.size >= 8)
        RoastLevel.entries.forEach { level ->
            val count = assetTemplates.personalByLevel[level].orEmpty().size
            assertTrue("$level personal has only $count phrases", count >= 15)
        }
        assertTrue(assetTemplates.personalPraise.size >= 6)
    }

    @Test
    fun `asset uses only known placeholders and has no duplicates`() {
        val all = assetTemplates.byLevel.values.flatten() + assetTemplates.praise +
            assetTemplates.explicitByLevel.values.flatten() + assetTemplates.explicitPraise +
            assetTemplates.personalByLevel.values.flatten() + assetTemplates.personalPraise
        val placeholder = Regex("\\{[^}]*}")
        all.forEach { phrase ->
            placeholder.findAll(phrase).forEach { match ->
                assertTrue("Unknown placeholder in: $phrase", match.value in RoastTemplates.PLACEHOLDERS)
            }
        }
        assertEquals(all.size, all.toSet().size)
    }

    @Test
    fun `roast fills every placeholder`() = runTest {
        val generator = TemplateRoastGenerator(lazyOf(assetTemplates), Random(42))
        repeat(200) {
            val level = RoastLevel.entries[it % RoastLevel.entries.size]
            val roast = generator.roast(context(level = level))
            assertEquals(RoastSource.TEMPLATE, roast.source)
            assertFalse(roast.text, roast.text.contains('{'))
        }
    }

    @Test
    fun `uses the phrase list of the requested level`() = runTest {
        val templates = RoastTemplates(
            byLevel = mapOf(
                RoastLevel.SOFT to listOf("soft {task}"),
                RoastLevel.MEDIUM to listOf("medium {done}/{total}"),
                RoastLevel.HARD to listOf("hard {hours}"),
            ),
            praise = listOf("praise {task}"),
        )
        val generator = TemplateRoastGenerator(lazyOf(templates), Random(1))
        assertEquals("soft диплом", generator.roast(context(level = RoastLevel.SOFT)).text)
        assertEquals("medium 0/1", generator.roast(context(level = RoastLevel.MEDIUM)).text)
        assertEquals("hard 4", generator.roast(context(level = RoastLevel.HARD)).text)
        assertEquals(
            "praise спорт",
            generator.praise(context(pending = emptyList(), done = listOf("спорт"))).text,
        )
    }

    @Test
    fun `vulgarity picks the matching phrase set and falls back to milder ones`() = runTest {
        val templates = RoastTemplates(
            byLevel = RoastLevel.entries.associateWith { listOf("обычная") },
            praise = listOf("молодец"),
            explicitByLevel = mapOf(RoastLevel.HARD to listOf("с матом"), RoastLevel.MEDIUM to listOf("мат medium")),
            explicitPraise = listOf("охуенно"),
            personalByLevel = mapOf(RoastLevel.HARD to listOf("на личности")),
        )
        val generator = TemplateRoastGenerator(lazyOf(templates), Random(3))
        assertEquals("обычная", generator.roast(context(level = RoastLevel.HARD)).text)
        assertEquals("с матом", generator.roast(context(level = RoastLevel.HARD, vulgarity = Vulgarity.PROFANITY)).text)
        assertEquals("на личности", generator.roast(context(level = RoastLevel.HARD, vulgarity = Vulgarity.PERSONAL)).text)
        // No personal set for MEDIUM -> profanity set.
        assertEquals("мат medium", generator.roast(context(level = RoastLevel.MEDIUM, vulgarity = Vulgarity.PERSONAL)).text)
        // No explicit set for SOFT -> regular phrases.
        assertEquals("обычная", generator.roast(context(level = RoastLevel.SOFT, vulgarity = Vulgarity.PERSONAL)).text)
        assertEquals("охуенно", generator.praise(context(vulgarity = Vulgarity.PROFANITY)).text)
        // No personal praise -> profanity praise.
        assertEquals("охуенно", generator.praise(context(vulgarity = Vulgarity.PERSONAL)).text)
        assertEquals("молодец", generator.praise(context()).text)
    }

    @Test
    fun `explicit and personal asset phrases fill every placeholder`() = runTest {
        val generator = TemplateRoastGenerator(lazyOf(assetTemplates), Random(9))
        repeat(150) {
            val level = RoastLevel.entries[it % RoastLevel.entries.size]
            val vulgarity = if (it % 2 == 0) Vulgarity.PROFANITY else Vulgarity.PERSONAL
            val text = generator.roast(context(level = level, vulgarity = vulgarity)).text
            assertFalse(text, text.contains('{'))
        }
    }

    @Test
    fun `skips phrases that need a task or hours when they are missing`() = runTest {
        val templates = RoastTemplates(
            byLevel = RoastLevel.entries.associateWith { listOf("про {task}", "уже {hours} ч.", "просто фраза") },
            praise = listOf("ok"),
        )
        val generator = TemplateRoastGenerator(lazyOf(templates), Random(7))
        repeat(50) {
            val text = generator.roast(context(pending = emptyList(), idleHours = 0)).text
            assertEquals("просто фраза", text)
        }
    }

    @Test
    fun `long task titles are shortened`() = runTest {
        val templates = RoastTemplates(
            byLevel = RoastLevel.entries.associateWith { listOf("{task}") },
            praise = listOf("ok"),
        )
        val generator = TemplateRoastGenerator(lazyOf(templates), Random(0))
        val text = generator.roast(context(pending = listOf("а".repeat(100)))).text
        assertEquals(40, text.length)
        assertTrue(text.endsWith("…"))
    }
}
