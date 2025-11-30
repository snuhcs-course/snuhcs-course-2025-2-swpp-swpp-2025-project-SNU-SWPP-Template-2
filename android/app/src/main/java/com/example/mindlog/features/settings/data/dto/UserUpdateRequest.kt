package com.example.mindlog.features.settings.data.dto

import com.google.gson.annotations.SerializedName

data class UserUpdateRequest(
    @SerializedName("password") val password: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("birthdate") val birthdate: String? = null,
    @SerializedName("appearance") val appearance: String? = null
)
