package com.example.mindlog.features.journal.di

import com.example.mindlog.features.journal.data.api.JournalApi
import com.example.mindlog.features.journal.data.mapper.JournalMapper
import com.example.mindlog.features.journal.data.repository.JournalRepositoryImpl
import com.example.mindlog.features.journal.domain.repository.JournalRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object JournalModule {

    @Provides
    @Singleton
    fun provideJournalRepository(
        journalApi: JournalApi,
        mapper: JournalMapper
    ): JournalRepository {
        return JournalRepositoryImpl(journalApi, mapper)
    }

    @Provides
    @Singleton
    fun provideJournalApi(retrofit: Retrofit): JournalApi {
        return retrofit.create(JournalApi::class.java)
    }

    @Provides
    @Singleton
    fun provideJournalMapper(): JournalMapper {
        return JournalMapper()
    }
}
