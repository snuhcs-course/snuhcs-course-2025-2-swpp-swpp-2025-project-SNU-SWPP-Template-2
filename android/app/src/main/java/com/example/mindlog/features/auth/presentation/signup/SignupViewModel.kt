package com.example.mindlog.features.auth.presentation.signup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import com.example.mindlog.features.auth.domain.usecase.SignupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val signupUseCase: SignupUseCase
): ViewModel() {
    val signupResult = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<String?>()

    fun signup(loginId: String, password: String, username: String) {
        viewModelScope.launch {
            try {
                val result = signupUseCase(loginId, password, username)
                signupResult.value = result.isSuccess
            } catch (e: Exception) {
                errorMessage.value = e.message
            }
        }
    }
}
