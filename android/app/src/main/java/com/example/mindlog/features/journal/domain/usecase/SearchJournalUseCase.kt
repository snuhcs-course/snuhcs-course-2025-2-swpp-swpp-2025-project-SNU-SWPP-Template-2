package com.example.mindlog.features.journal.domain.usecase

import com.example.mindlog.core.model.JournalEntry
import com.example.mindlog.core.model.PagedResult
import com.example.mindlog.features.journal.domain.repository.JournalRepository
import javax.inject.Inject

class SearchJournalsUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(
        startDate: String?,
        endDate: String?,
        title: String?,
        limit: Int,
        cursor: Int?
    ): PagedResult<JournalEntry> {
        return repository.searchJournals(startDate, endDate, title, limit, cursor)
    }
}
