package com.example.mindlog.settings

import com.example.mindlog.features.settings.di.SettingsModule
import com.example.mindlog.features.settings.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [SettingsModule::class] // 실제 앱의 SettingsModule을 대체
)
abstract class TestSettingsBindModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: TestSettingsRepository): SettingsRepository
}
