package com.example.mindlog.features.statistics.data.dto

import android.health.connect.datatypes.units.Percentage
import com.example.mindlog.features.statistics.domain.model.EmotionRate
import com.google.gson.annotations.SerializedName


data class EmotionRatesResponse(
    @SerializedName("total_count") val totalCount: Int,
    @SerializedName("statistics") val statistics: List<EmotionRateItem>
)

data class EmotionRateItem(
    @SerializedName("emotion") val emotion: String,
    @SerializedName("count") val count: Int,
    @SerializedName("percentage") val percentage: Float
)