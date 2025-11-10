package com.example.sumdays.data.style

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_style")
data class UserStyle(
    @PrimaryKey(autoGenerate = true)
    val styleId: Long = 0, // PK: 각 스타일을 식별하는 고유 ID
    val styleName: String, // 사용자가 지정할 이름 (예: '시니컬 스타일', '경쾌한 스타일')
    val styleVector: List<Float>, // AI 스타일 벡터 (768차원)
    val styleExamples: List<String>, // 추출된 예시 문장들
    val stylePrompt: StylePrompt // 상세 분석 결과 객체
)