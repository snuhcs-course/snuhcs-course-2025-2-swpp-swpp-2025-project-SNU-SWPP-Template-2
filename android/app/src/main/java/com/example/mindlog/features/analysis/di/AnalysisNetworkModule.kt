package com.example.mindlog.features.analysis.di

import com.example.mindlog.features.analysis.data.api.AnalysisApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnalysisNetworkModule {
    @Provides @Singleton
    fun provideAnalysisApi(retrofit: Retrofit): AnalysisApi =
        retrofit.create(AnalysisApi::class.java)
}