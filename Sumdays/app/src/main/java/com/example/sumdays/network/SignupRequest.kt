package com.example.sumdays.network

import com.google.gson.annotations.SerializedName

// ★★★ 회원가입 관련 데이터 클래스 추가 ★★★
data class SignupRequest(
    val nickname: String,
    val email: String,
    val password: String
)