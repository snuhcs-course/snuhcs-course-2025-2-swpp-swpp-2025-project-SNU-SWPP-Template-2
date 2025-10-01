package com.example.librarytogether.feature.auth

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.librarytogether.R
import com.example.librarytogether.feature.auth.data.AuthApi
import com.example.librarytogether.feature.auth.data.LoginRequest
import com.example.librarytogether.network.AuthManager
import com.example.librarytogether.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnForgot: MaterialButton
    private lateinit var btnGoogle: MaterialButton
    private lateinit var btnKakao: MaterialButton
    private lateinit var btnSignUp: MaterialButton

    private val service: AuthApi by lazy {
        RetrofitClient.getClient(applicationContext).create(AuthApi::class.java)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.log_in)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        email = findViewById(R.id.EmailText)
        password = findViewById(R.id.PasswordText)
        btnLogin = findViewById(R.id.LogInButton)
        btnForgot = findViewById(R.id.ForgotPasswordButton)
        btnGoogle = findViewById(R.id.GoogleLoginButton)
        btnKakao = findViewById(R.id.KakaoLoginButton)
        btnSignUp = findViewById(R.id.SignUpButton)

        btnLogin.setOnClickListener { onClickLogin() }
        btnForgot.setOnClickListener { onClickForgotPassword() }
        btnSignUp.setOnClickListener { onClickSignUp() }
        btnGoogle.setOnClickListener { onClickGoogleLogin() }
        btnKakao.setOnClickListener { onClickKakaoLogin() }
    }

    private fun onClickLogin() {
        val id = email.text.toString().trim()
        val pw = password.text.toString()

        if (id.isEmpty() || pw.isEmpty()) {
            Toast.makeText(this, "아이디/비밀번호를 입력하세요", Toast.LENGTH_SHORT).show()
            return
        }

        btnLogin.isEnabled = false

        lifecycleScope.launch {
            try {
                val resp = service.login(LoginRequest(username=id, password=pw))

                if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body?.ok == true) {
                        AuthManager.saveTokens(
                            context = this@LoginActivity,
                            access = body.accessToken,
                            refresh = body.refreshToken
                        )
                        email.setText("")
                        password.setText("")

                        // startActivity(Intent(this, HomeActivity::class.java))
                        // finish()
                    }
                    else {
                        Toast.makeText(this@LoginActivity, "아이디 또는 비밀번호가 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                else {
                    Toast.makeText(this@LoginActivity, "서버 응답 없음", Toast.LENGTH_SHORT).show()
                }
            }
            catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "네트워크 에러", Toast.LENGTH_SHORT).show()
            }
            finally {
                btnLogin.isEnabled = true
            }
        }
    }

    private fun onClickForgotPassword() {
        val intent = Intent(this, ForgotPasswordActivity::class.java)
        startActivity(intent)
    }

    private fun onClickSignUp() {
        val intent = Intent(this, SignupActivity::class.java)
        startActivity(intent)
    }

    private fun onClickGoogleLogin() {
        Toast.makeText(this, "구글 회원가입", Toast.LENGTH_SHORT).show()
    }

    private fun onClickKakaoLogin() {
        Toast.makeText(this, "카카오 회원가입", Toast.LENGTH_SHORT).show()
    }
}