package com.example.librarytogether.feature.bookdetail.data

data class BookDetail(
    val id: String,
    val title: String,
    val author: String,
    val publisher: String?,
    val isbn: String?,
    val coverUrl: String?,
    val description: String?,
    val barterable: Boolean,
)
