package com.example.mindlog.features.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import com.example.mindlog.features.auth.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject


@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {
    val loginResult = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<String?>()
    val isLoading = MutableLiveData<Boolean>()

    fun login(loginId: String, password: String) {
        viewModelScope.launch {
            try {
                isLoading.value = true

                val result = loginUseCase(loginId, password)
                loginResult.value = result.isSuccess

            } catch (e: HttpException) {
                errorMessage.value = "서버 오류 (${e.code()})가 발생했습니다."
            } catch (e: IOException) {
                errorMessage.value = "네트워크 연결을 확인해주세요."
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "알 수 없는 오류가 발생했습니다."
            } finally {
                isLoading.value = false
            }
        }
    }
}
