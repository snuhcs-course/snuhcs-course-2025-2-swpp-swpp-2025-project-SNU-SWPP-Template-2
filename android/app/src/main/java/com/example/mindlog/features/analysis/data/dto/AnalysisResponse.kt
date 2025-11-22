package com.example.mindlog.features.analysis.data.dto

import com.google.gson.annotations.SerializedName
import java.io.Serial


data class UserTypeResponse(
    @SerializedName("user_type") val userType: String,
    @SerializedName("description") val description: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class ComprehensiveAnalysisResponse(
    @SerializedName("conscientiousness") val conscientiousness: String,
    @SerializedName("neuroticism") val neuroticism: String,
    @SerializedName("extraversion") val extraversion: String,
    @SerializedName("openness") val openness: String,
    @SerializedName("agreeableness") val agreeableness: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class PersonalizedAdviceResponse(
    @SerializedName("advice_type") val adviceType: String,
    @SerializedName("personalized_advice") val personalizedAdvice: String,
    @SerializedName("updated_at") val updatedAt: String
)

