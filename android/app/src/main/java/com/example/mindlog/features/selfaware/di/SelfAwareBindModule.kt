package com.example.mindlog.features.selfaware.di

import com.example.mindlog.features.selfaware.data.repository.SelfAwareRepositoryImpl
import com.example.mindlog.features.selfaware.domain.repository.SelfAwareRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SelfAwareBindModule {
    @Binds @Singleton
    abstract fun bindSelfAwareRepository(impl: SelfAwareRepositoryImpl): SelfAwareRepository
}