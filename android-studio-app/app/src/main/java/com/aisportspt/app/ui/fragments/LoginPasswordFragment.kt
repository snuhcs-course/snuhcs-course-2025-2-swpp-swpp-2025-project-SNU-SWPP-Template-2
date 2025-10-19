package com.aisportspt.app.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aisportspt.app.MainActivity
import com.aisportspt.app.databinding.FragmentLoginPasswordBinding
import android.text.Editable
import android.text.TextWatcher
import androidx.core.content.ContextCompat
import com.aisportspt.app.R
import com.aisportspt.app.data.remote.LoginRequest
import com.aisportspt.app.data.remote.RetrofitClient
import kotlinx.coroutines.launch

class LoginPasswordFragment : Fragment() {

    private var _binding: FragmentLoginPasswordBinding? = null
    private val binding get() = _binding!!

    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        email = arguments?.getString(ARG_EMAIL)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val hasText = !s.isNullOrEmpty()
                binding.btnLogin.apply {
                    isEnabled = hasText
                    setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            if (hasText) R.color.nav_selected else R.color.achievement_silver
                        )
                    )
                }
            }
        })

        binding.btnLogin.setOnClickListener {
            val password = binding.etPassword.text.toString().trim()
            val emailValue = email?.trim()

            if (emailValue.isNullOrEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "이메일과 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.apiService.login(
                        LoginRequest(emailValue, password)
                    )
                    // DB 로그인 충돌로 인해 임시 패스
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()

//                    if (response.isSuccessful && response.body()?.token != null) {
//                        // 로그인 성공 시 메인 화면으로 이동
//                        val intent = Intent(requireContext(), MainActivity::class.java)
//                        startActivity(intent)
//                        requireActivity().finish()
//                    } else {
//                        Toast.makeText(requireContext(), "로그인 실패: 이메일 또는 비밀번호를 확인하세요.", Toast.LENGTH_SHORT).show()
//                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "서버 연결 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_EMAIL = "email"

        fun newInstance(email: String): LoginPasswordFragment {
            val fragment = LoginPasswordFragment()
            val args = Bundle()
            args.putString(ARG_EMAIL, email)
            fragment.arguments = args
            return fragment
        }
    }
}
