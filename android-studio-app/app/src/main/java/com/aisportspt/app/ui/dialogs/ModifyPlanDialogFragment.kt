package com.aisportspt.app.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.aisportspt.app.databinding.DialogModifyPlanBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ModifyPlanDialogFragment(
    private val onPlanModified: (String, String) -> Unit // (날짜, 시간대)
) : DialogFragment() {

    private var _binding: DialogModifyPlanBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogModifyPlanBinding.inflate(layoutInflater)

        binding.btnBack.setOnClickListener {
            dismiss() // 뒤로가기 누르면 닫기
        }

        binding.btnCompleteEdit.setOnClickListener {
            // CalendarView에서 날짜 가져오기

            val cal = Calendar.getInstance().apply {
                timeInMillis=binding.calendarViewNewDate.date
            }
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val selectedDate = sdf.format(cal.time)

            // RadioGroup에서 시간대 가져오기
            val text=binding.root.findViewById<RadioButton>(binding.rgNewTimeSlots.checkedRadioButtonId)
                .text.toString()


            onPlanModified(selectedDate,text)

            Toast.makeText(requireContext(), "계획이 수정되었습니다!", Toast.LENGTH_SHORT).show()

            dismiss()
        }

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
