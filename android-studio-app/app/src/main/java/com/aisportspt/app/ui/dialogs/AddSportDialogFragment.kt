package com.aisportspt.app.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.aisportspt.app.R
import com.aisportspt.app.databinding.DialogAddSportBinding
import com.aisportspt.app.model.Sport
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*

class AddSportDialogFragment(
    private val onSportAdded: (Sport) -> Unit
) : DialogFragment() {

    private var _binding: DialogAddSportBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddSportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
    }

    private fun setupUI() {
        // 스포츠 스피너 설정
        val sports = resources.getStringArray(R.array.sports_array)
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            sports
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSport.adapter = adapter

        // 스킬 레벨 스피너 설정
        val skillLevels = arrayOf("초급", "초중급", "중급", "중고급", "고급")
        val skillAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            skillLevels
        )
        skillAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSkillLevel.adapter = skillAdapter

        // 주간 목표 스피너 설정
        val weeklyGoals = arrayOf("1회", "2회", "3회", "4회", "5회", "6회", "7회")
        val goalAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            weeklyGoals
        )
        goalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerWeeklyGoal.adapter = goalAdapter

        // 버튼 리스너
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            saveSport()
        }
    }

    private fun saveSport() {
        val sportName = binding.spinnerSport.selectedItem.toString()
        val skillLevel = binding.spinnerSkillLevel.selectedItem.toString()
        val weeklyGoalText = binding.spinnerWeeklyGoal.selectedItem.toString()
        val nextGoal = binding.editNextGoal.text.toString().trim()

        if (nextGoal.isEmpty()) {
            binding.editNextGoal.error = "목표를 입력해주세요"
            return
        }

        val weeklyGoal = weeklyGoalText.replace("회", "").toInt()

        val sport = Sport(
            id = UUID.randomUUID().toString(),
            name = sportName,
            imageUrl = getSportImageUrl(sportName),
            totalSessions = 0,
            weeklyGoal = weeklyGoal,
            currentWeekSessions = 0,
            lastSession = "없음",
            skillLevel = skillLevel,
            nextGoal = nextGoal
        )

        onSportAdded(sport)
        dismiss()
    }

    private fun getSportImageUrl(sportName: String): String {
        return when (sportName) {
            "골프" -> "https://images.unsplash.com/photo-1703293024102-44224053a305"
            "볼링" -> "https://images.unsplash.com/photo-1628139417027-c356ba05fe4f"
            "테니스" -> "https://images.unsplash.com/photo-1622279457486-62dcc4a431d6"
            "배드민턴" -> "https://images.unsplash.com/photo-1613918108466-292b78a8ef95"
            "탁구" -> "https://images.unsplash.com/photo-1609710228159-0fa9bd7c0827"
            "야구" -> "https://images.unsplash.com/photo-1566577739112-5180d4bf9e10"
            "농구" -> "https://images.unsplash.com/photo-1546519638-68e109498ffc"
            else -> "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}