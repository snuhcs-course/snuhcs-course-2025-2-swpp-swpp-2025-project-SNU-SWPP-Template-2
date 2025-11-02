package com.example.librarytogether.feature.onboarding.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.librarytogether.databinding.ItemChipSelectableBinding

class ChipSelectableAdapter(
    private val onToggle: (id: Int, checked: Boolean) -> Unit
) : RecyclerView.Adapter<ChipSelectableAdapter.VH>() {

    data class Item(val id: Int, val label: String, var checked: Boolean = false)

    private val items = mutableListOf<Item>()

    fun submit(list: List<Item>, selected: Set<Int>) {
        items.clear()
        items += list.map { it.copy(checked = selected.contains(it.id)) }
        notifyDataSetChanged()
    }

    val selectedIds: List<Int> get() = items.filter { it.checked }.map { it.id }

    inner class VH(val b: ItemChipSelectableBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: Item) {
            b.chip.text = item.label
            b.chip.isChecked = item.checked
            b.chip.setOnClickListener {
                val now = !item.checked
                item.checked = now
                b.chip.isChecked = now
                onToggle(item.id, now)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inf = LayoutInflater.from(parent.context)
        val binding = ItemChipSelectableBinding.inflate(inf, parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size
}
