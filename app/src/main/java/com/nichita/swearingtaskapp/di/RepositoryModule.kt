package com.nichita.swearingtaskapp.di

import com.nichita.swearingtaskapp.data.repository.TaskRepositoryImpl
import com.nichita.swearingtaskapp.data.settings.SettingsRepositoryImpl
import com.nichita.swearingtaskapp.domain.repository.SettingsRepository
import com.nichita.swearingtaskapp.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
