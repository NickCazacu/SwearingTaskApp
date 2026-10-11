package com.nichita.swearingtaskapp.di

import android.content.Context
import com.nichita.swearingtaskapp.data.roast.FallbackRoastGenerator
import com.nichita.swearingtaskapp.data.roast.RoastTemplates
import com.nichita.swearingtaskapp.data.roast.TemplateRoastGenerator
import com.nichita.swearingtaskapp.domain.roast.RoastGenerator
import com.nichita.swearingtaskapp.domain.roast.RoastNotifier
import com.nichita.swearingtaskapp.worker.AndroidRoastNotifier
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RoastModule {

    @Binds
    abstract fun bindRoastGenerator(impl: FallbackRoastGenerator): RoastGenerator

    @Binds
    abstract fun bindRoastNotifier(impl: AndroidRoastNotifier): RoastNotifier

    companion object {
        @Provides
        @Singleton
        fun provideTemplateRoastGenerator(@ApplicationContext context: Context): TemplateRoastGenerator =
            TemplateRoastGenerator(
                templates = lazy {
                    context.assets.open(RoastTemplates.ASSET_NAME).bufferedReader().use { it.readText() }
                        .let(RoastTemplates::parse)
                },
            )
    }
}
