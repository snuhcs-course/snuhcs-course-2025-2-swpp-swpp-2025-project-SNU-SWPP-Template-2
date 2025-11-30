package com.example.mindlog.auth

import com.example.mindlog.features.auth.di.AuthBindModule
import com.example.mindlog.features.auth.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AuthBindModule::class]   // ✅ prod 바인딩 모듈만 교체
)
abstract class TestAuthBindModule {
    @Binds @Singleton
    abstract fun bindAuthRepository(impl: TestAuthRepository): AuthRepository
}