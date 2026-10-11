package com.nichita.swearingtaskapp.data.roast

import com.nichita.swearingtaskapp.domain.model.RoastLevel
import com.nichita.swearingtaskapp.domain.model.Vulgarity
import kotlinx.serialization.json.Json

/**
 * Phrases from `assets/roasts.json`: one array per [RoastLevel] plus a `PRAISE` array, and
 * optional variants with the `_EXPLICIT` (мат) and `_PERSONAL` (мат + на личности) suffixes.
 * A missing variant falls back to the next milder one.
 */
class RoastTemplates(
    val byLevel: Map<RoastLevel, List<String>>,
    val praise: List<String>,
    val explicitByLevel: Map<RoastLevel, List<String>> = emptyMap(),
    val explicitPraise: List<String> = emptyList(),
    val personalByLevel: Map<RoastLevel, List<String>> = emptyMap(),
    val personalPraise: List<String> = emptyList(),
) {
    fun roastPhrases(level: RoastLevel, vulgarity: Vulgarity): List<String> =
        firstNonEmpty(
            personalByLevel[level].takeIf { vulgarity.allowsPersonal },
            explicitByLevel[level].takeIf { vulgarity.allowsProfanity },
        ) ?: byLevel.getValue(level)

    fun praisePhrases(vulgarity: Vulgarity): List<String> =
        firstNonEmpty(
            personalPraise.takeIf { vulgarity.allowsPersonal },
            explicitPraise.takeIf { vulgarity.allowsProfanity },
        ) ?: praise

    private fun firstNonEmpty(vararg candidates: List<String>?): List<String>? =
        candidates.firstOrNull { !it.isNullOrEmpty() }

    companion object {
        const val ASSET_NAME = "roasts.json"
        const val PRAISE_KEY = "PRAISE"
        const val EXPLICIT_SUFFIX = "_EXPLICIT"
        const val PERSONAL_SUFFIX = "_PERSONAL"

        const val TASK = "{task}"
        const val HOURS = "{hours}"
        const val DONE = "{done}"
        const val TOTAL = "{total}"
        val PLACEHOLDERS = setOf(TASK, HOURS, DONE, TOTAL)

        fun parse(json: String): RoastTemplates {
            val raw = Json.decodeFromString<Map<String, List<String>>>(json)
            val byLevel = RoastLevel.entries.associateWith { level ->
                raw[level.name].orEmpty().also {
                    require(it.isNotEmpty()) { "No templates for $level" }
                }
            }
            val praise = raw[PRAISE_KEY].orEmpty()
            require(praise.isNotEmpty()) { "No praise templates" }
            return RoastTemplates(
                byLevel = byLevel,
                praise = praise,
                explicitByLevel = variant(raw, EXPLICIT_SUFFIX),
                explicitPraise = raw[PRAISE_KEY + EXPLICIT_SUFFIX].orEmpty(),
                personalByLevel = variant(raw, PERSONAL_SUFFIX),
                personalPraise = raw[PRAISE_KEY + PERSONAL_SUFFIX].orEmpty(),
            )
        }

        private fun variant(raw: Map<String, List<String>>, suffix: String): Map<RoastLevel, List<String>> =
            RoastLevel.entries
                .associateWith { raw[it.name + suffix].orEmpty() }
                .filterValues { it.isNotEmpty() }
    }
}
