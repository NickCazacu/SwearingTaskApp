package com.nichita.swearingtaskapp.domain.model

/** How rough the language may get. Each mode includes the previous one. */
enum class Vulgarity {
    /** No profanity. */
    CLEAN,

    /** Мат aimed at procrastination only (18+). */
    PROFANITY,

    /** Мат plus name-calling aimed at the user's laziness and willpower (18+). */
    PERSONAL;

    val allowsProfanity: Boolean get() = this != CLEAN
    val allowsPersonal: Boolean get() = this == PERSONAL
}
