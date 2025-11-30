package com.example.mindlog.features.auth.presentation.signup

import android.util.Log
import com.example.mindlog.core.common.Result
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.core.dispatcher.DispatcherProvider
import com.example.mindlog.features.auth.domain.usecase.SignupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import java.time.LocalDate


@HiltViewModel
class SignupViewModel @Inject constructor(
    private val signupUseCase: SignupUseCase,
    private val dispatcher: DispatcherProvider
) : ViewModel() {

    val signupResult = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<String?>()

    fun signup(
        loginId: String,
        password: String,
        username: String,
        gender: String,
        birthDate: LocalDate
    ) {
        viewModelScope.launch(dispatcher.io) {
            when (val result = signupUseCase(loginId, password, username, gender, birthDate)) {
                is Result.Success -> {
                    withContext(dispatcher.main) {
                        signupResult.value = result.data
                        errorMessage.value = null
                    }
                }
                is Result.Error -> {
                    Log.d("SignupViewModel", "$result")
                    withContext(dispatcher.main) {
                        signupResult.value = false
                        errorMessage.value = result.message ?: "회원가입 중 오류 발생"
                    }
                }
            }
        }
    }
}