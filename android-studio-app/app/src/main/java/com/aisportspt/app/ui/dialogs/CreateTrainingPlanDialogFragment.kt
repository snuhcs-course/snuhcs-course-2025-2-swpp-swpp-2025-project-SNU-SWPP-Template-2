package com.aisportspt.app.ui.dialogs

import android.app.Dialog
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.widget.CheckBox
import android.widget.RadioGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.aisportspt.app.R
import com.aisportspt.app.databinding.DialogCreatePlanBinding
import com.aisportspt.app.model.Schedule
import com.aisportspt.app.ui.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.util.Locale

class CreateTrainingPlanDialogFragment : DialogFragment() {

    private var _binding: DialogCreatePlanBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels<MainViewModel>()

    private val dayCheckboxes = mutableListOf<CheckBox>()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCreatePlanBinding.inflate(layoutInflater)

        // 체크박스 모음
        dayCheckboxes.apply {
            add(binding.cbMonday)
            add(binding.cbTuesday)
            add(binding.cbWednesday)
            add(binding.cbThursday)
            add(binding.cbFriday)
            add(binding.cbSaturday)
            add(binding.cbSunday)
        }

        binding.btnCompleteCreation.setOnClickListener {
            if (isFormValid()) {
                val selectedDays = dayCheckboxes.filter { it.isChecked }.map { it.text.toString() }
                val selectedTimeSlot = when (binding.rgTimeSlots.checkedRadioButtonId) {
                    R.id.rb_morning -> "09:00"
                    R.id.rb_afternoon -> "13:00"
                    R.id.rb_evening -> "18:00"
                    else -> ""
                }
                val selectedFinishTime=when(binding.rgTimeSlots.checkedRadioButtonId){
                    R.id.rb_morning->"11:00"
                    R.id.rb_afternoon->"15:00"
                    R.id.rb_evening->"20:00"
                    else->"0"
                }
                viewModel.createTrainingPlan(selectedDays,selectedTimeSlot,selectedFinishTime)
                Toast.makeText(requireContext(), "계획이 생성되었습니다!", Toast.LENGTH_SHORT).show()
                dismiss()
            } else {
                Toast.makeText(requireContext(), "요일과 시간대를 모두 선택해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }

    private fun isFormValid(): Boolean {
        val isDaySelected = dayCheckboxes.any { it.isChecked }
        val isTimeSelected = binding.rgTimeSlots.checkedRadioButtonId != RadioGroup.NO_ID
        return isDaySelected && isTimeSelected
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
