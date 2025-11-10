package com.example.sumdays.network

// 서버로부터 받을 로그인 응답 데이터 형식
data class LoginResponse(
    val success: Boolean,
    val message: String,
    val userId: Int?,
    val token: String?,
    val nickname: String?
)
