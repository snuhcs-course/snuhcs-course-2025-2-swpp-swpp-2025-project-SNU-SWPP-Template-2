package com.example.mindlog.auth

import com.example.mindlog.core.common.Result
import com.example.mindlog.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class TestAuthRepository @Inject constructor() : AuthRepository {

    private val loggedInState = MutableStateFlow(false)

    // 각 API 호출의 성공 / 실패 여부를 제어하기 위한 플래그들
    var shouldSignupSucceed: Boolean = true
    var shouldLoginSucceed: Boolean = true
    var shouldRefreshSucceed: Boolean = true
    var shouldVerifySucceed: Boolean = true
    var shouldLogoutSucceed: Boolean = true

    override fun isLoggedInFlow(): Flow<Boolean> = loggedInState.asStateFlow()

    override suspend fun signup(
        loginId: String,
        password: String,
        username: String,
        gender: String,
        birthDate: LocalDate
    ): Result<Boolean> {
        return if (shouldSignupSucceed) {
            // 회원가입 성공 시 자동 로그인되었다고 가정
            loggedInState.value = true
            Result.Success(true)
        } else {
            Result.Error(null, "signup failed in TestAuthRepository")
        }
    }

    override suspend fun login(
        loginId: String,
        password: String
    ): Result<Boolean> {
        return if (shouldLoginSucceed) {
            loggedInState.value = true
            Result.Success(true)
        } else {
            loggedInState.value = false
            Result.Error(null, "login failed in TestAuthRepository")
        }
    }

    override suspend fun refresh(): Result<Boolean> {
        return if (shouldRefreshSucceed && loggedInState.value) {
            // 실제 토큰 갱신은 없고, 단순히 성공했다고만 리턴
            Result.Success(true)
        } else {
            Result.Error(null, "refresh failed in TestAuthRepository")
        }
    }

    override suspend fun verify(): Result<Boolean> {
        return if (shouldVerifySucceed && loggedInState.value) {
            Result.Success(true)
        } else {
            Result.Error(null, "verify failed in TestAuthRepository")
        }
    }

    override suspend fun logout(): Result<Boolean> {
        return if (shouldLogoutSucceed) {
            loggedInState.value = false
            Result.Success(true)
        } else {
            Result.Error(null, "logout failed in TestAuthRepository")
        }
    }
}