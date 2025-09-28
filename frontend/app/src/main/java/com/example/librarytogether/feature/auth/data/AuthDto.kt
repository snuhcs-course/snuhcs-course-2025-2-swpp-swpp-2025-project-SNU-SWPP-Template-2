package com.example.librarytogether.feature.auth.data

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val ok: Boolean,
    val token: String?,
    val message: String? = null
)

data class SignUpRequest(
    val username: String,
    val password: String,
    val email: String
)

data class SignUpResponse(
    val ok: Boolean,
)