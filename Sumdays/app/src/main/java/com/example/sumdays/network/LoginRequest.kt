package com.example.sumdays.network

// 서버로 보낼 로그인 요청 데이터 형식
data class LoginRequest(
    val email: String,
    val password: String
)