package com.example.mindlog.settings

import com.example.mindlog.core.model.UserInfo
import com.example.mindlog.features.settings.domain.repository.SettingsRepository
import com.example.mindlog.features.settings.domain.usecase.GetUserInfoUseCase
import com.example.mindlog.features.settings.domain.usecase.UpdateUserInfoUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SettingsUseCasesTest {

    private lateinit var mockRepository: SettingsRepository
    private lateinit var getUserInfoUseCase: GetUserInfoUseCase
    private lateinit var updateUserInfoUseCase: UpdateUserInfoUseCase

    private val dummyUserInfo = UserInfo(
        id = 1, loginId = "id", username = "name",
        gender = "M", birthdate = "2000-01-01", appearance = null
    )

    @Before
    fun setup() {
        mockRepository = mock()
        getUserInfoUseCase = GetUserInfoUseCase(mockRepository)
        updateUserInfoUseCase = UpdateUserInfoUseCase(mockRepository)
    }

    @Test
    fun `GetUserInfoUseCase - repository의 getUserInfo를 호출하고 결과를 반환한다`() = runTest {
        // Given
        whenever(mockRepository.getUserInfo()).thenReturn(dummyUserInfo)

        // When
        val result = getUserInfoUseCase()

        // Then
        verify(mockRepository).getUserInfo()
        assertEquals(dummyUserInfo, result)
    }

    @Test
    fun `UpdateUserInfoUseCase - repository의 updateUserInfo를 올바른 파라미터로 호출한다`() = runTest {
        // Given
        val newName = "New Name"
        val newGender = "F"

        // When
        updateUserInfoUseCase(username = newName, gender = newGender)

        // Then
        verify(mockRepository).updateUserInfo(
            password = null,
            username = newName,
            gender = newGender,
            birthdate = null,
            appearance = null
        )
    }
}
