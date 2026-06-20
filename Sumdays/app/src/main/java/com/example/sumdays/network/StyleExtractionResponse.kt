package com.example.sumdays.network

import com.example.sumdays.data.style.StylePrompt

data class StyleExtractionResponse(
    // 서버 응답에 'success' 필드가 없거나 파싱 오류 발생 시 기본값은 false로 설정
    val success: Boolean = false,

    // 데이터 필드는 서버가 실패 시 누락할 수 있으므로 Nullable 처리
    val style_vector: List<Float>?,
    val style_examples: List<String>?,
    val style_prompt: StylePrompt?,

    val message: String? = null
)