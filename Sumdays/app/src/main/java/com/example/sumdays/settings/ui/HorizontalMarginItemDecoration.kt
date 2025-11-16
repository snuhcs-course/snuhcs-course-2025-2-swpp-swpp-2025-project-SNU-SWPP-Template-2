package com.example.sumdays.settings.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class HorizontalMarginItemDecoration(private val marginPx: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, v: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.left = if (parent.getChildAdapterPosition(v) == 0) marginPx else marginPx / 2
        outRect.right = if (parent.getChildAdapterPosition(v) == (parent.adapter?.itemCount ?: 1) - 1) marginPx else marginPx / 2
    }
}
