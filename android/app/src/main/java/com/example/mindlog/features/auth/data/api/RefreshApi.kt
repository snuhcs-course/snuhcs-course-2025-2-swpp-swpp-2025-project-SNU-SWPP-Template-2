package com.example.mindlog.features.auth.data.api

import com.example.mindlog.features.auth.data.dto.RefreshTokenRequest
import com.example.mindlog.features.auth.data.dto.TokenResponseEnvelope
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface RefreshApi {
    @Headers("No-Auth: true")
    @POST("auth/refresh")
    fun refresh(@Body refreshTokenRequest: RefreshTokenRequest): Call<TokenResponseEnvelope>
}