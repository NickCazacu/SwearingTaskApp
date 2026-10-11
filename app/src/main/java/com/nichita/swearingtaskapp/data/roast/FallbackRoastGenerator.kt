package com.nichita.swearingtaskapp.data.roast

import android.util.Log
import com.nichita.swearingtaskapp.domain.model.Roast
import com.nichita.swearingtaskapp.domain.model.RoastContext
import com.nichita.swearingtaskapp.domain.repository.SettingsRepository
import com.nichita.swearingtaskapp.domain.roast.RoastGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** LLM first (if enabled and configured), local templates on any failure. */
class FallbackRoastGenerator @Inject constructor(
    private val llm: LlmRoastGenerator,
    private val templates: TemplateRoastGenerator,
    private val settingsRepository: SettingsRepository,
) : RoastGenerator {

    override suspend fun roast(context: RoastContext): Roast = generate { roast(context) }

    override suspend fun praise(context: RoastContext): Roast = generate { praise(context) }

    private suspend fun generate(block: suspend RoastGenerator.() -> Roast): Roast {
        if (llm.isConfigured && settingsRepository.settings.first().useAi) {
            try {
                return llm.block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "LLM roast failed, using templates: ${e.message}")
            }
        }
        return templates.block()
    }

    private companion object {
        const val TAG = "FallbackRoastGenerator"
    }
}
