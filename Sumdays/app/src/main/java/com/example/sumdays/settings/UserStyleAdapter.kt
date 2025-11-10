package com.example.sumdays.settings

// com.example.sumdays.settings.UserStyleAdapter.kt

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.databinding.ItemUserStyleBinding
import com.example.sumdays.data.style.UserStyle // Room Entity로 가정된 클래스

class UserStyleAdapter(
    private val onStyleSelected: (styleId: Long) -> Unit, // 스타일 선택 (RadioButton 클릭)
    private val onDeleteClicked: (style: UserStyle) -> Unit,
    private val onStyleDeactivated: () -> Unit // ★★★ 활성 스타일 비활성화 요청 콜백 추가 ★★★
) : RecyclerView.Adapter<UserStyleAdapter.StyleViewHolder>() {

    private val stylesList = mutableListOf<UserStyle>()
    // 현재 활성화된 스타일의 ID (UserStats Prefs에서 가져옴)
    private var activeStyleId: Long? = null

    fun updateList(newStyles: List<UserStyle>, newActiveStyleId: Long?) {
        stylesList.clear()
        stylesList.addAll(newStyles)
        activeStyleId = newActiveStyleId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StyleViewHolder {
        val binding = ItemUserStyleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StyleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StyleViewHolder, position: Int) {
        val style = stylesList[position]
        holder.bind(style, activeStyleId)
    }

    override fun getItemCount(): Int = stylesList.size

    inner class StyleViewHolder(private val binding: ItemUserStyleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(style: UserStyle, activeId: Long?) {
            binding.styleNameTextView.text = style.styleName

            // StylePrompt에서 요약 정보 추출 (예: emotional_tone과 formality)
            binding.styleSummaryTextView.text =
                "톤: ${style.stylePrompt.emotional_tone}, 형식: ${style.stylePrompt.formality}"

            // --- 1. 활성 스타일 표시 (라디오 버튼) ---
            val isSelected = style.styleId == activeId
            binding.styleRadioButton.isChecked = isSelected

            // --- 2. 리스너 설정 수정 ---

            // 라디오 버튼 클릭 리스너 (root 클릭 리스너는 제거하거나 라디오 버튼 리스너와 동일하게 작동하도록 수정)
            binding.styleRadioButton.setOnClickListener {
                if (isSelected) {
                    // 1. 활성화된 상태의 라디오 버튼을 다시 누른 경우 -> 비활성화 요청
                    Toast.makeText(binding.root.context, "현재 스타일을 해제합니다.", Toast.LENGTH_SHORT).show()
                    onStyleDeactivated() // 상위 컴포넌트로 비활성화 요청
                } else {
                    // 2. 비활성화된 상태의 라디오 버튼을 누른 경우 -> 새로운 활성 스타일로 설정 요청
                    onStyleSelected(style.styleId) // 상위 컴포넌트로 활성 스타일 변경 요청
                }
            }

            // 항목 전체를 클릭했을 때도 라디오 버튼과 동일하게 작동하도록 처리 (선택 사항)
            binding.root.setOnClickListener {
                // 라디오 버튼의 클릭 로직을 그대로 호출
                binding.styleRadioButton.performClick()
            }

            // 3. 삭제 버튼
            binding.deleteStyleButton.setOnClickListener {
                // ... (기존 삭제 로직 유지: 활성 스타일일 경우 비활성화 요청)
                if (isSelected) {
                    Toast.makeText(binding.root.context, "현재 스타일을 해제합니다.", Toast.LENGTH_SHORT).show()
                    onStyleDeactivated()
                } else {
                    onDeleteClicked(style)
                }
            }
        }
    }
}