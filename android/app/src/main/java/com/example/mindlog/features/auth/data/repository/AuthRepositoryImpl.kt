package com.example.mindlog.features.auth.data.repository

import android.content.Context
import android.util.Log
import com.example.mindlog.features.auth.data.api.*
import com.example.mindlog.features.auth.data.dto.LoginRequest
import com.example.mindlog.features.auth.data.dto.RefreshTokenRequest
import com.example.mindlog.features.auth.data.dto.SignupRequest
import com.example.mindlog.features.auth.domain.repository.AuthRepository
import com.example.mindlog.features.auth.util.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import retrofit2.HttpException
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
        username: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val resp = authApi.signup(SignupRequest(loginId, password, username)).execute()
            if (!resp.isSuccessful) return@withContext false

            // 서버가 회원가입 시 토큰을 줄 수도/안 줄 수도 있음 → 있으면 저장
            resp.body()?.data?.let { tokenPair ->
                val access = tokenPair.access
                val refresh = tokenPair.refresh
                if (!access.isNullOrBlank()) {
                    tokenManager.saveTokens(access, refresh)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun login(
        loginId: String,
        password: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val resp = authApi.login(LoginRequest(loginId, password)).execute()
            if (!resp.isSuccessful) return@withContext false

            val body = resp.body() ?: return@withContext false
            val access = body.data.access ?: return@withContext false
            val refresh = body.data.refresh
            tokenManager.saveTokens(access, refresh)
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun refresh(refresh: String): Boolean = withContext(Dispatchers.IO) {
        val currentRefresh = tokenManager.getRefreshToken() ?: return@withContext false
        try {
            val resp = refreshApi.refresh(RefreshTokenRequest(currentRefresh)).execute()
            if (!resp.isSuccessful) return@withContext false

            val body = resp.body() ?: return@withContext false
            val newAccess = body.data.access ?: return@withContext false
            val newRefresh = body.data.refresh ?: currentRefresh
            tokenManager.saveTokens(newAccess, newRefresh)
            true
        } catch (_: HttpException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun verify(): Boolean = withContext(Dispatchers.IO) {
        val access = tokenManager.getAccessToken() ?: return@withContext false
        try {
            // verify는 Authorization을 직접 넘겨야 하므로 Bearer 작성
            val resp = authApi.verify("Bearer $access").execute()
            resp.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun logout(): Boolean = withContext(Dispatchers.IO) {
        try {
            // 서버 알림은 실패해도 로컬 토큰은 반드시 삭제
            val access = tokenManager.getAccessToken()
            if (!access.isNullOrBlank()) {
                kotlin.runCatching {
                    authApi.logout("Bearer $access").execute()
                }
            }
            tokenManager.clearTokens()
            true
        } catch (_: Exception) {
            // 그래도 토큰은 비운다 (로그아웃 관점에서 성공)
            tokenManager.clearTokens()
            true
        }
    }
}