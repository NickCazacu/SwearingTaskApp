package com.nichita.swearingtaskapp.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    /** Not a singleton: picks up the current default zone, so a timezone change is respected. */
    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
