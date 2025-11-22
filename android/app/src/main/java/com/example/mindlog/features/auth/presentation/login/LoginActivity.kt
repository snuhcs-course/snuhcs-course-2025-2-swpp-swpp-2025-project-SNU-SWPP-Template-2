package com.example.mindlog.features.auth.presentation.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.mindlog.core.network.NetworkModule
import com.example.mindlog.databinding.ActivityLoginBinding
import com.example.mindlog.features.auth.data.api.AuthApi
import com.example.mindlog.features.auth.data.api.RefreshApi
import com.example.mindlog.features.auth.data.network.AuthInterceptor
import com.example.mindlog.features.auth.data.network.TokenAuthenticator
import com.example.mindlog.features.auth.data.repository.AuthRepositoryImpl
import com.example.mindlog.features.auth.domain.usecase.LoginUseCase
import com.example.mindlog.features.auth.util.TokenManager
import com.example.mindlog.features.auth.presentation.signup.SignupActivity
import com.example.mindlog.features.auth.presentation.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import com.example.mindlog.R
import com.example.mindlog.core.common.SystemUiHelper


@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemUiHelper.hideSystemUI(this)

        binding.btnLogin.setOnClickListener {
            val id = binding.etLoginId.text.toString()
            val password = binding.etPassword.text.toString()

            var isIdEmpty = false
            var isPasswordEmpty = false

            if (id.isEmpty()) {
                binding.etLoginId.setBackgroundResource(R.drawable.edittext_bg_error)
                isIdEmpty = true
            } else {
                binding.etLoginId.setBackgroundResource(R.drawable.edittext_bg)
            }

            // 비밀번호 검증
            if (password.isEmpty()) {
                binding.etPassword.setBackgroundResource(R.drawable.edittext_bg_error)
                isPasswordEmpty = true
            } else {
                binding.etPassword.setBackgroundResource(R.drawable.edittext_bg)
            }

            if (isIdEmpty && isPasswordEmpty) {
                binding.tvError.text = "아이디와 비밀번호를 입력해주세요"
                binding.tvError.visibility = View.VISIBLE
                return@setOnClickListener
            } else if (isIdEmpty) {
                binding.tvError.text = "아이디를 입력해주세요"
                binding.tvError.visibility = View.VISIBLE
                return@setOnClickListener
            } else if (isPasswordEmpty) {
                binding.tvError.text = "비밀번호를 입력해주세요"
                binding.tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            viewModel.login(id, password)
        }

        binding.etLoginId.setOnFocusChangeListener { _, hasFocus ->
            binding.etLoginId.hint = if (hasFocus) "" else "아이디를 입력하세요"
        }
        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            binding.etPassword.hint = if (hasFocus) "" else "비밀번호를 입력하세요"
        }

        binding.tvGoSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        viewModel.loginResult.observe(this, Observer { success ->
            if (success) {
                Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            } else {
                binding.tvError.text = "아이디와 비밀번호를 확인해주세요"
                binding.tvError.visibility = View.VISIBLE
            }
        })

        viewModel.errorMessage.observe(this, Observer { message ->
            message?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        })
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            SystemUiHelper.hideSystemUI(this)
        }
    }
}
