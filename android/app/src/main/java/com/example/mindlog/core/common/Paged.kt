package com.example.mindlog.core.common

/**
 * 서버의 페이징 API 결과를 공통 구조로 담는 클래스.
 */
data class Paged<T>(
    val items: List<T>,
    val cursor: Int?,
    val size: Int,
)