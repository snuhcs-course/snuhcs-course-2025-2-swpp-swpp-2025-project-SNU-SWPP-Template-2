package com.example.sumdays.data.style

data class StylePrompt(
    val common_phrases: List<String>,
    val emotional_tone: String,
    val formality: String,
    val irony_or_sarcasm: String,
    val lexical_choice: String,
    val pacing: String,
    val sentence_endings: List<String>,
    val sentence_length: String,
    val sentence_structure: String,
    val slang_or_dialect: String,
    val tone: String
)