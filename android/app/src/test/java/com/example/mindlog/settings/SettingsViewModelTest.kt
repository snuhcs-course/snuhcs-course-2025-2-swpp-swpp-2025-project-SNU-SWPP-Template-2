package com.example.mindlog.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.mindlog.core.common.Result
import com.example.mindlog.core.model.UserInfo
import com.example.mindlog.features.auth.domain.repository.AuthRepository
import com.example.mindlog.features.settings.domain.usecase.GetUserInfoUseCase
import com.example.mindlog.features.settings.domain.usecase.UpdateUserInfoUseCase
import com.example.mindlog.features.settings.presentation.SettingsViewModel
import com.example.mindlog.utils.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class SettingsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getUserInfoUseCase: GetUserInfoUseCase
    private lateinit var updateUserInfoUseCase: UpdateUserInfoUseCase
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: SettingsViewModel

    private val dummyUserInfo = UserInfo(
        id = 1,
        loginId = "testId",
        username = "TestUser",
        gender = "M",
        birthdate = "2000-01-01",
        appearance = "Cool"
    )

    @Before
    fun setup() {
        getUserInfoUseCase = mock()
        updateUserInfoUseCase = mock()
        authRepository = mock()
        viewModel = SettingsViewModel(getUserInfoUseCase, updateUserInfoUseCase, authRepository)
    }

    // --- loadUserInfo ---
    @Test
    fun `loadUserInfo - 성공 시 userInfo LiveData에 데이터를 업데이트한다`() = runTest {
        // Given
        whenever(getUserInfoUseCase.invoke()).thenReturn(dummyUserInfo)

        // When
        viewModel.loadUserInfo()
        advanceUntilIdle()

        // Then
        val result = viewModel.userInfo.value
        assertTrue(result is Result.Success)
        assertEquals(dummyUserInfo, (result as Result.Success).data)
    }

    @Test
    fun `loadUserInfo - 실패 시 Error 상태를 업데이트한다`() = runTest {
        // Given
        val errorMessage = "네트워크 오류"
        whenever(getUserInfoUseCase.invoke()).thenThrow(RuntimeException(errorMessage))

        // When
        viewModel.loadUserInfo()
        advanceUntilIdle()

        // Then
        val result = viewModel.userInfo.value
        assertTrue(result is Result.Error)
        assertEquals(errorMessage, (result as Result.Error).message)
    }

    // --- updateProfile ---
    @Test
    fun `updateProfile - 성공 시 Success 이벤트를 방출하고 정보를 다시 로드한다`() = runTest {
        // Given
        val newUsername = "UpdatedUser"

        // 순서대로: 업데이트 -> 정보 재로딩
        whenever(updateUserInfoUseCase.invoke(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).then { }
        whenever(getUserInfoUseCase.invoke()).thenReturn(dummyUserInfo.copy(username = newUsername))

        val results = mutableListOf<Result<String>>()
        val job = launch { viewModel.updateResult.collect { results.add(it) } }

        // When
        viewModel.updateProfile(newUsername, "M", "2000-01-01", "Cool")
        advanceUntilIdle()
        job.cancel()

        // Then
        // 1. Update 호출 검증
        verify(updateUserInfoUseCase).invoke(username = newUsername, gender = "M", birthdate = "2000-01-01", appearance = "Cool")
        // 2. 성공 메시지 방출 확인
        assertTrue(results.first() is Result.Success)
        assertEquals("프로필이 저장되었습니다.", (results.first() as Result.Success).data)
        // 3. 정보 재로딩 확인
        verify(getUserInfoUseCase).invoke()
    }

    @Test
    fun `updateProfile - 실패 시 Error 이벤트를 방출한다`() = runTest {
        // Given
        val errorMessage = "업데이트 실패"
        whenever(updateUserInfoUseCase.invoke(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenThrow(RuntimeException(errorMessage))

        val results = mutableListOf<Result<String>>()
        val job = launch { viewModel.updateResult.collect { results.add(it) } }

        // When
        viewModel.updateProfile("name", null, null, null)
        advanceUntilIdle()
        job.cancel()

        // Then
        assertTrue(results.first() is Result.Error)
        assertEquals(errorMessage, (results.first() as Result.Error).message)
    }

    // --- updatePassword ---
    @Test
    fun `updatePassword - 성공 시 Success 이벤트를 방출한다`() = runTest {
        // Given
        val newPw = "1234"
        val results = mutableListOf<Result<String>>()
        val job = launch { viewModel.updateResult.collect { results.add(it) } }

        // When
        viewModel.updatePassword(newPw)
        advanceUntilIdle()
        job.cancel()

        // Then
        verify(updateUserInfoUseCase).invoke(password = newPw)
        assertTrue(results.first() is Result.Success)
        assertEquals("비밀번호가 변경되었습니다.", (results.first() as Result.Success).data)
    }

    @Test
    fun `updatePassword - 예외 발생 시 Error 이벤트를 방출한다`() = runTest {
        // Given
        val errorMessage = "비밀번호 변경 중 오류"
        // ✨ [수정됨] 모든 파라미터에 Matcher 명시
        whenever(
            updateUserInfoUseCase.invoke(
                password = any(),
                username = anyOrNull(),
                gender = anyOrNull(),
                birthdate = anyOrNull(),
                appearance = anyOrNull()
            )
        ).thenThrow(RuntimeException(errorMessage))

        val results = mutableListOf<Result<String>>()
        val job = launch { viewModel.updateResult.collect { results.add(it) } }

        // When
        viewModel.updatePassword("1234")
        advanceUntilIdle()
        job.cancel()

        // Then
        assertTrue(results.first() is Result.Error)
        assertEquals(errorMessage, (results.first() as Result.Error).message)
    }

    // --- logout ---
    @Test
    fun `logout - 성공 시 Success 이벤트를 방출한다`() = runTest {
        // Given
        whenever(authRepository.logout()).thenReturn(true)
        val results = mutableListOf<Result<Unit>>()
        val job = launch { viewModel.logoutEvent.collect { results.add(it) } }

        // When
        viewModel.logout()
        advanceUntilIdle()
        job.cancel()

        // Then
        verify(authRepository).logout()
        assertTrue(results.first() is Result.Success)
    }

    @Test
    fun `logout - 실패(false 반환) 시 Error 이벤트를 방출한다`() = runTest {
        // Given
        whenever(authRepository.logout()).thenReturn(false)
        val results = mutableListOf<Result<Unit>>()
        val job = launch { viewModel.logoutEvent.collect { results.add(it) } }

        // When
        viewModel.logout()
        advanceUntilIdle()
        job.cancel()

        // Then
        verify(authRepository).logout()
        assertTrue(results.first() is Result.Error)
        assertEquals("로그아웃에 실패했습니다.", (results.first() as Result.Error).message)
    }

    @Test
    fun `logout - 예외 발생 시 Error 이벤트를 방출한다`() = runTest {
        // Given
        val errorMessage = "네트워크 연결 끊김"
        whenever(authRepository.logout()).thenThrow(RuntimeException(errorMessage))

        val results = mutableListOf<Result<Unit>>()
        val job = launch { viewModel.logoutEvent.collect { results.add(it) } }

        // When
        viewModel.logout()
        advanceUntilIdle()
        job.cancel()

        // Then
        verify(authRepository).logout()
        assertTrue(results.first() is Result.Error)
        assertEquals(errorMessage, (results.first() as Result.Error).message)
    }
}
