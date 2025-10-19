package com.aisportspt.app.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.aisportspt.app.MainActivity
import com.aisportspt.app.databinding.FragmentSignupStep3Binding
import androidx.core.content.ContextCompat
import com.aisportspt.app.R



class SignupStep3Fragment : Fragment() {

    private var _binding: FragmentSignupStep3Binding? = null
    private val binding get() = _binding!!

    private var name: String? = null
    private var email: String? = null
    private var password: String? = null
    private var sports: ArrayList<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            name = it.getString(ARG_NAME)
            email = it.getString(ARG_EMAIL)
            password = it.getString(ARG_PASSWORD)
            sports = it.getStringArrayList(ARG_SPORTS)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupStep3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rgLevel.setOnCheckedChangeListener { _, checkedId ->
            val selected = checkedId != -1
            binding.btnComplete.apply {
                isEnabled = selected
                setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        if (selected) R.color.nav_selected else R.color.achievement_silver
                    )
                )
            }
        }

        binding.btnComplete.setOnClickListener {
            val level = when (binding.rgLevel.checkedRadioButtonId) {
                binding.rbBeginner.id -> "초급"
                binding.rbIntermediate.id -> "중급"
                binding.rbAdvanced.id -> "고급"
                else -> ""
            }

            Toast.makeText(requireContext(), "회원가입 완료!", Toast.LENGTH_SHORT).show()

            val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_NAME = "name"
        private const val ARG_EMAIL = "email"
        private const val ARG_PASSWORD = "password"
        private const val ARG_SPORTS = "sports"

        fun newInstance(name: String, email: String, password: String, sports: ArrayList<String>) =
            SignupStep3Fragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_NAME, name)
                    putString(ARG_EMAIL, email)
                    putString(ARG_PASSWORD, password)
                    putStringArrayList(ARG_SPORTS, sports)
                }
            }
    }
}
