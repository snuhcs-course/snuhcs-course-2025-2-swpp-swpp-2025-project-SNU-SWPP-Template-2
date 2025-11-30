package com.example.mindlog.features.journal.domain.usecase

import com.example.mindlog.core.model.JournalEntry
import com.example.mindlog.core.model.PagedResult
import com.example.mindlog.features.journal.domain.repository.JournalRepository
import javax.inject.Inject

/**
 * '일기 목록 조회' 비즈니스 로직을 처리하는 UseCase.
 */
class GetJournalUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(limit: Int, cursor: Int?): PagedResult<JournalEntry> {
        return repository.getJournals(limit = limit, cursor = cursor)
    }
}
