package com.example.librarytogether.feature.search.data

data class SearchItem(
    val id: String,
    val title: String,
    val authors: List<Int>,
    val publisher: Int?,
    val isbn13: String?,
    val cover_image: String? = null,
    val is_for_barter: Boolean?
)

data class SearchResponse(
    val results: List<SearchItem>
)
