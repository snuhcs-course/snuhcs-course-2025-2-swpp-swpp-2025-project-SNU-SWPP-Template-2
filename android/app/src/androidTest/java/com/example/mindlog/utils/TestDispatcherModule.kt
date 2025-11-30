package com.example.mindlog.utils

import com.example.mindlog.core.dispatcher.DispatcherModule
import com.example.mindlog.core.dispatcher.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DispatcherModule::class]
)
object TestDispatcherModule {
    @Provides @Singleton
    fun provideDispatcherProvider(): DispatcherProvider =
        TestDispatcherProvider()
}