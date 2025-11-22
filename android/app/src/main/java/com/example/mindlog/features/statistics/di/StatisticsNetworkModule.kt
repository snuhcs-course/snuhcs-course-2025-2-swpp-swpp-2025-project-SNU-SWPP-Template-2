package com.example.mindlog.features.statistics.di

import com.example.mindlog.features.statistics.data.api.StatisticsApi
import com.example.mindlog.features.statistics.data.repository.StatisticsRepositoryImpl
import com.example.mindlog.features.statistics.domain.repository.StatisticsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class StatisticsNetworkModule {
    @Provides @Singleton
    fun provideStatisticsApi(retrofit: Retrofit): StatisticsApi =
        retrofit.create(StatisticsApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class StatisticsBindModule {
    @Binds
    @Singleton
    abstract fun bindStatisticsRepository(impl: StatisticsRepositoryImpl): StatisticsRepository
}