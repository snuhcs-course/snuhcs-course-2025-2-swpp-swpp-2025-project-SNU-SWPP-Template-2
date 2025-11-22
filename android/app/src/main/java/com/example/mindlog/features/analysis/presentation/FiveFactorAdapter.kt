package com.example.mindlog.features.analysis.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.example.mindlog.R
import com.google.android.material.card.MaterialCardView

class FiveFactorAdapter(
    private val items: List<Pair<String, String>>,
    private val useColorfulBackground: Boolean = false
) : RecyclerView.Adapter<FiveFactorAdapter.FactorViewHolder>() {

    inner class FactorViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvFactorTitle)
        val body: TextView = view.findViewById(R.id.tvFactorBody)
        val lottie: LottieAnimationView = view.findViewById(R.id.lottieFactor)
    }

    // 5가지 파스텔 색 (colors.xml 에 정의해둘 예정)
    private val cardColors = listOf(
        R.color.factor_conscientious_bg, // 성실성
        R.color.factor_neuroticism_bg,   // 정서 안정성
        R.color.factor_extraversion_bg,  // 외향성
        R.color.factor_openness_bg,      // 개방성
        R.color.factor_agreeableness_bg  // 수용성
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FactorViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_five_factor, parent, false)
        return FactorViewHolder(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: FactorViewHolder, position: Int) {
        val (title, desc) = items[position]
        holder.title.text = title
        holder.body.text = desc

        val animRes = when (position) {
            0 -> R.raw.anim_conscientious
            1 -> R.raw.anim_neuroticism
            2 -> R.raw.anim_extraversion
            3 -> R.raw.anim_openness
            4 -> R.raw.anim_agreeableness
            else -> R.raw.anim_analysis
        }

        if (useColorfulBackground) {
            holder.lottie.setAnimation(animRes)
        } else {
            holder.lottie.setAnimation(R.raw.anim_analysis)
        }
        holder.lottie.playAnimation()

        val card = holder.view as MaterialCardView

        if (useColorfulBackground) {
            val colorRes = cardColors[position % cardColors.size]
            val colorInt = ContextCompat.getColor(
                holder.view.context,
                colorRes
            )
            card.setCardBackgroundColor(colorInt)
        } else {
            val defaultColor = ContextCompat.getColor(
                holder.view.context,
                R.color.five_factor_default_bg
            )
            card.setCardBackgroundColor(defaultColor)
        }
    }
}