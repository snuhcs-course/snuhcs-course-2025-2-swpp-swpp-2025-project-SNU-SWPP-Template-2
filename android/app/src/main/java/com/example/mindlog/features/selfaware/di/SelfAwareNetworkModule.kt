package com.example.mindlog.features.selfaware.di

import com.example.mindlog.features.selfaware.data.api.SelfAwareApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object SelfAwareNetworkModule {
    @Provides @Singleton
    fun provideSelfAwareApi(retrofit: Retrofit): SelfAwareApi =
        retrofit.create(SelfAwareApi::class.java)
}