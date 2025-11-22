package com.example.mindlog.features.auth.data.api

import com.example.mindlog.features.auth.data.dto.LoginRequest
import com.example.mindlog.features.auth.data.dto.SignupRequest
import com.example.mindlog.features.auth.data.dto.TokenResponseEnvelope
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST


interface AuthApi {
    @Headers("No-Auth: true")
    @POST("auth/signup")
    fun signup(@Body signupRequest: SignupRequest): Call<TokenResponseEnvelope>

    @Headers("No-Auth: true")
    @POST("auth/login")
    fun login(@Body loginRequest: LoginRequest): Call<TokenResponseEnvelope>

    @POST("auth/verify")
    fun verify(@Header("Authorization") bearerToken: String): Call<Void>

    @POST("auth/logout")
    fun logout(@Header("Authorization") bearerToken: String): Call<Void>
}
