package com.example.mindlog.journal
import com.example.mindlog.features.journal.di.JournalModule
import com.example.mindlog.features.journal.domain.repository.JournalRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [JournalModule::class] // 실제 앱의 JournalModule을 대체
)
abstract class TestJournalBindModule {

    @Binds
    @Singleton
    abstract fun bindJournalRepository(impl: TestJournalRepository): JournalRepository
}
