package com.example.mindlog.features.auth.presentation.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mindlog.core.common.SystemUiHelper
import com.example.mindlog.core.common.Result
import com.example.mindlog.databinding.ActivityMainBinding
import com.example.mindlog.features.auth.domain.repository.AuthRepository
import com.example.mindlog.features.auth.presentation.login.LoginActivity
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
                goToLogin()
            }
            tokenManager.isAccessTokenExpired() -> {
                // refresh 시도
                when (val result = authRepository.refresh()) {
                    is Result.Success -> {
                        if (result.data) goToJournal()
                        else {
                            tokenManager.clearTokens()
                            goToLogin()
                        }
                    }
                    is Result.Error -> {
                        tokenManager.clearTokens()
                        goToLogin()
                    }
                }
            }
            else -> {
                when (val result = authRepository.verify()) {
                    is Result.Success -> {
                        if (result.data) goToJournal()
                        else {
                            tokenManager.clearTokens()
                            goToLogin()
                        }
                    }
                    is Result.Error -> {
                        tokenManager.clearTokens()
                        goToLogin()
                    }
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
