package com.aisportspt.app.data.remote
import com.google.gson.annotations.SerializedName

data class RecommendationDto (
    val name: String,
    val price: Int,
    val level: Int,
    @SerializedName("image_url")
    val imageUrl: String?
)