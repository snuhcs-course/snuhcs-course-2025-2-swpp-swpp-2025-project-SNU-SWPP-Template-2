package com.example.librarytogether.feature.bookdetail.data

data class BookDetail(
    val id: String,
    val title: String,
    val authors: String,
    val publisher: String?,
    val isbn_13: String?,
    val description: String?,
    val cover_image: String?,
    val is_for_barter: Boolean,
    val owner: String?
)
