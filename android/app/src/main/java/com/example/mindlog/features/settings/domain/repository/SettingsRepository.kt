package com.example.mindlog.features.settings.domain.repository

import com.example.mindlog.core.model.UserInfo

interface SettingsRepository {
    suspend fun getUserInfo(): UserInfo

    suspend fun updateUserInfo(
        password: String? = null,
        username: String? = null,
        gender: String? = null,
        birthdate: String? = null,
        appearance: String? = null
    )
}
