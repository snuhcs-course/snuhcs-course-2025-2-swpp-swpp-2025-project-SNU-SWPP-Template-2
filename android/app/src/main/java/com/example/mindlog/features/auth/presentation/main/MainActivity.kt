package com.example.mindlog.features.auth.presentation.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mindlog.core.common.SystemUiHelper
import com.example.mindlog.core.network.NetworkModule
import com.example.mindlog.databinding.ActivityMainBinding
import com.example.mindlog.features.auth.data.api.AuthApi
import com.example.mindlog.features.auth.data.api.RefreshApi
import com.example.mindlog.features.auth.data.network.AuthInterceptor
import com.example.mindlog.features.auth.data.network.TokenAuthenticator
import com.example.mindlog.features.auth.data.repository.AuthRepositoryImpl
import com.example.mindlog.features.auth.domain.repository.AuthRepository
import com.example.mindlog.features.auth.presentation.login.LoginActivity
import com.example.mindlog.features.auth.presentation.signup.SignupActivity
import com.example.mindlog.features.auth.util.TokenManager
import com.example.mindlog.features.home.presentation.HomeActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemUiHelper.hideSystemUI(this)

        lifecycleScope.launch {
            checkAutoLogin()
        }
    }

    private suspend fun checkAutoLogin() {
        val access = tokenManager.getAccessToken()
        val refresh = tokenManager.getRefreshToken()

        when {
            access.isNullOrEmpty() || refresh.isNullOrEmpty() -> {
                // 토큰 없음 → 로그인 화면으로
                goToLogin()
            }
            tokenManager.isAccessTokenExpired() -> {
                // 토큰 만료 → refresh 시도
                refresh.let { token ->
                    if (authRepository.refresh(token)) {
                        goToJournal()
                    } else {
                        tokenManager.clearTokens()
                        goToLogin()
                    }
                }
            }
            else -> {
                if (authRepository.verify()) {
                    goToJournal()
                } else {
                    tokenManager.clearTokens()
                    goToLogin()
                }
            }
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun goToJournal() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            SystemUiHelper.hideSystemUI(this)
        }
    }
}
