package com.example.mindlog.journal

import com.example.mindlog.core.model.JournalEntry
import com.example.mindlog.core.model.Keyword
import com.example.mindlog.core.model.PagedResult
import com.example.mindlog.features.journal.data.dto.UpdateJournalRequest
import com.example.mindlog.features.journal.domain.repository.JournalRepository
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestJournalRepository @Inject constructor() : JournalRepository {

    // 메모리 DB 역할
    private val journals = mutableListOf<JournalEntry>()
    private var nextId = 1

    override suspend fun createJournal(
        title: String,
        content: String,
        emotions: Map<String, Int>,
        gratitude: String
    ): Int {
        val newEntry = JournalEntry(
            id = nextId++,
            title = title,
            content = content,
            createdAt = Date(),
            imageUrl = null,
            keywords = emptyList(),
            emotions = emptyList(), // 필요시 emotions Map을 List<Emotion>으로 변환 로직 추가 가능
            gratitude = gratitude
        )
        journals.add(0, newEntry) // 최신순 저장
        return newEntry.id
    }

    override suspend fun getJournals(limit: Int, cursor: Int?): PagedResult<JournalEntry> {
        // 단순화된 페이징: 커서 없이 전체 반환하거나, 리스트가 비어있지 않으면 반환
        return PagedResult(items = journals.toList(), nextCursor = null)
    }

    override suspend fun getJournalById(journalId: Int): JournalEntry {
        return journals.find { it.id == journalId }
            ?: throw Exception("Journal not found with id: $journalId")
    }

    override suspend fun updateJournal(journalId: Int, request: UpdateJournalRequest) {
        val index = journals.indexOfFirst { it.id == journalId }
        if (index != -1) {
            val old = journals[index]
            journals[index] = old.copy(
                title = request.title ?: old.title,
                content = request.content ?: old.content,
                gratitude = request.gratitude ?: old.gratitude
            )
        } else {
            throw Exception("Journal not found for update")
        }
    }

    override suspend fun deleteJournal(journalId: Int) {
        val removed = journals.removeIf { it.id == journalId }
        if (!removed) throw Exception("Journal not found for delete")
    }

    // --- 아래 메서드들은 테스트 시나리오에 따라 간단한 Stub 구현 ---

    override suspend fun uploadJournalImage(
        journalId: Int,
        imageBytes: ByteArray,
        contentType: String,
        fileName: String
    ) {
        // 이미지 업로드 성공 시, 해당 일기의 imageUrl 필드 업데이트 시늉
        val index = journals.indexOfFirst { it.id == journalId }
        if (index != -1) {
            val old = journals[index]
            journals[index] = old.copy(imageUrl = "https://fake-s3-url.com/$fileName")
        }
    }

    override suspend fun searchJournals(
        startDate: String?,
        endDate: String?,
        title: String?,
        limit: Int,
        cursor: Int?
    ): PagedResult<JournalEntry> {
        // 간단한 제목 검색 필터링 구현
        val filtered = if (title != null) {
            journals.filter { it.title.contains(title) }
        } else {
            journals
        }
        return PagedResult(items = filtered, nextCursor = null)
    }

    override suspend fun searchByKeyword(
        keyword: String,
        limit: Int,
        cursor: Int?
    ): PagedResult<JournalEntry> {
        // 키워드 검색 필터링 시늉
        val filtered = journals.filter { entry ->
            entry.keywords.any { it.keyword.contains(keyword) }
        }
        return PagedResult(items = filtered, nextCursor = null)
    }

    override suspend fun generateImage(style: String, content: String): String {
        return "fake_base64_image_string"
    }

    override suspend fun extractKeywords(journalId: Int): List<Keyword> {
        // 더미 키워드 생성 및 저장
        val index = journals.indexOfFirst { it.id == journalId }
        val dummyKeywords = listOf(Keyword("테스트키워드", "happy", "요약", 0.9f))

        if (index != -1) {
            val old = journals[index]
            journals[index] = old.copy(keywords = dummyKeywords)
        }
        return dummyKeywords
    }

    // 테스트를 위한 헬퍼 메서드: 데이터 초기화
    fun clear() {
        journals.clear()
        nextId = 1
    }

    // 테스트를 위한 헬퍼 메서드: 데이터 미리 넣기
    fun addDummyData(entry: JournalEntry) {
        journals.add(entry)
        if (entry.id >= nextId) nextId = entry.id + 1
    }
}
