package com.example.mindlog.features.auth.presentation.signup

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.mindlog.core.common.SystemUiHelper
import com.example.mindlog.databinding.ActivitySignupBinding
import com.example.mindlog.features.auth.presentation.login.LoginActivity
import com.example.mindlog.features.auth.presentation.main.MainActivity
import com.google.android.material.textfield.TextInputLayout
import java.time.YearMonth

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

        // 힌트 조작 리스너 제거 (XML 힌트만 사용)
        // (기존 setOnFocusChangeListener 4개 전부 삭제)

        // 생년월일 드롭다운 어댑터 연결
        setupBirthDropdowns()

        // 확인 비밀번호 endIcon 동적 전환
        setupConfirmPasswordEndIcon()

        // 회원가입 버튼
        binding.btnSignup.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val id = binding.etLoginId.text.toString().trim()
            val pw = binding.etPassword.text.toString()
            val confirm = binding.etConfirmPassword.text.toString()

            val gender = when (binding.rgGender.checkedRadioButtonId) {
                com.example.mindlog.R.id.rbMale -> "Male"
                com.example.mindlog.R.id.rbFemale -> "Female"
                else -> null
            }
            val birthY = binding.actBirthYear.text.toString().toIntOrNull()
            val birthM = binding.actBirthMonth.text.toString().toIntOrNull()
            val birthD = binding.actBirthDay.text.toString().toIntOrNull()

            if (username.isBlank() || id.isBlank() || pw.isBlank()) {
                showSignupError("모든 필드를 입력해주세요.")
                return@setOnClickListener
            }
            if (pw != confirm) {
                showSignupError("비밀번호가 일치하지 않습니다.")
                return@setOnClickListener
            }
            if (gender == null) {
                showSignupError("성별을 선택해주세요.")
                return@setOnClickListener
            }
            if (birthY == null || birthM == null || birthD == null) {
                showSignupError("생년월일을 선택해주세요.")
                return@setOnClickListener
            }
            // (선택) 존재하지 않는 날짜 방지 체크
            if (!isValidDate(birthY, birthM, birthD)) {
                showSignupError("유효하지 않은 생년월일입니다.")
                return@setOnClickListener
            }

            // 클라이언트 측 검증 통과 시 기존 에러 메시지 제거
            showSignupError(null)

            val birthDate = java.time.LocalDate.of(birthY, birthM, birthD)
            viewModel.signup(id, pw, username, gender, birthDate)
        }

        // 로그인 화면 이동
        binding.tvGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // ViewModel 관찰
        viewModel.signupResult.observe(this) { success ->
            if (success) {
                // 성공 시 에러 메시지 제거
                showSignupError(null)
                Toast.makeText(this, "회원가입 성공! 자동 로그인 중...", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            } else {
                // 실패 시 ViewModel의 에러 메시지를 버튼 위에 표시
                val msg = viewModel.errorMessage.value
                showSignupError(msg ?: "회원가입에 실패했습니다. 다시 시도해주세요.")
            }
        }

        viewModel.errorMessage.observe(this) { msg ->
            // 서버/유효성 에러 메시지를 항상 빨간 글씨로 표시
            showSignupError(msg)
        }
    }

    private fun setupBirthDropdowns() {
        val currentYear = java.time.LocalDate.now().year
        val years = (1900..currentYear).toList().reversed().map { it.toString() }
        val months = (1..12).map { it.toString() }
        val days31 = (1..31).map { it.toString() }

        binding.actBirthYear.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, years))
        binding.actBirthMonth.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, months))
        binding.actBirthDay.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, days31))

        fun daysInMonth(y: Int, m: Int) = YearMonth.of(y, m).lengthOfMonth()

        fun updateDays() {
            val y = binding.actBirthYear.text.toString().toIntOrNull() ?: return
            val m = binding.actBirthMonth.text.toString().toIntOrNull() ?: return
            val maxD = daysInMonth(y, m)
            val dayList = (1..maxD).map { it.toString() }
            binding.actBirthDay.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, dayList))
            val cur = binding.actBirthDay.text.toString().toIntOrNull()
            if (cur == null || cur > maxD) binding.actBirthDay.setText("")
        }

        binding.actBirthYear.setOnItemClickListener { _, _, _, _ -> updateDays() }
        binding.actBirthMonth.setOnItemClickListener { _, _, _, _ -> updateDays() }
    }

    private fun setupConfirmPasswordEndIcon() {
        val tilConfirm = binding.tilConfirmPassword
        val etConfirm = binding.etConfirmPassword

        etConfirm.setOnFocusChangeListener { _, hasFocus ->
            tilConfirm.endIconMode = if (hasFocus)
                TextInputLayout.END_ICON_PASSWORD_TOGGLE
            else
                TextInputLayout.END_ICON_CLEAR_TEXT
        }
    }

    private fun isValidDate(y: Int, m: Int, d: Int): Boolean {
        return try {
            YearMonth.of(y, m).atDay(d)  // 예외 던지면 invalid
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) SystemUiHelper.hideSystemUI(this)
    }

    private fun showSignupError(message: String?) {
        if (message.isNullOrBlank()) {
            binding.tvSignupError.visibility = View.GONE
            binding.tvSignupError.text = ""
        } else {
            val errorText = when {
                message.contains("password", ignoreCase = true) ->
                    "조금 더 복잡한 비밀번호를 사용해주세요."
                message.contains("login ID", ignoreCase = true) ||
                        message.contains("loginId", ignoreCase = true) ||
                        message.contains("login_id", ignoreCase = true) ->
                    "동일한 로그인 아이디가 존재합니다. 다른 아이디를 사용해주세요."
                else -> message
            }

            binding.tvSignupError.visibility = View.VISIBLE
            binding.tvSignupError.text = errorText
        }
    }
}
