package com.example.mindlog.features.auth.data.dto

data class TokenResponseEnvelope(
    val data: TokenData
)

data class TokenData(
    val access: String,
    val refresh: String
)