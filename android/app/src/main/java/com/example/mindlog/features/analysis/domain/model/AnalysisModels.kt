package com.example.mindlog.features.analysis.domain.model

data class UserType(
    val userType: String,
    val description: String,
    val updatedAt: String
)

data class ComprehensiveAnalysis(
    val conscientiousness: String,
    val neuroticism: String,
    val extraversion: String,
    val openness: String,
    val agreeableness: String,
    val updatedAt: String
)

data class PersonalizedAdvice(
    val adviceType: String,
    val personalizedAdvice: String,
    val updatedAt: String
)