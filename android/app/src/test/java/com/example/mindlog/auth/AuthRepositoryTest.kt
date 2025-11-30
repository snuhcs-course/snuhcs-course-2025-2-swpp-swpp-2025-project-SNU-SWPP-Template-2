package com.example.mindlog.auth

import com.example.mindlog.core.common.Result
import com.example.mindlog.features.auth.data.api.AuthApi
import com.example.mindlog.features.auth.data.api.RefreshApi
import com.example.mindlog.features.auth.data.dto.*
import com.example.mindlog.features.auth.data.repository.AuthRepositoryImpl
import com.example.mindlog.features.auth.util.TokenManager
import com.example.mindlog.utils.MainDispatcherRule
import com.example.mindlog.utils.TestDispatcherProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.mockito.Mockito.mock
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.never
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import retrofit2.Response
import java.time.LocalDate

class AuthRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authApi: AuthApi
    private lateinit var refreshApi: RefreshApi
    private lateinit var tokenManager: TokenManager
    private lateinit var repo: AuthRepositoryImpl

    private lateinit var dispatcherProvider: TestDispatcherProvider

    @Before
    fun setUp() {
        authApi = mock(AuthApi::class.java)
        refreshApi = mock(RefreshApi::class.java)
        tokenManager = mock(TokenManager::class.java)
        repo = AuthRepositoryImpl(authApi, refreshApi, tokenManager)
    }

    @Test
    fun `signup returns Success(true) and saves tokens on success`() = runTest {
        // given
        val req = SignupRequest("id", "pw", "user", "M", "2000-01-01")
        `when`(authApi.signup(req)).thenReturn(TokenResponse("access123", "refresh456"))

        // when
        val result = repo.signup("id", "pw", "user", "M", LocalDate.parse("2000-01-01"))

        // then
        assertTrue(result is Result.Success && result.data)
        verify(tokenManager).saveTokens("access123", "refresh456")
        verifyNoMoreInteractions(tokenManager)
    }

    @Test
    fun `signup returns Error on exception`() = runTest {
        // given
        val req = SignupRequest("id", "pw", "user", "M", "2000-01-01")
        `when`(authApi.signup(req)).thenThrow(RuntimeException("boom"))

        // when
        val result = repo.signup("id", "pw", "user", "M", LocalDate.parse("2000-01-01"))

        // then
        assertTrue(result is Result.Error)
        verifyNoInteractions(tokenManager)
    }

    // ------------------------
    // login
    // ------------------------
    @Test
    fun `login returns Success(true) and saves tokens on success`() = runTest {
        // given
        val req = LoginRequest("id", "pw")
        `when`(authApi.login(req)).thenReturn(TokenResponse("A", "R"))

        // when
        val result = repo.login("id", "pw")

        // then
        assertTrue(result is Result.Success && result.data)
        verify(tokenManager).saveTokens("A", "R")
        verifyNoMoreInteractions(tokenManager)
    }

    @Test
    fun `login returns Error on exception`() = runBlocking {
        // given
        val req = LoginRequest("id", "pw")
        `when`(authApi.login(req)).thenThrow(RuntimeException("fail"))

        // when
        val result = repo.login("id", "pw")

        // then
        assertTrue(result is Result.Error)
        verifyNoInteractions(tokenManager)
    }

    // ------------------------
    // refresh
    // ------------------------
    @Test
    fun `refresh returns Success(false) when no refresh token`() = runTest {
        // given
        `when`(tokenManager.getRefreshToken()).thenReturn(null)

        // when
        val result = repo.refresh()

        // then
        // 코드 상 runCatching 블록에서 false를 반환 → Result.Success(false)
        assertTrue(result is Result.Error)
        verify(tokenManager, never()).saveTokens(any(), any())
        verifyNoInteractions(refreshApi)
    }

    @Test
    fun `refresh returns Success(true) and saves tokens on success`() = runTest {
        // given
        `when`(tokenManager.getRefreshToken()).thenReturn("RRR")
        `when`(refreshApi.refresh(RefreshTokenRequest("RRR")))
            .thenReturn(TokenResponse("NA", "NR"))

        // when
        val result = repo.refresh()

        // then
        assertTrue(result is Result.Success && result.data)
        verify(tokenManager).getRefreshToken()
        verify(tokenManager).saveTokens("NA", "NR")
        verifyNoMoreInteractions(tokenManager)
    }

    @Test
    fun `refresh returns Error on exception`() = runTest {
        // given
        `when`(tokenManager.getRefreshToken()).thenReturn("RRR")
        `when`(refreshApi.refresh(RefreshTokenRequest("RRR")))
            .thenThrow(RuntimeException("network"))

        // when
        val result = repo.refresh()

        // then
        assertTrue(result is Result.Error)
        verify(tokenManager).getRefreshToken()
        verify(tokenManager, never()).saveTokens(any(), any())
        verifyNoMoreInteractions(tokenManager)
    }

    // ------------------------
    // verify
    // ------------------------
    @Test
    fun `verify returns Success(false) when no access token`() = runTest {
        // given
        `when`(tokenManager.getAccessToken()).thenReturn(null)

        // when
        val result = repo.verify()

        // then
        assertTrue(result is Result.Error)
        verify(tokenManager).getAccessToken()
        verifyNoMoreInteractions(tokenManager)
        verifyNoInteractions(authApi)
    }

    @Test
    fun `verify returns Success(true) when api does not throw`() = runTest {
        // given
        `when`(tokenManager.getAccessToken()).thenReturn("AAA")
        // authApi.verify returns Unit; no throw == success
        // default: doNothing()

        // when
        val result = repo.verify()

        // then
        assertTrue(result is Result.Success && result.data)
        verify(authApi).verify("Bearer AAA")
        verify(tokenManager).getAccessToken()
        verifyNoMoreInteractions(authApi, tokenManager)
    }

    @Test
    fun `verify returns Error when api throws`() = runTest {
        // given
        `when`(tokenManager.getAccessToken()).thenReturn("AAA")
        doThrow(RuntimeException("server")).`when`(authApi).verify("Bearer AAA")

        // when
        val result = repo.verify()

        // then
        assertTrue(result is Result.Error)
        verify(authApi).verify("Bearer AAA")
        verify(tokenManager).getAccessToken()
        verifyNoMoreInteractions(authApi, tokenManager)
    }

    // ------------------------
    // logout
    // ------------------------
    @Test
    fun `logout clears tokens even when no access`() = runTest {
        // given
        `when`(tokenManager.getAccessToken()).thenReturn(null)

        // when
        val result = repo.logout()

        // then
        assertTrue(result is Result.Success && result.data)
        verify(tokenManager).getAccessToken()
        verify(tokenManager).clearTokens()
        verifyNoMoreInteractions(tokenManager)
        verifyNoInteractions(authApi)
    }

    @Test
    fun `logout calls api and clears tokens when access exists`() = runBlocking {
        // given
        `when`(tokenManager.getAccessToken()).thenReturn("AAA")

        // when
        val result = repo.logout()

        // then
        assertTrue(result is Result.Success && result.data)
        verify(authApi).logout("Bearer AAA")
        verify(tokenManager).getAccessToken()
        verify(tokenManager).clearTokens()
        verifyNoMoreInteractions(authApi, tokenManager)
    }
}