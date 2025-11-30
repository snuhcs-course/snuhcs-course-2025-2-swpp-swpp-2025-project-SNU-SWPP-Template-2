package com.example.mindlog.features.journal.domain.usecase

import com.example.mindlog.core.model.JournalEntry
import com.example.mindlog.core.model.PagedResult
import com.example.mindlog.features.journal.domain.repository.JournalRepository
import javax.inject.Inject

class SearchByKeywordUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(
        keyword: String,
        limit: Int,
        cursor: Int?
    ): PagedResult<JournalEntry> {
        return repository.searchByKeyword(keyword, limit, cursor)
    }
}