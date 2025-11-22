package com.example.mindlog.features.auth.data.dto

import com.google.gson.annotations.SerializedName

data class SignupRequest(
    @SerializedName("login_id") val loginId: String,
    @SerializedName("password") val password: String,
    @SerializedName("username") val username: String
)

data class LoginRequest(
    @SerializedName("login_id") val loginId: String,
    @SerializedName("password") val password: String
)

data class RefreshTokenRequest(
    @SerializedName("refreshToken") val refreshToken: String
)
