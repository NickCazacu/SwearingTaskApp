package com.nichita.swearingtaskapp.domain.roast

import com.nichita.swearingtaskapp.domain.model.Roast
import com.nichita.swearingtaskapp.domain.model.RoastContext

interface RoastGenerator {
    /** Phrase that roasts the user for undone tasks at [RoastContext.level]. */
    suspend fun roast(context: RoastContext): Roast

    /** Phrase that praises the user for finishing every task of the day. */
    suspend fun praise(context: RoastContext): Roast
}
