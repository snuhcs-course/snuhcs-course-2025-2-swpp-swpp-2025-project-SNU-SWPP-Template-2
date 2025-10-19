package com.aisportspt.app.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.aisportspt.app.databinding.FragmentTrainingBinding
import com.aisportspt.app.ui.dialogs.CreateTrainingPlanDialogFragment
import com.aisportspt.app.ui.dialogs.ModifyDateDialogFragment
import com.aisportspt.app.ui.dialogs.ModifyPlanDialogFragment
import com.aisportspt.app.*
import com.aisportspt.app.model.Difficulty
import com.aisportspt.app.model.Schedule
import com.aisportspt.app.model.Session
import com.aisportspt.app.model.TrainingPlan
import com.aisportspt.app.ui.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.*
class TrainingFragment : Fragment() {

    private var _binding: FragmentTrainingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels<MainViewModel>()

    fun makeTrainingPlan(): TrainingPlan{
        //TODO: 훈련 계획 생성
        return TrainingPlan("","","","", Difficulty.INTERMEDIATE,"",LinkedList(),false)
    }

    // 샘플 데이터
    private val workoutDates = HashSet<String>()

    private var achievementRate = 68

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrainingBinding.inflate(inflater, container, false)
        val view = binding.root

        setupMockData()
        setupClickListeners()
        updateUI()

        return view
    }

    private fun setupMockData() {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        cal.set(2025, Calendar.OCTOBER, 5)
        workoutDates.add(sdf.format(cal.time))
        cal.set(2025, Calendar.OCTOBER, 8)
        workoutDates.add(sdf.format(cal.time))
        cal.set(2025, Calendar.OCTOBER, 12)
        workoutDates.add(sdf.format(cal.time))
    }

    private fun setupClickListeners() {
        binding.btnCreatePlan.setOnClickListener {
            CreateTrainingPlanDialogFragment()
                .show(childFragmentManager, "CreatePlanDialog")
        }

        binding.btnEditPlan.setOnClickListener {
            if(viewModel.user.value!!.workDates.contains(viewModel.user.value?.selectedDate))
                ModifyDateDialogFragment { selectedDate ->
                // 날짜 선택 완료되면 → 시간대 다이얼로그 실행
                ModifyPlanDialogFragment { selectedDateFinal, selectedTimeSlot ->
                    viewModel.modifyTrainingPlan(selectedDateFinal,selectedTimeSlot)
                }.show(childFragmentManager, "ModifyPlanDialog")
            }.show(childFragmentManager, "ModifyDateDialog")
            else{
                Toast.makeText(requireContext(),"변경할 날짜를 먼저 선택해 주세요",Toast.LENGTH_SHORT).show()
            }
        }



        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(year, month, dayOfMonth)

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val selectedDateStr = sdf.format(selectedDate.time)
            viewModel.user.value!!.selectedDate=selectedDateStr
            if (viewModel.user.value!!.workDates.contains(selectedDateStr)) {

                // 선택된 날짜에 훈련 계획이 있으면 상세보기 열기
                // 일단 ui의 부재로 인해 toast로 대체

                val scheduleList=viewModel.user.value!!.schedules.filter { it.date==selectedDateStr }
                for(schedule in scheduleList){
                    Toast.makeText(requireContext(),schedule.session.focus,Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateUI() {
        // TODO:
        val monthFormat = SimpleDateFormat("yyyy년 M월", Locale.KOREAN)

        binding.progressAchievement.progress = achievementRate
        binding.tvAchievementPercentage.text = "$achievementRate%"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
