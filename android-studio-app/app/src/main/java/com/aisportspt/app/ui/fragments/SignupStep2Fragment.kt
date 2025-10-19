package com.aisportspt.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aisportspt.app.R
import com.aisportspt.app.databinding.FragmentSignupStep2Binding
import androidx.core.content.ContextCompat

class SignupStep2Fragment : Fragment() {

    private var _binding: FragmentSignupStep2Binding? = null
    private val binding get() = _binding!!

    private var name: String? = null
    private var email: String? = null
    private var password: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            name = it.getString(ARG_NAME)
            email = it.getString(ARG_EMAIL)
            password = it.getString(ARG_PASSWORD)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupStep2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val checkBoxes = listOf(binding.cbSoccer, binding.cbBasketball, binding.cbFitness)

        checkBoxes.forEach { cb ->
            cb.setOnCheckedChangeListener { _, _ ->
                val anyChecked = checkBoxes.any { it.isChecked }
                binding.btnNext.apply {
                    isEnabled = anyChecked
                    setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            if (anyChecked) R.color.nav_selected else R.color.achievement_silver
                        )
                    )
                }
            }
        }


        binding.btnNext.setOnClickListener {
            val selectedSports = mutableListOf<String>()
            if (binding.cbSoccer.isChecked) selectedSports.add("축구")
            if (binding.cbBasketball.isChecked) selectedSports.add("농구")
            if (binding.cbFitness.isChecked) selectedSports.add("헬스")

            val fragment = SignupStep3Fragment.newInstance(
                name!!, email!!, password!!, ArrayList(selectedSports)
            )
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

    companion object {
        private const val ARG_NAME = "name"
        private const val ARG_EMAIL = "email"
        private const val ARG_PASSWORD = "password"

        fun newInstance(name: String, email: String, password: String) =
            SignupStep2Fragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_NAME, name)
                    putString(ARG_EMAIL, email)
                    putString(ARG_PASSWORD, password)
                }
            }
    }
}
