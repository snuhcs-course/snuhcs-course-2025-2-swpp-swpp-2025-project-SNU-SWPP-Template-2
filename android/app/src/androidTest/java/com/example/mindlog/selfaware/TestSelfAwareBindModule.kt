package com.example.mindlog.selfaware

import com.example.mindlog.features.selfaware.di.SelfAwareBindModule
import com.example.mindlog.features.selfaware.domain.repository.SelfAwareRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [SelfAwareBindModule::class]
)
abstract class TestSelfAwareBindModule {
    @Binds @Singleton
    abstract fun bindSelfAwareRepository(impl: TestSelfAwareRepository): SelfAwareRepository
}