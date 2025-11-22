package com.example.mindlog.statistics

import com.example.mindlog.features.statistics.di.StatisticsBindModule
import com.example.mindlog.features.statistics.domain.repository.StatisticsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [StatisticsBindModule::class]
)
abstract class TestStatisticsBindModule {
    @Binds @Singleton
    abstract fun bindStatisticsRepository(impl: TestStatisticsRepository): StatisticsRepository

}