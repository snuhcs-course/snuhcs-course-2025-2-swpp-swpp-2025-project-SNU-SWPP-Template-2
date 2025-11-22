package com.example.mindlog.features.journal.domain.usecase

import com.example.mindlog.features.journal.domain.repository.JournalRepository
import javax.inject.Inject

class UploadJournalImageUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(
        journalId: Int,
        imageBytes: ByteArray,
        contentType: String,
        fileName: String
    ) {
        repository.uploadJournalImage(
            journalId = journalId,
            imageBytes = imageBytes,
            contentType = contentType,
            fileName = fileName
        )
    }
}
