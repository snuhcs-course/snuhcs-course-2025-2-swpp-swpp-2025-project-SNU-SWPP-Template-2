package com.example.mindlog.features.settings.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.mindlog.core.common.Result
import com.example.mindlog.databinding.FragmentEditProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupBirthDateDropdowns() // 년/월/일 드롭다운 설정
        setupSaveButton()

        // 데이터 관찰 및 로드
        observeViewModel()
        viewModel.loadUserInfo()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupBirthDateDropdowns() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)

        // 년도: 현재 년도부터 1900년까지 역순
        val years = (currentYear downTo 1900).map { it.toString() }
        val yearAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, years)
        binding.actBirthYear.setAdapter(yearAdapter)

        // 월: 1~12
        val months = (1..12).map { it.toString().padStart(2, '0') }
        val monthAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, months)
        binding.actBirthMonth.setAdapter(monthAdapter)

        // 일: 1~31
        val days = (1..31).map { it.toString().padStart(2, '0') }
        val dayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, days)
        binding.actBirthDay.setAdapter(dayAdapter)
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()

            if (username.isBlank()) {
                binding.tilUsername.error = "이름을 입력해주세요."
                return@setOnClickListener
            }
            binding.tilUsername.error = null

            val gender = when (binding.rgGender.checkedRadioButtonId) {
                binding.rbMale.id -> "Male"
                binding.rbFemale.id -> "Female"
                else -> null
            }

            // 생년월일 합치기
            val year = binding.actBirthYear.text.toString()
            val month = binding.actBirthMonth.text.toString()
            val day = binding.actBirthDay.text.toString()

            val birthdate = if (year.isNotBlank() && month.isNotBlank() && day.isNotBlank()) {
                "$year-$month-$day"
            } else {
                null
            }

            val appearance = binding.etAppearance.text.toString().trim().ifBlank { null }

            viewModel.updateProfile(username, gender, birthdate, appearance)
        }
    }

    private fun observeViewModel() {
        // 1. 초기 데이터 로드 시 UI 채우기
        viewModel.userInfo.observe(viewLifecycleOwner) { result ->
            if (result is Result.Success) {
                val user = result.data
                if (binding.etUsername.text.isNullOrBlank()) {
                    binding.etUsername.setText(user.username)
                    binding.etAppearance.setText(user.appearance ?: "")

                    when (user.gender) {
                        "Male" -> binding.rbMale.isChecked = true
                        "Female" -> binding.rbFemale.isChecked = true
                    }

                    user.birthdate?.let { dateStr ->
                        val parts = dateStr.split("-")
                        if (parts.size == 3) {
                            binding.actBirthYear.setText(parts[0], false)
                            binding.actBirthMonth.setText(parts[1], false)
                            binding.actBirthDay.setText(parts[2], false)
                        }
                    }
                }
            } else if (result is Result.Error) {
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
            }
        }

        // 2. 저장 결과 처리
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.updateResult.collect { result ->
                    when (result) {
                        is Result.Success -> {
                            Toast.makeText(requireContext(), result.data, Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        }
                        is Result.Error -> {
                            Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
