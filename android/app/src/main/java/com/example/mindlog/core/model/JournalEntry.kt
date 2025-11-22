package com.example.mindlog.core.model

import java.util.Date

data class JournalEntry(
    val id: Int,
    val title: String,
    val content: String,
    val createdAt: Date,
    val imageUrl: String?,
    val keywords: List<Keyword>,
    val emotions: List<Emotion>,
    val gratitude: String?
)
