package com.example.mindlog.features.journal.data.dto

import com.google.gson.annotations.SerializedName

data class ImageUploadRequest(
    @SerializedName("filename")
    val filename: String,
    @SerializedName("content_type")
    val contentType: String
)
    