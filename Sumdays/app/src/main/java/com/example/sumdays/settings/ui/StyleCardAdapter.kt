package com.example.sumdays.settings.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.R
import com.example.sumdays.data.style.UserStyle
import com.example.sumdays.databinding.ItemStyleAddCardBinding
import com.example.sumdays.databinding.ItemStyleCardBinding


class StyleCardAdapter(
    private val onSelect: (UserStyle?) -> Unit,
    private val onRename: (UserStyle, String) -> Unit,
    private val onDelete: (UserStyle) -> Unit,
    private val onAdd: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val items = mutableListOf<UserStyle>()
    private var activeId: Long? = null

    companion object {
        private const val TYPE_STYLE = 0
        private const val TYPE_ADD = 1
    }

    fun submit(list: List<UserStyle>, activeStyleId: Long?) {
        items.clear()

        // 기본 스타일 항상 첫 번째
        items.add(UserStyle.Default)

        // DB 스타일은 기본 스타일과 ID 충돌 없도록
        items.addAll(list.filter { it.styleId != 0L })

        this.activeId = activeStyleId
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) =
        if (position == items.size) TYPE_ADD else TYPE_STYLE

    override fun getItemCount(): Int = items.size + 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_STYLE) {
            StyleVH(ItemStyleCardBinding.inflate(inf, parent, false))
        } else {
            AddVH(ItemStyleAddCardBinding.inflate(inf, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is StyleVH) holder.bind(items[position])
        else (holder as AddVH).bind()
    }

    inner class StyleVH(private val b: ItemStyleCardBinding) : RecyclerView.ViewHolder(b.root) {
        private var flipped = false
        private var isFront = true

        fun bind(style: UserStyle) {
            // 기본 스타일 메뉴 삭제
            if (style.styleId == 0L) {
                b.moreButton.visibility = View.INVISIBLE
            } else {
                b.moreButton.visibility = View.VISIBLE
            }

            // 항상 초기 상태는 앞면
            b.front.visibility = View.VISIBLE
            b.back.visibility = View.GONE
            isFront = true

            // filp 애니메이션 효과
            b.flipContainer.setOnClickListener {
                isFront = !isFront
                applyFlipAnimation(isFront)
            }

            // 선택 상태에 따른 테두리 강조 효과
            if (style.styleId == activeId) {
                b.cardRoot.strokeWidth = (4 * b.root.resources.displayMetrics.density).toInt() // 2dp
            } else {
                b.cardRoot.strokeWidth = 0
            }

            // 기본 스타일 이름
            b.styleName.text = style.styleName

            // 샘플 일기 표시
            b.sampleDiary.text = style.sampleDiary.ifBlank { "샘플 생성 중..." }

            // 프롬프트
            val p = style.stylePrompt
            b.promptBody.text = buildString {
                p.tone.let { append("• Tone: $it\n") }
                p.formality.let { append("• Formality: $it\n") }
                p.sentence_length.let { append("• Sentence Length: $it\n") }
                p.sentence_structure.let { append("• Structure: $it\n") }

                p.sentence_endings.takeIf { it.isNotEmpty() }?.let {
                    append("• Endings: ${it.joinToString(", ")}\n")
                }

                p.lexical_choice.let { append("• Word Choice: $it\n") }

                p.common_phrases.takeIf { it.isNotEmpty() }?.let {
                    append("• Common Phrases: ${it.joinToString(", ")}\n")
                }

                p.emotional_tone.let { append("• Emotional Tone: $it\n") }
                p.irony_or_sarcasm.let { append("• Irony/Sarcasm: $it\n") }
                p.slang_or_dialect.let { append("• Slang/Dialect: $it\n") }
                p.pacing.let { append("• Pacing: $it\n") }
            }


            // flip
            b.flipContainer.setOnClickListener {
                flipped = !flipped
                b.front.visibility = if (flipped) View.GONE else View.VISIBLE
                b.back.visibility = if (flipped) View.VISIBLE else View.GONE
            }

            // ⋮ 메뉴
            b.moreButton.setOnClickListener { v ->
                PopupMenu(v.context, v).apply {
                    menuInflater.inflate(R.menu.menu_style_card, menu)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.action_rename -> {
                                showRenameDialog(style)
                                true
                            }
                            R.id.action_delete -> { onDelete(style); true }
                            else -> false
                        }
                    }
                }.show()
            }

            // 선택 상태(외부 버튼에서 처리하므로 카드에는 시각 피드백만)
            val isActive = (style.styleId == activeId)
            b.cardRoot.strokeWidth = if (isActive) (4 * b.root.resources.displayMetrics.density).toInt() else 0
            b.cardRoot.strokeColor = 0xFF8B008B.toInt()
        }

        private fun applyFlipAnimation(showFront: Boolean) {
            val scale = b.root.resources.displayMetrics.density
            b.flipContainer.cameraDistance = 6000 * scale

            val flipOut = ObjectAnimator.ofFloat(b.flipContainer, "rotationY", 0f, 90f).apply {
                duration = 200
            }
            val flipIn = ObjectAnimator.ofFloat(b.flipContainer, "rotationY", -90f, 0f).apply {
                duration = 200
            }

            flipOut.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (showFront) {
                        b.back.visibility = View.GONE
                        b.front.visibility = View.VISIBLE
                    } else {
                        b.front.visibility = View.GONE
                        b.back.visibility = View.VISIBLE
                    }
                    flipIn.start()
                }
            })

            flipOut.start()
        }

        private fun showRenameDialog(style: UserStyle) {
            val ctx = b.root.context
            val input = android.widget.EditText(ctx).apply {
                setText(style.styleName)
                setSelection(text.length)
            }
            AlertDialog.Builder(ctx)
                .setTitle("스타일 이름 변경")
                .setView(input)
                .setPositiveButton("확인") { _, _ ->
                    onRename(style, input.text.toString().ifBlank { defaultName(adapterPosition) })
                }
                .setNegativeButton("취소", null)
                .show()
        }

        private fun defaultName(idx: Int) = "스타일 ${idx + 1}"
    }

    inner class AddVH(private val b: ItemStyleAddCardBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind() { b.addRoot.setOnClickListener { onAdd() } }
    }

    /** 외부에서 현재 스냅된 포지션의 스타일을 얻고 싶을 때 */
    fun styleAt(position: Int): UserStyle? =
        if (position in 0 until items.size) items[position] else null

    fun updateActiveStyleId(activeId: Long?) {
        this.activeId = activeId
        notifyDataSetChanged() // 즉시 UI 갱신
    }

}
