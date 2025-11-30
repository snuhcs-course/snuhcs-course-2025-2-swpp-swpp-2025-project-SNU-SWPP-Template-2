package com.example.mindlog.features.journal.domain.usecase

import com.example.mindlog.features.journal.domain.repository.JournalRepository
import javax.inject.Inject

class GenerateImageUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(style: String, content: String): String {
        return repository.generateImage(style, content)
    }
}
