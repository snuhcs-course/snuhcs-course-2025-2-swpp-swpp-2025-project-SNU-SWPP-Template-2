package com.example.mindlog.analysis


import com.example.mindlog.features.analysis.di.AnalysisBindModule
import com.example.mindlog.features.analysis.domain.repository.AnalysisRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AnalysisBindModule::class]
)
abstract class TestAnalysisBindModule {
    @Binds @Singleton
    abstract fun bindAnalysisRepository(impl: TestAnalysisRepository): AnalysisRepository

}