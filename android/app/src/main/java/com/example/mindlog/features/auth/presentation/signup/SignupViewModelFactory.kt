package com.example.mindlog.features.auth.presentation.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mindlog.features.auth.domain.usecase.SignupUseCase

class SignupViewModelFactory(private val signupUseCase: SignupUseCase): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SignupViewModel(signupUseCase) as T
    }
}
