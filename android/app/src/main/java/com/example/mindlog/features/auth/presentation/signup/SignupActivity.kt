package com.example.mindlog.features.auth.presentation.signup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.mindlog.core.common.SystemUiHelper
import com.example.mindlog.databinding.ActivitySignupBinding
import com.example.mindlog.core.network.NetworkModule
import com.example.mindlog.features.auth.data.api.AuthApi
import com.example.mindlog.features.auth.data.api.RefreshApi
import com.example.mindlog.features.auth.data.network.AuthInterceptor
import com.example.mindlog.features.auth.data.network.TokenAuthenticator
import com.example.mindlog.features.auth.data.repository.AuthRepositoryImpl
import com.example.mindlog.features.auth.domain.usecase.SignupUseCase
import com.example.mindlog.features.auth.util.TokenManager
import com.example.mindlog.features.auth.presentation.login.LoginActivity
import com.example.mindlog.features.auth.presentation.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val viewModel: SignupViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemUiHelper.hideSystemUI(this)

        binding.etUsername.setOnFocusChangeListener { _, hasFocus ->
            binding.etUsername.hint = if (hasFocus) "" else "비밀번호를 입력하세요"
        }
        binding.etLoginId.setOnFocusChangeListener { _, hasFocus ->
            binding.etLoginId.hint = if (hasFocus) "" else "아이디를 입력하세요"
        }
        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            binding.etPassword.hint = if (hasFocus) "" else "비밀번호를 입력하세요"
        }
        binding.etConfirmPassword.setOnFocusChangeListener { _, hasFocus ->
            binding.etConfirmPassword.hint = if (hasFocus) "" else "비밀번호를 입력하세요"
        }

        // 🔹 회원가입 버튼 클릭
        binding.btnSignup.setOnClickListener {
            val username = binding.etUsername.text.toString()
            val id = binding.etLoginId.text.toString()
            val pw = binding.etPassword.text.toString()
            val confirm = binding.etConfirmPassword.text.toString()

            if (id.isBlank() || pw.isBlank() || username.isBlank()) {
                Toast.makeText(this, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pw != confirm) {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.signup(id, pw, username)
        }

        // 🔹 로그인 화면 이동
        binding.tvGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // 🔹 회원가입 결과 관찰
        viewModel.signupResult.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "회원가입 성공! 자동 로그인 중...", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            } else {
                Toast.makeText(this, "회원가입 실패. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.errorMessage.observe(this) { msg ->
            msg?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            SystemUiHelper.hideSystemUI(this)
        }
    }
}
