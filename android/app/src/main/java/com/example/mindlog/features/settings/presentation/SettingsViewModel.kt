// path: android/app/src/main/java/com/example/mindlog/features/settings/presentation/SettingsViewModel.kt
package com.example.mindlog.features.settings.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.core.common.Result
import com.example.mindlog.core.model.UserInfo
import com.example.mindlog.features.auth.domain.repository.AuthRepository
import com.example.mindlog.features.settings.domain.usecase.GetUserInfoUseCase
import com.example.mindlog.features.settings.domain.usecase.UpdateUserInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val updateUserInfoUseCase: UpdateUserInfoUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _userInfo = MutableLiveData<Result<UserInfo>>()
    val userInfo: LiveData<Result<UserInfo>> = _userInfo

    private val _updateResult = MutableSharedFlow<Result<String>>()
    val updateResult = _updateResult.asSharedFlow()

    private val _logoutEvent = MutableSharedFlow<Result<Unit>>()
    val logoutEvent = _logoutEvent.asSharedFlow()

    fun loadUserInfo() {
        viewModelScope.launch {
            try {
                val info = getUserInfoUseCase()
                _userInfo.value = Result.Success(info)
            } catch (e: Exception) {
                _userInfo.value = Result.Error(message = e.message ?: "유저 정보를 불러오는데 실패했습니다.")
            }
        }
    }

    fun updateProfile(
        username: String,
        gender: String?,
        birthdate: String?,
        appearance: String?
    ) {
        viewModelScope.launch {
            try {
                updateUserInfoUseCase(
                    username = username,
                    gender = gender,
                    birthdate = birthdate,
                    appearance = appearance
                )
                _updateResult.emit(Result.Success("프로필이 저장되었습니다."))
                loadUserInfo()
            } catch (e: Exception) {
                _updateResult.emit(Result.Error(message = e.message ?: "프로필 저장 실패"))
            }
        }
    }

    fun updatePassword(newPassword: String) {
        viewModelScope.launch {
            try {
                updateUserInfoUseCase(password = newPassword)
                _updateResult.emit(Result.Success("비밀번호가 변경되었습니다."))
            } catch (e: Exception) {
                _updateResult.emit(Result.Error(message = e.message ?: "비밀번호 변경 실패"))
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                when (val result = authRepository.logout()) {
                    is Result.Success -> {

                        if (result.data) {
                            _logoutEvent.emit(Result.Success(Unit))
                        } else {
                            _logoutEvent.emit(Result.Error(message = "로그아웃에 실패했습니다."))
                        }
                    }
                    is Result.Error -> {
                        _logoutEvent.emit(result)
                    }
                }
            } catch (e: Exception) {
                _logoutEvent.emit(Result.Error(message = e.message ?: "오류가 발생했습니다."))
            }
        }
    }
}
