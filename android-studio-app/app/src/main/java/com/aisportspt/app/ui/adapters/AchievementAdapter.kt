package com.aisportspt.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aisportspt.app.R
import com.aisportspt.app.databinding.ItemAchievementBinding
import com.aisportspt.app.model.Achievement
import com.aisportspt.app.model.AchievementType
import java.text.SimpleDateFormat
import java.util.*

class AchievementAdapter : RecyclerView.Adapter<AchievementAdapter.AchievementViewHolder>() {

    private val achievements = mutableListOf<Achievement>()

    fun updateAchievements(newAchievements: List<Achievement>) {
        achievements.clear()
        achievements.addAll(newAchievements)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val binding = ItemAchievementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AchievementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        holder.bind(achievements[position])
    }

    override fun getItemCount(): Int = achievements.size

    inner class AchievementViewHolder(private val binding: ItemAchievementBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(achievement: Achievement) {
            binding.apply {
                // 기본 정보 설정
                textAchievementTitle.text = achievement.title
                textAchievementDescription.text = achievement.description
                
                // 업적 타입 설정
                chipAchievementType.text = getAchievementTypeText(achievement.type)
                chipAchievementType.setChipBackgroundColorResource(
                    getAchievementTypeColor(achievement.type)
                )

                // 진행도 설정
                val progressPercentage = ((achievement.progress.toFloat() / achievement.maxProgress) * 100).toInt()
                progressAchievement.progress = progressPercentage
                textProgress.text = "${achievement.progress} / ${achievement.maxProgress}"

                // 보상 포인트 표시
                textRewardPoints.text = "+${achievement.rewardPoints} XP"

                // 잠금/해제 상태 처리
                val isUnlocked = achievement.unlockedAt != null
                
                if (isUnlocked) {
                    // 해제된 업적
                    lockOverlay.visibility = View.GONE
                    imageAchievementIcon.alpha = 1.0f
                    
                    // 달성 날짜 표시
                    textUnlockedDate.visibility = View.VISIBLE
                    textUnlockedDate.text = formatUnlockedDate(achievement.unlockedAt!!)
                    
                    // 진행도 숨김 (이미 완료됨)
                    progressContainer.visibility = View.GONE
                } else {
                    // 잠긴 업적
                    lockOverlay.visibility = View.VISIBLE
                    imageAchievementIcon.alpha = 0.5f
                    
                    // 달성 날짜 숨김
                    textUnlockedDate.visibility = View.GONE
                    
                    // 진행도 표시
                    progressContainer.visibility = View.VISIBLE
                }

                // 아이콘 설정 (실제 구현에서는 achievement.icon에 따라 다른 아이콘 설정)
                imageAchievementIcon.setImageResource(getAchievementIcon(achievement.icon))
            }
        }

        private fun getAchievementTypeText(type: AchievementType): String {
            return when (type) {
                AchievementType.BRONZE -> "브론즈"
                AchievementType.SILVER -> "실버"
                AchievementType.GOLD -> "골드"
                AchievementType.PLATINUM -> "플래티넘"
            }
        }

        private fun getAchievementTypeColor(type: AchievementType): Int {
            return when (type) {
                AchievementType.BRONZE -> R.color.achievement_bronze
                AchievementType.SILVER -> R.color.achievement_silver
                AchievementType.GOLD -> R.color.achievement_gold
                AchievementType.PLATINUM -> R.color.achievement_platinum
            }
        }

        private fun formatUnlockedDate(timestamp: Long): String {
            val date = Date(timestamp)
            val formatter = SimpleDateFormat("yyyy년 M월 d일에 달성", Locale.KOREAN)
            return formatter.format(date)
        }

        private fun getAchievementIcon(iconName: String): Int {
            return when (iconName) {
                "ic_golf" -> R.drawable.ic_trophy
                "ic_target" -> R.drawable.ic_target
                "ic_streak" -> R.drawable.ic_zap
                "ic_ai" -> R.drawable.ic_auto_awesome
                "ic_bowling" -> R.drawable.ic_trophy
                else -> R.drawable.ic_award
            }
        }
    }
}