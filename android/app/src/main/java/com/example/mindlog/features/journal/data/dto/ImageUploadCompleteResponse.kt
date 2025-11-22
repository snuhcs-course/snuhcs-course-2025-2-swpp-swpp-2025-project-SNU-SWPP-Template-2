package com.example.mindlog.features.journal.data.dto

import com.google.gson.annotations.SerializedName
import java.util.Date

data class ImageUploadCompleteResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("journal_id")
    val journalId: Int,
    @SerializedName("s3_key")
    val s3Key: String,
    @SerializedName("created_at")
    val createdAt: Date
)
    