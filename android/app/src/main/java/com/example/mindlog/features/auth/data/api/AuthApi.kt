package com.example.mindlog.features.auth.data.api

import com.example.mindlog.features.auth.data.dto.LoginRequest
import com.example.mindlog.features.auth.data.dto.SignupRequest
import com.example.mindlog.features.auth.data.dto.TokenResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST


interface AuthApi {
    @Headers("No-Auth: true")
    @POST("auth/signup")
    suspend fun signup(@Body signupRequest: SignupRequest): TokenResponse

    @Headers("No-Auth: true")
    @POST("auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): TokenResponse

    @POST("auth/verify")
    suspend fun verify(@Header("Authorization") bearerToken: String): Unit

    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") bearerToken: String): Unit
}
