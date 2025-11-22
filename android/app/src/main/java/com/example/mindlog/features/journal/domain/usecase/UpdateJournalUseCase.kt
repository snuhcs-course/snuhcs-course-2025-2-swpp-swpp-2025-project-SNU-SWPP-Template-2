package com.example.mindlog.features.journal.domain.usecase

import com.example.mindlog.features.journal.data.dto.UpdateJournalRequest
import com.example.mindlog.features.journal.domain.repository.JournalRepository
import javax.inject.Inject

/**
 * '일기 수정' 비즈니스 로직을 처리하는 UseCase.
 * 원본 데이터와 비교하여 변경된 필드만 요청에 포함시킨다.
 */
class UpdateJournalUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(
        journalId: Int,
        originalTitle: String,
        originalContent: String,
        originalGratitude: String,
        newTitle: String,
        newContent: String,
        newGratitude: String
    ) {
        // 원본과 비교하여 변경된 값만 DTO에 할당, 아니면 null 할당
        val titleToUpdate = if (originalTitle != newTitle) newTitle else null
        val contentToUpdate = if (originalContent != newContent) newContent else null
        val gratitudeToUpdate = if (originalGratitude != newGratitude) newGratitude else null

        // 아무것도 변경되지 않았으면 요청을 보내지 않음
        if (titleToUpdate == null && contentToUpdate == null && gratitudeToUpdate == null) {
            return // 변경사항 없으면 함수 종료
        }

        val request = UpdateJournalRequest(
            title = titleToUpdate,
            content = contentToUpdate,
            gratitude = gratitudeToUpdate
        )

        repository.updateJournal(
            journalId = journalId,
            request = request
        )
    }
}
