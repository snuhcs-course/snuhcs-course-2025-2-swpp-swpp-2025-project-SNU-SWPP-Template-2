package com.example.mindlog.features.auth.domain.usecase

import com.example.mindlog.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {

    suspend operator fun invoke(loginId: String, password: String): Result<Boolean> {
        return try {
            val isSuccess = repository.login(loginId, password)
            if (isSuccess) {
                Result.success(isSuccess)
            } else {
                Result.failure(Exception("로그인 실패: 서버에서 토큰을 받지 못했습니다."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
