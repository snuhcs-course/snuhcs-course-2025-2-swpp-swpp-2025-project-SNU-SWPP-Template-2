package com.example.mindlog.features.auth.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val ACCESS_TOKEN = "access_token"
        private const val REFRESH_TOKEN = "refresh_token"
    }

    // 로그인 여부를 실시간으로 방출하는 StateFlow
    private val loggedInState = MutableStateFlow(isLoggedIn())

    // 토큰 저장
    fun saveTokens(access: String, refresh: String) {
        prefs.edit {
                putString(ACCESS_TOKEN, access)
                .putString(REFRESH_TOKEN, refresh)
            }

        loggedInState.value = isLoggedIn()
    }

    // 토큰 조회
    fun getAccessToken(): String? =
        prefs.getString(ACCESS_TOKEN, null)?.takeIf { it.isNotBlank() }

    fun getRefreshToken(): String? =
        prefs.getString(REFRESH_TOKEN, null)?.takeIf { it.isNotBlank() }

    // 로그인 여부 (토큰 존재)
    fun isLoggedIn(): Boolean = getAccessToken() != null && getRefreshToken() != null

    // JWT 만료 여부 확인
    fun isAccessTokenExpired(): Boolean {
        val token = getAccessToken() ?: return true
        try {
            val parts = token.split(".")
            if (parts.size < 2) return true
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
            val json = JSONObject(payload)
            val exp = json.optLong("exp", 0)
            val now = System.currentTimeMillis() / 1000
            return now >= exp
        } catch (e: Exception) {
            e.printStackTrace()
            return true
        }
    }

    // 토큰 삭제
    fun clearTokens() {
        prefs.edit {clear()}
        loggedInState.value = false
    }

    fun isLoggedInFlow(): StateFlow<Boolean> = loggedInState
}
