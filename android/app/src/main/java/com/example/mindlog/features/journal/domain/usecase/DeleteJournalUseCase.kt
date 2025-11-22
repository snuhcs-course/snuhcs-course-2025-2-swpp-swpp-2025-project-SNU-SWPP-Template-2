package com.example.mindlog.features.journal.domain.usecase

import com.example.mindlog.features.journal.domain.repository.JournalRepository
import javax.inject.Inject

/**
 * '일기 삭제' 비즈니스 로직을 처리하는 UseCase.
 */
class DeleteJournalUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(journalId: Int) {
        repository.deleteJournal(journalId = journalId)
    }
}
