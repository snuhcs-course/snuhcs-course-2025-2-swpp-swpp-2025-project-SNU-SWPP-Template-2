package com.example.sumdays.data.style

data class StylePrompt(
    // 0. 컨셉 정의
    val character_concept: String,
    // 1. 기본 언어 구조
    val tone: String,
    val formality: String,
    val sentence_length: String,
    val sentence_structure: String,
    val pacing: String,
    // 2. 캐릭터 시그니처
    val sentence_endings: List<String>,
    val speech_quirks: String,
    val punctuation_style: String,
    val special_syntax: String,
    // 3. 어휘
    val lexical_choice: String,
    val emotional_tone: String
)