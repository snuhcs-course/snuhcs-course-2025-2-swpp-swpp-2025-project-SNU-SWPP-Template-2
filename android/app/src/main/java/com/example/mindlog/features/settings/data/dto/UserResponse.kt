package com.example.mindlog.features.settings.data.dto

import com.google.gson.annotations.SerializedName

data class UserResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("login_id") val loginId: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("gender") val gender: String?,
    @SerializedName("birthdate") val birthdate: String?,
    @SerializedName("appearance") val appearance: String?
)
