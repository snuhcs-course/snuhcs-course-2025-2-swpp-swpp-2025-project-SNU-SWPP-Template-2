package com.example.librarytogether.feature.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.librarytogether.databinding.ItemChipSelectableBinding

/**
 * 온보딩 설문에서 Chip 목록(책/작가/장르 등)을 선택할 때 사용하는 어댑터.
 * ListAdapter를 사용하여 성능을 최적화하고, 선택 상태를 효율적으로 관리함.
 *
 * @param onToggle 칩의 선택 상태가 변경될 때 호출되는 콜백. (id, isSelected)
 */
class ChipSelectableAdapter(
    private val onToggle: (id: Int, isSelected: Boolean) -> Unit
) : ListAdapter<ChipSelectableAdapter.Item, ChipSelectableAdapter.VH>(ItemDiffCallback()) {

    /**
     * 표시될 아이템 데이터 구조.
     * 이제 'checked' 상태는 어댑터 외부(ViewModel)에서 관리되므로 제거함.
     */
    data class Item(
        val id: Int,
        val label: String
    )

    // 현재 선택된 아이템들의 ID를 저장하는 Set.
    private var selectedIds = emptySet<Int>()

    /**
     * 선택 상태가 변경되었을 때 호출되어 UI를 효율적으로 갱신.
     * @param newSelectedIds 새로 선택된 아이템 ID의 Set.
     */
    fun updateSelection(newSelectedIds: Set<Int>) {
        val oldSelection = this.selectedIds
        this.selectedIds = newSelectedIds

        // 변경이 필요한 아이템만 찾아서 갱신 (전체 갱신 방지)
        (oldSelection + newSelectedIds).forEach { id ->
            val position = currentList.indexOfFirst { it.id == id }
            if (position != -1) {
                notifyItemChanged(position)
            }
        }
    }

    /** ViewHolder */
    inner class VH(private val binding: ItemChipSelectableBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // setOnClickListener는 한 번만 설정하는 것이 효율적.
            binding.chip.setOnClickListener {
                // 어댑터가 현재 아이템의 위치를 알고 있으므로, 이를 이용해 ID를 찾음.
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)
                    // 현재 선택 상태의 반대로 토글.
                    onToggle(item.id, !selectedIds.contains(item.id))
                }
            }
        }

        fun bind(item: Item) {
            binding.chip.text = item.label
            // 현재 아이템의 ID가 selectedIds Set에 포함되어 있는지 여부로 checked 상태를 결정.
            binding.chip.isChecked = selectedIds.contains(item.id)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemChipSelectableBinding.inflate(inflater, parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    /** DiffUtil.ItemCallback: ListAdapter가 아이템 변경을 효율적으로 계산하도록 돕는 클래스 */
    class ItemDiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            // 아이템의 고유 ID가 같으면 같은 아이템으로 간주.
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            // 아이템의 내용(여기서는 label)이 같으면 내용이 같다고 간주.
            return oldItem == newItem
        }
    }
}
