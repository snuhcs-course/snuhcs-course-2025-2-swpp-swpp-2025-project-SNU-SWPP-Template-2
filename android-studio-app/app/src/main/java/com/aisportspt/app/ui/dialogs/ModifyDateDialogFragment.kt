package com.aisportspt.app.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.aisportspt.app.databinding.DialogModifyDateBinding
import java.text.SimpleDateFormat
import java.util.*

class ModifyDateDialogFragment(
    private val onDateSelected: (String) -> Unit
) : DialogFragment() {

    private var _binding: DialogModifyDateBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogModifyDateBinding.inflate(layoutInflater)

        binding.btnNextDate.setOnClickListener {
            val year = binding.datePickerModify.year
            val month = binding.datePickerModify.month
            val day = binding.datePickerModify.dayOfMonth

            val cal = Calendar.getInstance().apply {
                set(year, month, day)
            }
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val selectedDate = sdf.format(cal.time)

            // 선택한 날짜 전달
            onDateSelected(selectedDate)
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
