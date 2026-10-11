package com.nichita.swearingtaskapp.data.roast

import com.nichita.swearingtaskapp.data.roast.RoastTemplates.Companion.DONE
import com.nichita.swearingtaskapp.data.roast.RoastTemplates.Companion.HOURS
import com.nichita.swearingtaskapp.data.roast.RoastTemplates.Companion.TASK
import com.nichita.swearingtaskapp.data.roast.RoastTemplates.Companion.TOTAL
import com.nichita.swearingtaskapp.domain.model.Roast
import com.nichita.swearingtaskapp.domain.model.RoastContext
import com.nichita.swearingtaskapp.domain.model.RoastSource
import com.nichita.swearingtaskapp.domain.roast.RoastGenerator
import kotlin.random.Random

/**
 * Offline generator. Picks a random phrase for the level and fills placeholders. Phrases whose
 * placeholders would read badly (no task to mention, "0 ч.") are skipped when possible.
 */
class TemplateRoastGenerator(
    private val templates: Lazy<RoastTemplates>,
    private val random: Random = Random.Default,
) : RoastGenerator {

    override suspend fun roast(context: RoastContext): Roast =
        Roast(
            text = pick(templates.value.roastPhrases(context.level, context.vulgarity), context, context.pendingTasks),
            source = RoastSource.TEMPLATE,
        )

    override suspend fun praise(context: RoastContext): Roast =
        Roast(
            text = pick(templates.value.praisePhrases(context.vulgarity), context, context.doneTasks),
            source = RoastSource.TEMPLATE,
        )

    private fun pick(phrases: List<String>, context: RoastContext, taskPool: List<String>): String {
        val task = taskPool.randomOrNull(random)
        val suitable = phrases.filter { phrase ->
            (task != null || TASK !in phrase) && (context.idleHours > 0 || HOURS !in phrase)
        }
        val phrase = suitable.ifEmpty { phrases }.random(random)
        return fill(phrase, context, task)
    }

    private fun fill(phrase: String, context: RoastContext, task: String?): String =
        phrase
            .replace(TASK, task?.let(::shorten) ?: FALLBACK_TASK)
            .replace(HOURS, context.idleHours.toString())
            .replace(DONE, context.doneCount.toString())
            .replace(TOTAL, context.totalCount.toString())

    private fun shorten(title: String): String =
        if (title.length <= MAX_TASK_LENGTH) title else title.take(MAX_TASK_LENGTH - 1).trimEnd() + "…"

    private companion object {
        const val MAX_TASK_LENGTH = 40
        const val FALLBACK_TASK = "задачи"
    }
}
