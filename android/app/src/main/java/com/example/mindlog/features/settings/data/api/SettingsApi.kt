package com.example.mindlog.features.settings.data.api

import com.example.mindlog.features.settings.data.dto.UserResponse
import com.example.mindlog.features.settings.data.dto.UserUpdateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface SettingsApi {
    @GET("user/me")
    suspend fun getUserInfo(): UserResponse

    @PATCH("user/me")
    suspend fun updateUserInfo(@Body request: UserUpdateRequest): Response<String>
}
