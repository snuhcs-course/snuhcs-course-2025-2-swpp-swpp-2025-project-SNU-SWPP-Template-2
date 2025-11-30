package com.example.mindlog.features.settings.domain.usecase

import com.example.mindlog.core.model.UserInfo
import com.example.mindlog.features.settings.domain.repository.SettingsRepository
import javax.inject.Inject

class GetUserInfoUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(): UserInfo {
        return repository.getUserInfo()
    }
}
