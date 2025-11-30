package com.example.mindlog.auth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.mindlog.core.common.Result
import com.example.mindlog.features.auth.domain.usecase.SignupUseCase
import com.example.mindlog.features.auth.presentation.signup.SignupViewModel
import com.example.mindlog.testutil.getOrAwaitValue
import com.example.mindlog.utils.MainDispatcherRule
import com.example.mindlog.utils.TestDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.mockito.kotlin.whenever
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SignupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var signupUseCase: SignupUseCase
    private lateinit var vm: SignupViewModel

    @Before
    fun setup() {
        val testDispatcherProvider = TestDispatcherProvider(mainDispatcherRule.testDispatcher)

        signupUseCase = mock()

        vm = SignupViewModel(signupUseCase, testDispatcherProvider)
    }

    @Test
    fun `signup success updates signupResult true and clears error`() = runTest {
        // given
        val birth = LocalDate.parse("2000-01-01")

        // 인자 전부를 정확 매칭으로 stub
        whenever(
            signupUseCase.invoke(
                eq("id"),
                eq("pw"),
                eq("user"),
                eq("M"),
                eq(birth)
            )
        ).thenReturn(Result.Success(true))

        // when
        vm.signup(
            loginId = "id",
            password = "pw",
            username = "user",
            gender = "M",
            birthDate = birth
        )
        advanceUntilIdle()

        // then: 인자 정확 검증
        verify(signupUseCase).invoke(
            eq("id"),
            eq("pw"),
            eq("user"),
            eq("M"),
            eq(birth)
        )

        val result = vm.signupResult.getOrAwaitValue()
        val error  = vm.errorMessage.getOrAwaitValue()

        Assert.assertEquals(true, result)
        Assert.assertEquals(null, error)
    }

    @Test
    fun `signup error sets signupResult false and posts error message`() = runTest {
        // given
        val birth = LocalDate.parse("1999-12-31")
        whenever(signupUseCase.invoke("bad", "pw", "user", "F", birth))
            .thenReturn(com.example.mindlog.core.common.Result.Error(code = 400, message = "이미 존재하는 아이디입니다."))

        // when
        vm.signup(
            loginId = "bad",
            password = "pw",
            username = "user",
            gender = "F",
            birthDate = birth
        )
        advanceUntilIdle()

        // then
        verify(signupUseCase).invoke(eq("bad"), eq("pw"), eq("user"), eq("F"), eq(birth))

        val result = vm.signupResult.getOrAwaitValue()
        val error  = vm.errorMessage.getOrAwaitValue()

        Assert.assertEquals(false, result)
        Assert.assertEquals("이미 존재하는 아이디입니다.", error)
    }
}
