package com.example.mindlog.auth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.mindlog.core.common.Result
import com.example.mindlog.features.auth.domain.usecase.LoginUseCase
import com.example.mindlog.features.auth.presentation.login.LoginViewModel
import com.example.mindlog.testutil.getOrAwaitValue
import com.example.mindlog.utils.MainDispatcherRule
import com.example.mindlog.utils.TestDispatcherProvider
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.mockito.Mockito.verify
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var loginUseCase: LoginUseCase
    private lateinit var vm: LoginViewModel

    @Before
    fun setup() {
        val testDispatcherProvider = TestDispatcherProvider(mainDispatcherRule.testDispatcher)
        loginUseCase = mock()

        vm = LoginViewModel(
            loginUseCase = loginUseCase,
            dispatcher = testDispatcherProvider
        )
    }

    @Test
    fun `login success updates loginResult true`() = runTest {
        // UseCase를 mock: 성공 반환
        whenever(loginUseCase.invoke("id", "pw"))
            .thenReturn(Result.Success(true))

        vm.login("id", "pw")
        advanceUntilIdle()

        // UseCase가 정확히 호출되었는지 검증
        verify(loginUseCase).invoke("id", "pw")

        // LiveData 검증 (getOrAwaitValue를 쓰고 있다면 그대로 사용)
        assertEquals(true, vm.loginResult.getOrAwaitValue())
        assertNull(vm.errorMessage.getOrAwaitValue())
    }

    @Test
    fun `login failure sets error message`() = runTest {
        // 실패 케이스 스텁
        whenever(loginUseCase.invoke("bad", "pw"))
            .thenReturn(Result.Error(code = 401, message = "아이디 혹은 비밀번호가 올바르지 않습니다."))

        vm.login("bad", "pw")
        advanceUntilIdle()

        assertEquals(false, vm.loginResult.getOrAwaitValue())
        assertEquals(
            "아이디 혹은 비밀번호가 올바르지 않습니다.",
            vm.errorMessage.getOrAwaitValue()
        )
    }
}