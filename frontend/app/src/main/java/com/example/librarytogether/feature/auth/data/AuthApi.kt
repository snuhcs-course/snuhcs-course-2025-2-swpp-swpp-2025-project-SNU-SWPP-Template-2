package com.example.librarytogether.feature.auth.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>
    @POST("auth/signup")
    suspend fun signUp(@Body body: SignUpRequest): Response<SignUpResponse>
}