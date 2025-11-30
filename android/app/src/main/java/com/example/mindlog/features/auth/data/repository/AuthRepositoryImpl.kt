package com.example.mindlog.features.auth.data.repository

import android.content.Context
import android.util.Log
import com.example.mindlog.core.common.Result
import com.example.mindlog.core.common.toResult
import com.example.mindlog.features.auth.data.api.*
import com.example.mindlog.features.auth.data.dto.LoginRequest
import com.example.mindlog.features.auth.data.dto.RefreshTokenRequest
import com.example.mindlog.features.auth.data.dto.SignupRequest
import com.example.mindlog.features.auth.data.dto.TokenResponse
import com.example.mindlog.features.auth.domain.repository.AuthRepository
import com.example.mindlog.features.auth.util.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Named

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    @Named("refreshApi") private val refreshApi: RefreshApi,
    private val tokenManager: TokenManager
) : AuthRepository {

    override fun isLoggedInFlow(): Flow<Boolean> = tokenManager.isLoggedInFlow()

    override suspend fun signup(
        loginId: String,
        password: String,
        username: String,
        gender: String,
        birthDate: LocalDate
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val request = SignupRequest(
                loginId, password, username, gender, birthDate.toString()
            )
            val res: TokenResponse = authApi.signup(request)
            tokenManager.saveTokens(res.access, res.refresh)
            true
        }.toResult()
    }

    override suspend fun login(
        loginId: String,
        password: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val res: TokenResponse = authApi.login(LoginRequest(loginId, password))
            tokenManager.saveTokens(res.access, res.refresh)
            true
        }.toResult()
    }

    override suspend fun refresh(): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val refresh = tokenManager.getRefreshToken() ?: throw IllegalStateException("Refresh token missing")
            val res: TokenResponse = refreshApi.refresh(RefreshTokenRequest(refresh))
            tokenManager.saveTokens(res.access, res.refresh)
            true
        }.toResult()
    }

    override suspend fun verify(): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val access = tokenManager.getAccessToken() ?: throw IllegalStateException("Refresh token missing")
            val res = authApi.verify("Bearer $access")
            true
        }.toResult()
    }

    override suspend fun logout(): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val access = tokenManager.getAccessToken()
            if (!access.isNullOrBlank()) {
                authApi.logout("Bearer $access")
            }
            tokenManager.clearTokens()
            true
        }.toResult()
    }
}