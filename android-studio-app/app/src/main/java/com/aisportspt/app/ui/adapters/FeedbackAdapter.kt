package com.aisportspt.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aisportspt.app.R
import com.aisportspt.app.databinding.ItemFeedbackBinding
import com.aisportspt.app.model.AIPoseFeedback

class FeedbackAdapter : RecyclerView.Adapter<FeedbackAdapter.FeedbackViewHolder>() {

    private val feedbackList = mutableListOf<AIPoseFeedback>()

    fun updateFeedback(newFeedback: List<AIPoseFeedback>) {
        feedbackList.clear()
        feedbackList.addAll(newFeedback)
        notifyDataSetChanged()
    }

    fun addFeedback(feedback: AIPoseFeedback) {
        feedbackList.add(0, feedback) // Add at beginning
        notifyItemInserted(0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedbackViewHolder {
        val binding = ItemFeedbackBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FeedbackViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeedbackViewHolder, position: Int) {
        holder.bind(feedbackList[position])
    }

    override fun getItemCount(): Int = feedbackList.size

    inner class FeedbackViewHolder(private val binding: ItemFeedbackBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(feedback: AIPoseFeedback) {
            binding.apply {
                // 피드백 메시지 설정
                textFeedbackMessage.text = feedback.message
                
                // 신뢰도 설정
                textConfidence.text = "${feedback.confidence}%"
                
                // 타임스탬프 설정
                textTimestamp.text = getTimeAgo(feedback.timestamp)
                
                // 피드백 타입에 따른 아이콘 및 색상 설정
                when (feedback.type) {
                    "good" -> {
                        iconFeedbackType.setImageResource(R.drawable.ic_target)
                        iconFeedbackType.setColorFilter(
                            binding.root.context.getColor(R.color.feedback_good)
                        )
                    }
                    "warning" -> {
                        iconFeedbackType.setImageResource(R.drawable.ic_clock)
                        iconFeedbackType.setColorFilter(
                            binding.root.context.getColor(R.color.feedback_warning)
                        )
                    }
                    "error" -> {
                        iconFeedbackType.setImageResource(R.drawable.ic_refresh)
                        iconFeedbackType.setColorFilter(
                            binding.root.context.getColor(R.color.feedback_error)
                        )
                    }
                }
            }
        }

        private fun getTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            return when {
                diff < 1000 -> "방금 전"
                diff < 60000 -> "${diff / 1000}초 전"
                diff < 3600000 -> "${diff / 60000}분 전"
                diff < 86400000 -> "${diff / 3600000}시간 전"
                else -> "${diff / 86400000}일 전"
            }
        }
    }
}