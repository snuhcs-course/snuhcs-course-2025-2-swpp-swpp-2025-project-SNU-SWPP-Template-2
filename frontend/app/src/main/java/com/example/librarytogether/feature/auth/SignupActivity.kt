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
import com.example.librarytogether.feature.auth.data.SignUpRequest
import com.example.librarytogether.feature.auth.data.SignUpResponse
import com.example.librarytogether.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {

    private lateinit var userName: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var btnSignUp: MaterialButton
    private lateinit var btnGoogle: MaterialButton
    private lateinit var btnKakao: MaterialButton
    private lateinit var btnLogin: MaterialButton

    private val service: AuthApi by lazy { RetrofitClient.instance.create(AuthApi::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.sign_up)

        // View binding
        userName = findViewById(R.id.UserNameText)
        email = findViewById(R.id.EmailText)
        password = findViewById(R.id.PasswordText)
        btnSignUp = findViewById(R.id.SignUpButton)
        btnGoogle = findViewById(R.id.GoogleSignUpButton)
        btnKakao = findViewById(R.id.KakaoLoginButton)
        btnLogin = findViewById(R.id.LogInButton)

        // Button click handler
        btnSignUp.setOnClickListener { onClickSignUp() }
        btnGoogle.setOnClickListener { onClickGoogleSignUp() }
        btnKakao.setOnClickListener { onClickKakaoSignUp() }
        btnLogin.setOnClickListener { onClickGoToLogin() }
    }

    private fun onClickSignUp() {
        val id = userName.text.toString().trim()
        val mail = email.text.toString().trim()
        val pw = password.text.toString()

        if (id.isEmpty() || mail.isEmpty() || pw.isEmpty()) {
            Toast.makeText(this, "모든 필드를 입력하세요", Toast.LENGTH_SHORT).show()
            return
        }
        val passwordRegex = Regex("^(?=.*[A-Za-z]).{6,}$")
        if(!pw.matches(passwordRegex)){
            Toast.makeText(this, "비밀번호는 6자 이상 영문을 포함해야 합니다", Toast.LENGTH_SHORT).show()
            return
        }

        btnSignUp.isEnabled = false


        lifecycleScope.launch {
            try {
                val resp = service.signUp(SignUpRequest(username=id, password=pw, email=mail))

                if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body?.ok == true) {
                        // Reset input fields(입력 초기화)
                        userName.setText("")
                        email.setText("")
                        password.setText("")

                        // startActivity(Intent(this, HomeActivity::class.java))
                        // finish()
                    }
                    else {
                        Toast.makeText(this@SignupActivity, "모든 필드를 올바르게 입력하세요", Toast.LENGTH_SHORT).show()
                    }
                }
                else {
                    Toast.makeText(this@SignupActivity, "서버 응답 없음", Toast.LENGTH_SHORT).show()
                }
            }
            catch (e: Exception) {
                Toast.makeText(this@SignupActivity, "네트워크 에러", Toast.LENGTH_SHORT).show()
            }
            finally {
                btnLogin.isEnabled = true
            }
        }
    }

    private fun onClickGoogleSignUp() {
        Toast.makeText(this, "구글 회원가입", Toast.LENGTH_SHORT).show()
    }

    private fun onClickKakaoSignUp() {
        Toast.makeText(this, "카카오 회원가입", Toast.LENGTH_SHORT).show()
    }

    private fun onClickGoToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}