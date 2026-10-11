package com.nichita.swearingtaskapp.domain.roast

interface RoastNotifier {
    /** False if notifications are disabled or POST_NOTIFICATIONS is not granted. */
    fun canNotify(): Boolean

    fun showRoast(text: String)

    fun showPraise(text: String)
}
