package com.example.mindlog.core.model

data class UserInfo(
    val id: Int,
    val loginId: String,
    val username: String,
    val gender: String?,
    val birthdate: String?,
    val appearance: String?
)