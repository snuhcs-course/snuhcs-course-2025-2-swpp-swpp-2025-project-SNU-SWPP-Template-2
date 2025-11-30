package com.example.mindlog.core.model

/**
 * 페이징된 API 응답을 UI/Domain 계층에서 사용하기 위한 제네릭 모델 클래스.
 * DTO인 [JournalListResponse]를 대체하여 ViewModel이 DTO에 대한 의존성을 갖지 않도록 한다.
 *
 * @param T 페이징된 데이터의 아이템 타입 (예: JournalEntry)
 * @property items 현재 페이지의 아이템 리스트
 * @property nextCursor 다음 페이지를 로드하기 위한 커서 값. 마지막 페이지인 경우 null.
 */
data class PagedResult<T>(
    val items: List<T>,
    val nextCursor: Int?
)
