package com.example.sumdays.network

import com.google.gson.annotations.SerializedName

data class OcrResponse(
    val success: Boolean,
    val type: String,
    @SerializedName("result") val result: String?
)