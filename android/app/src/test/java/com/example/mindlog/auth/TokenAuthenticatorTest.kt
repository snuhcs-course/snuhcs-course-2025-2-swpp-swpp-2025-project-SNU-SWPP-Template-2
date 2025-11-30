package com.example.mindlog.features.auth.data.network

import com.example.mindlog.features.auth.data.api.RefreshApi
import com.example.mindlog.features.auth.data.dto.TokenResponse
import com.example.mindlog.features.auth.data.dto.RefreshTokenRequest
import com.example.mindlog.features.auth.util.TokenManager
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TokenAuthenticatorTest {

    private lateinit var tokenManager: TokenManager
    private lateinit var refreshApi: RefreshApi
    private lateinit var authenticator: TokenAuthenticator

    @Before
    fun setup() {
        tokenManager = mock()
        refreshApi = mock()
        authenticator = TokenAuthenticator(tokenManager, refreshApi)
    }

    @Test
    fun `returns null when priorResponse exists`() {
        val response = mock<Response> {
            on { priorResponse } doReturn mock()
        }

        val result = authenticator.authenticate(mock<Route>(), response)
        assertNull(result)
    }

    @Test
    fun `returns null when refresh token missing`() {
        whenever(tokenManager.getRefreshToken()).thenReturn(null)
        val response = mock<Response>()

        val result = authenticator.authenticate(mock<Route>(), response)
        assertNull(result)
    }

    @Test
    fun `returns new request with updated header when refresh succeeds`() {
        val response = mock<Response> {
            on { priorResponse } doReturn null
        }

        whenever(tokenManager.getRefreshToken()).thenReturn("oldRefresh")
        refreshApi.stub {
            onBlocking { refresh(any<RefreshTokenRequest>()) } doReturn
                    TokenResponse(access = "newAccess", refresh = "newRefresh")
        }
        val result = authenticator.authenticate(mock(), response)

        verify(tokenManager).saveTokens("newAccess", "newRefresh")
    }

    @Test
    fun `returns null when refreshApi throws exception`() {
        val response = mock<Response> {
            on { priorResponse } doReturn null
        }

        whenever(tokenManager.getRefreshToken()).thenReturn("refreshToken")
        refreshApi.stub {
            onBlocking { refresh(any()) } doThrow RuntimeException("Network error")
        }
        val result = authenticator.authenticate(mock(), response)
        assertNull(result)
    }

    @Test
    fun `returns null when already refreshing`() {
        authenticator.apply {
            val field = this::class.java.getDeclaredField("refreshing")
            field.isAccessible = true
            field.set(this, true)
        }

        val response = mock<Response>()
        val result = authenticator.authenticate(mock(), response)
        assertNull(result)
    }
}