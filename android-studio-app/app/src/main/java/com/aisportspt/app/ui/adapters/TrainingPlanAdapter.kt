package com.aisportspt.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aisportspt.app.databinding.ItemTrainingPlanBinding
import com.aisportspt.app.model.TrainingPlan
import com.aisportspt.app.model.Difficulty

class TrainingPlanAdapter(
    private val onStartTraining: (TrainingPlan) -> Unit,
    private val onViewDetails: (TrainingPlan) -> Unit
) : RecyclerView.Adapter<TrainingPlanAdapter.TrainingPlanViewHolder>() {

    private val plans = mutableListOf<TrainingPlan>()

    fun updatePlans(newPlans: List<TrainingPlan>) {
        plans.clear()
        plans.addAll(newPlans)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainingPlanViewHolder {
        val binding = ItemTrainingPlanBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TrainingPlanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrainingPlanViewHolder, position: Int) {
        holder.bind(plans[position])
    }

    override fun getItemCount(): Int = plans.size

    inner class TrainingPlanViewHolder(private val binding: ItemTrainingPlanBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(plan: TrainingPlan) {
            binding.apply {
                // 기본 정보 설정
                textPlanName.text = plan.name
                textPlanSport.text = plan.sport
                textDuration.text = plan.duration
                textDifficulty.text = getDifficultyText(plan.difficulty)
                textExerciseCount.text = "${plan.exercises.size}개 운동"
                textFocus.text = plan.focus

                // AI 생성 배지 표시
                aiBadge.visibility = if (plan.aiGenerated) View.VISIBLE else View.GONE

                // 버튼 클릭 리스너
                btnStartTraining.setOnClickListener {
                    onStartTraining(plan)
                }

                root.setOnClickListener {
                    onViewDetails(plan)
                }
            }
        }

        private fun getDifficultyText(difficulty: Difficulty): String {
            return when (difficulty) {
                Difficulty.BEGINNER -> "초급"
                Difficulty.INTERMEDIATE -> "중급"
                Difficulty.ADVANCED -> "고급"
            }
        }
    }
}