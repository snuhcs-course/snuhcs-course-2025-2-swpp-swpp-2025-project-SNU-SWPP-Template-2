package com.aisportspt.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aisportspt.app.databinding.ItemSportCardBinding
import com.aisportspt.app.model.Sport
import com.bumptech.glide.Glide

class SportsAdapter(
    private val onAddSession: (Sport) -> Unit,
    private val onViewDetails: (Sport) -> Unit
) : RecyclerView.Adapter<SportsAdapter.SportViewHolder>() {

    private val sports = mutableListOf<Sport>()

    fun updateSports(newSports: List<Sport>) {
        sports.clear()
        sports.addAll(newSports)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SportViewHolder {
        val binding = ItemSportCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SportViewHolder, position: Int) {
        holder.bind(sports[position])
    }

    override fun getItemCount(): Int = sports.size

    inner class SportViewHolder(private val binding: ItemSportCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(sport: Sport) {
            binding.apply {
                // 스포츠 정보 설정
                textSportName.text = sport.name
                chipSkillLevel.text = sport.skillLevel
                textTotalSessions.text = sport.totalSessions.toString()
                textWeeklySessions.text = "${sport.currentWeekSessions}/${sport.weeklyGoal}"
                textLastSession.text = sport.lastSession
                textNextGoal.text = sport.nextGoal

                // 진행도 계산 및 설정
                val progressPercentage = ((sport.currentWeekSessions.toFloat() / sport.weeklyGoal) * 100).toInt()
                progressWeeklyGoal.progress = progressPercentage
                textProgressPercentage.text = "${progressPercentage}%"

                // 이미지 로드 (Glide 사용)
                Glide.with(imageSport.context)
                    .load(sport.imageUrl)
                    .centerCrop()
                    .into(imageSport)

                // 버튼 클릭 리스너
                btnAddSession.setOnClickListener {
                    onAddSession(sport)
                }

                btnViewStats.setOnClickListener {
                    onViewDetails(sport)
                }
            }
        }
    }
}