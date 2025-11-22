package com.example.mindlog.core.model

data class Keyword(
    val keyword: String,
    val emotion: String,
    val summary: String?,
    val weight: Float
)
