package com.example.mindlog.features.auth.data.network

import com.example.mindlog.features.auth.data.api.RefreshApi
import com.example.mindlog.features.auth.data.dto.RefreshTokenRequest
import com.example.mindlog.features.auth.util.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Named
import kotlin.concurrent.Volatile

class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    @Named("refreshApi") private val refreshApi: RefreshApi
) : Authenticator {

    @Volatile private var refreshing = false
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse != null) return null
        val refresh = tokenManager.getRefreshToken() ?: return null

        synchronized(lock) {
            if (refreshing) return null
            refreshing = true
        }

        return try {
            val res = runBlocking {
                refreshApi.refresh(RefreshTokenRequest(refresh))
            }

            val newAccess = res.access ?: return null
            val newRefresh = res.refresh ?: refresh
            tokenManager.saveTokens(newAccess, newRefresh)

            response.request.newBuilder()
                .header("Authorization", "Bearer $newAccess")
                .build()
        } catch (_: Exception) {
            null
        } finally {
            refreshing = false
        }
    }
}