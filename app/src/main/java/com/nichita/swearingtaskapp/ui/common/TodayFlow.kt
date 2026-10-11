package com.nichita.swearingtaskapp.ui.common

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.LocalDate

/** Emits the current date and re-emits after midnight while the screen stays open. */
fun todayFlow(clock: Clock): Flow<LocalDate> = flow {
    while (true) {
        emit(LocalDate.now(clock))
        delay(60_000)
    }
}.distinctUntilChanged()
