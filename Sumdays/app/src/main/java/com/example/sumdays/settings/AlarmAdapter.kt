package com.example.sumdays.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.databinding.ItemAlarmTimeBinding

class AlarmAdapter(
    private val onTimeClicked: (position: Int, time: String) -> Unit, // 시간 수정 콜백
    private val onDeleteClicked: (position: Int) -> Unit, // 삭제 콜백
) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    private val alarmList = mutableListOf<String>()

    // 알람 목록을 업데이트하고 UI를 갱신
    fun updateList(newTimes: List<String>) {
        alarmList.clear()
        alarmList.addAll(newTimes)
        notifyDataSetChanged()
    }

    // 마스터 스위치 상태에 따라 UI를 업데이트 (흐림 효과)
    var isMasterOn: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged() // 모든 아이템을 다시 그려 흐림 효과 적용
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val binding = ItemAlarmTimeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AlarmViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val time = alarmList[position]
        holder.bind(time, position, isMasterOn)
    }

    override fun getItemCount(): Int = alarmList.size

    inner class AlarmViewHolder(private val binding: ItemAlarmTimeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(time: String, position: Int, isMasterEnabled: Boolean) {
            binding.alarmTimeTextView.text = time

            // --- 마스터 스위치 ON/OFF에 따른 흐림 효과 및 클릭 비활성화 ---
            val alpha = if (isMasterEnabled) 1.0f else 0.5f
            binding.root.alpha = alpha
            binding.root.isEnabled = isMasterEnabled
            binding.alarmTimeTextView.isEnabled = isMasterEnabled
            binding.deleteAlarmButton.isEnabled = isMasterEnabled // 삭제 버튼도 비활성화

            // --- 리스너 설정 ---

            // 1. 시간 클릭 (시간 수정)
            binding.alarmTimeTextView.setOnClickListener {
                if (isMasterEnabled) onTimeClicked(position, time)
            }

            // 2. 삭제 버튼 클릭
            binding.deleteAlarmButton.setOnClickListener {
                if (isMasterEnabled) onDeleteClicked(position)
            }
        }
    }
}