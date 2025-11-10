package com.example.sumdays.network

import com.google.gson.annotations.SerializedName

// ★★★ STT API 응답 데이터 클래스 추가 ★★★
data class STTResponse(
    val success: Boolean,
    @SerializedName("transcribed_text") val transcribedText: String?,
    val message: String? // 오류 메시지 등을 받을 수 있도록 추가 (선택적)
)