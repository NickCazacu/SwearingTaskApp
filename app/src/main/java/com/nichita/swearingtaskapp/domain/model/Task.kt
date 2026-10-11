package com.nichita.swearingtaskapp.domain.model

import java.time.Instant
import java.time.LocalDate

data class Task(
    val id: Long = 0,
    val title: String,
    val date: LocalDate,
    val isDone: Boolean = false,
    val createdAt: Instant,
    val completedAt: Instant? = null,
)
