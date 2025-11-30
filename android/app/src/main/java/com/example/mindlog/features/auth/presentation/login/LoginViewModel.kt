package com.example.mindlog.features.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import com.example.mindlog.core.common.Result
import com.example.mindlog.core.dispatcher.DispatcherProvider
import com.example.mindlog.features.auth.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val dispatcher: DispatcherProvider     // ✅ DispatcherProvider 주입
) : ViewModel() {

    val loginResult = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<String?>()

    fun login(loginId: String, password: String) {
        viewModelScope.launch(dispatcher.io) {    // ✅ IO 디스패처에서 API 호출
            when (val result = loginUseCase(loginId, password)) {
                is Result.Success -> {
                    withContext(dispatcher.main) { // ✅ UI 업데이트는 Main 디스패처
                        loginResult.value = result.data
                        errorMessage.value = null
                    }
                }
                is Result.Error -> {
                    withContext(dispatcher.main) {
                        loginResult.value = false
                        errorMessage.value =
                            result.message ?: "로그인 중 오류가 발생했습니다 (${result.code ?: "unknown"})"
                    }
                }
            }
        }
    }
}