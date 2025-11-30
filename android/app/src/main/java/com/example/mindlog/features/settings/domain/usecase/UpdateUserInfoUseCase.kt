package com.example.mindlog.features.settings.domain.usecase

import com.example.mindlog.features.settings.domain.repository.SettingsRepository
import javax.inject.Inject

class UpdateUserInfoUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(
        password: String? = null,
        username: String? = null,
        gender: String? = null,
        birthdate: String? = null,
        appearance: String? = null
    ) {
        repository.updateUserInfo(
            password = password,
            username = username,
            gender = gender,
            birthdate = birthdate,
            appearance = appearance
        )
    }
}
