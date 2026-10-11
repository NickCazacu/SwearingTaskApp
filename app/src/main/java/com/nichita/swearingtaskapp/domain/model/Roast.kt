package com.nichita.swearingtaskapp.domain.model

/** Input for phrase generation. Mirrors the proxy request body. */
data class RoastContext(
    val level: RoastLevel,
    val pendingTasks: List<String>,
    val doneTasks: List<String>,
    val idleHours: Int,
    val doneCount: Int,
    val totalCount: Int,
    val vulgarity: Vulgarity = Vulgarity.CLEAN,
)

enum class RoastSource { AI, TEMPLATE }

data class Roast(val text: String, val source: RoastSource)
