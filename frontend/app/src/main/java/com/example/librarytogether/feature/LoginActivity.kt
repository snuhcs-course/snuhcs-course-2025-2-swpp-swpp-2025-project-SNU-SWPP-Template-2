package com.example.librarytogether.feature

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.librarytogether.R
import com.google.android.material.button.MaterialButton

class LoginActivity : AppCompatActivity() {

    private lateinit var userName: EditText
    private lateinit var password: EditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnForgot: MaterialButton
    private lateinit var btnGoogle: MaterialButton
    private lateinit var btnKakao: MaterialButton
    private lateinit var btnSignUp: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.log_in)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        userName = findViewById(R.id.UserNameText)
        password = findViewById(R.id.PasswordText)
        btnLogin = findViewById(R.id.LogInButton)
        btnForgot = findViewById(R.id.ForgotPasswordButton)
        btnGoogle = findViewById(R.id.GoogleLoginButton)
        btnKakao = findViewById(R.id.KakaoLoginButton)
        btnSignUp = findViewById(R.id.SignUpButton)

        btnLogin.setOnClickListener { onClickLogin() }
        btnForgot.setOnClickListener { onClickForgotPassword() }
        btnGoogle.setOnClickListener { onClickGoogleLogin() }
        btnKakao.setOnClickListener { onClickKakaoLogin() }
        btnSignUp.setOnClickListener { onClickSignUp() }
    }

    private fun onClickLogin() {
        val id = userName.text.toString().trim()
        val pw = password.text.toString()

        if (id.isEmpty() || pw.isEmpty()) {
            Toast.makeText(this, "아이디/비밀번호를 입력하세요", Toast.LENGTH_SHORT).show()
            return
        }

        userName.setText("")
        password.setText("")

        // startActivity(Intent(this, HomeActivity::class.java))
        // finish()
    }
}