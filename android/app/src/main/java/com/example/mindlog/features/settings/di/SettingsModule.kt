package com.example.mindlog.features.settings.di

import com.example.mindlog.features.settings.data.api.SettingsApi
import com.example.mindlog.features.settings.data.repository.SettingsRepositoryImpl
import com.example.mindlog.features.settings.domain.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {

    @Provides
    @Singleton
    fun provideSettingsApi(retrofit: Retrofit): SettingsApi {
        return retrofit.create(SettingsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        api: SettingsApi
    ): SettingsRepository {
        return SettingsRepositoryImpl(api)
    }
}
