package com.example.mindlog.features.journal.domain.usecase

import com.example.mindlog.features.journal.data.dto.JournalResponse
import com.example.mindlog.features.journal.domain.repository.JournalRepository
import javax.inject.Inject

class CreateJournalUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(
        title: String,
        content: String,
        emotions: Map<String, Int>,
        gratitude: String
    ): Int {
        return repository.createJournal(title, content, emotions, gratitude)
    }
}
