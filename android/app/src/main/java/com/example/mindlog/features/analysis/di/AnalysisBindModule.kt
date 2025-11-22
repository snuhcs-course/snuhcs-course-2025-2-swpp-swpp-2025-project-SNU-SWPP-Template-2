package com.example.mindlog.features.analysis.di

import com.example.mindlog.features.analysis.data.repository.AnalysisRepositoryImpl
import com.example.mindlog.features.analysis.domain.repository.AnalysisRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalysisBindModule {
    @Binds @Singleton
    abstract fun bindAnalysisRepository(impl: AnalysisRepositoryImpl): AnalysisRepository
}