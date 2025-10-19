package com.aisportspt.app.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.aisportspt.app.R
import com.aisportspt.app.databinding.DialogAddSessionBinding
import com.aisportspt.app.model.Session
import com.aisportspt.app.model.Sport
import java.text.SimpleDateFormat
import java.util.*

class AddSessionDialogFragment(
    private val sport: Sport,
    private val onSessionAdded: (Session) -> Unit
) : DialogFragment() {

    private var _binding: DialogAddSessionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddSessionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
    }

    private fun setupUI() {
        // 제목 설정
        binding.textTitle.text = "${sport.name} 연습 세션 추가"

        // 날짜 설정 (오늘 날짜로 기본 설정)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        binding.editDate.setText(today)

        // 강도 스피너 설정
        val intensities = arrayOf("가벼움", "보통", "높음")
        val intensityAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            intensities
        )
        intensityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerIntensity.adapter = intensityAdapter

        // 만족도 슬라이더 설정
        binding.sliderSatisfaction.valueTo = 10f
        binding.sliderSatisfaction.value = 7f
        binding.sliderSatisfaction.addOnChangeListener { _, value, _ ->
            binding.textSatisfactionValue.text = "${value.toInt()}/10"
        }
        binding.textSatisfactionValue.text = "7/10"

        // 운동 시간 기본값 설정
        binding.editDuration.setText("60")

        // 버튼 리스너
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            saveSession()
        }
    }

    private fun saveSession() {
        val date = binding.editDate.text.toString().trim()
        val durationText = binding.editDuration.text.toString().trim()
        val focus = binding.editFocus.text.toString().trim()
        val notes = binding.editNotes.text.toString().trim()
        val week=0
        val satisfaction = binding.sliderSatisfaction.value.toInt()
        val intensity = when (binding.spinnerIntensity.selectedItemPosition) {
            0 -> "light"
            1 -> "moderate"
            2 -> "high"
            else -> "moderate"
        }

        // 유효성 검사
        if (date.isEmpty()) {
            binding.editDate.error = "날짜를 입력해주세요"
            return
        }

        if (durationText.isEmpty()) {
            binding.editDuration.error = "운동 시간을 입력해주세요"
            return
        }

        val duration = try {
            durationText.toInt()
        } catch (e: NumberFormatException) {
            binding.editDuration.error = "올바른 숫자를 입력해주세요"
            return
        }

        if (focus.isEmpty()) {
            binding.editFocus.error = "집중 분야를 입력해주세요"
            return
        }

        val session = Session(
            id = UUID.randomUUID().toString(),
            sportId = sport.id,
            date = date,
            duration = duration,
            satisfaction = satisfaction,
            focus = focus,
            week=week,
            intensity = intensity,
            notes = notes
        )

        onSessionAdded(session)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}