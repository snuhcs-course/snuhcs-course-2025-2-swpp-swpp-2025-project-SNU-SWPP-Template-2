package com.example.mindlog.auth

import com.example.mindlog.core.common.Result
import com.example.mindlog.features.auth.domain.repository.AuthRepository
import com.example.mindlog.features.auth.domain.usecase.LoginUseCase
import com.example.mindlog.features.auth.domain.usecase.SignupUseCase
import com.example.mindlog.features.auth.domain.usecase.RreshTokenUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import java.time.LocalDate

class AuthUseCasesTest {

    private lateinit var repo: AuthRepository
    private lateinit var loginUseCase: LoginUseCase
    private lateinit var signupUseCase: SignupUseCase
    private lateinit var refreshUseCase: RreshTokenUseCase

    @Before
    fun setUp() {
        repo = mock(AuthRepository::class.java)
        loginUseCase = LoginUseCase(repo)
        signupUseCase = SignupUseCase(repo)
        refreshUseCase = RreshTokenUseCase(repo)
    }

    @Test
    fun `loginUseCase calls repo and returns success`() = runBlocking {
        // given
        `when`(repo.login("id", "pw")).thenReturn(Result.Success(true))

        // when
        val result = loginUseCase("id", "pw")

        // then
        verify(repo).login("id", "pw")
        assertTrue(result is Result.Success && result.data)
    }

    @Test
    fun `signupUseCase calls repo and returns success`() = runBlocking {
        // given
        val date = LocalDate.parse("2000-01-01")
        `when`(repo.signup("id", "pw", "user", "M", date))
            .thenReturn(Result.Success(true))

        // when
        val result = signupUseCase("id", "pw", "user", "M", date)

        // then
        verify(repo).signup("id", "pw", "user", "M", date)
        assertTrue(result is Result.Success && result.data)
    }

    @Test
    fun `refreshUseCase calls repo and returns success`() = runBlocking {
        // given
        `when`(repo.refresh()).thenReturn(Result.Success(true))

        // when
        val result = refreshUseCase()

        // then
        verify(repo).refresh()
        assertTrue(result is Result.Success && result.data)
    }
}