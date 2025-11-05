package com.example.sumdays.statistics

// 나무 한 그루에 해당하는 월별 전체 통계 클래스
data class MonthStatistics(
    val year: Int,
    val month: Int,
    val monthTitle: String, // 예: "2025년 9월"
    val weekSummaries: List<WeekSummary>, // 밑에서부터 쌓이는 초록 블록들
    val monthSummary: MonthSummary? = null // 맨 위의 포도 (null일 수 있음)
)