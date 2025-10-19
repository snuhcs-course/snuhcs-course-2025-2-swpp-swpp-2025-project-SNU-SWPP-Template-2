package com.aisportspt.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.aisportspt.app.databinding.FragmentSignupStep1Binding
import android.text.Editable
import android.text.TextWatcher
import androidx.core.content.ContextCompat
import com.aisportspt.app.R


class SignupStep1Fragment : Fragment() {

    private var _binding: FragmentSignupStep1Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupStep1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 모든 입력란 감시
        val inputs = listOf(binding.etName, binding.etEmail, binding.etPassword, binding.etPasswordConfirm)

        inputs.forEach { editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    val allFilled = inputs.all { it.text.toString().trim().isNotEmpty() }
                    binding.btnNext.apply {
                        isEnabled = allFilled
                        setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                if (allFilled) R.color.nav_selected else R.color.achievement_silver
                            )
                        )
                    }
                }
            })
        }


        binding.btnNext.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val pw = binding.etPassword.text.toString().trim()
            val pwConfirm = binding.etPasswordConfirm.text.toString().trim()

            if (pw != pwConfirm) {
                Toast.makeText(requireContext(), "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fragment = SignupStep2Fragment.newInstance(name, email, pw)
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.login_container, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
