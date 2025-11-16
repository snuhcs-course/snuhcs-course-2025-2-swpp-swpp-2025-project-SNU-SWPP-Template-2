package com.example.sumdays.settings.ui

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

class CenterScaleOnScrollListener : RecyclerView.OnScrollListener() {
    override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
        val lm = rv.layoutManager as? LinearLayoutManager ?: return
        val mid = rv.width / 2f
        val scaleMax = 1.0f
        val scaleMin = 0.92f

        for (i in 0 until rv.childCount) {
            val child: View = rv.getChildAt(i) ?: continue
            val childMid = (child.left + child.right) / 2f
            val d = abs(mid - childMid) / mid
            val scale = scaleMax - (scaleMax - scaleMin) * d.coerceIn(0f, 1f)
            child.scaleY = scale
            child.scaleX = scale
        }
    }
}
