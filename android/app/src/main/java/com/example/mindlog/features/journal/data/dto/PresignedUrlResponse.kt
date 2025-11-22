package com.example.mindlog.features.journal.data.dto

import com.google.gson.annotations.SerializedName

data class PresignedUrlResponse(
    @SerializedName("presigned_url")
    val presignedUrl: String,
    @SerializedName("file_url")
    val fileUrl: String,
    @SerializedName("s3_key")
    val s3Key: String
)
    