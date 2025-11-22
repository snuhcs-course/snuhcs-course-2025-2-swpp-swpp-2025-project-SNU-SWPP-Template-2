package com.example.mindlog.features.auth.domain.usecase

import com.example.mindlog.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

class RefreshTokenUseCase @Inject constructor(
    private val repository: AuthRepository
) {

    suspend operator fun invoke(refresh: String): Result<Boolean> {
        return try {
            val isSuccess = repository.refresh(refresh)
            if (isSuccess) {
                Result.success(isSuccess)
            } else {
                Result.failure(Exception("토큰 갱신 실패: 서버에서 새 토큰을 받지 못했습니다."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
