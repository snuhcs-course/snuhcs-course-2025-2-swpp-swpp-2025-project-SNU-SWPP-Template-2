package com.example.librarytogether.feature.onboarding.data

import com.google.gson.annotations.SerializedName

data class BookDto(
    val id: Int,
    val title: String
)

data class AuthorDto(
    val id: Int,
    val name: String
)

data class GenreDto(
    val id: Int,
    val name: String
)

/** Payload to be submitted to the server */
data class OnboardingSubmitRequest(
    @SerializedName("book_ids")   val bookIds: List<Int>,
    @SerializedName("author_ids") val authorIds: List<Int>,
    @SerializedName("genre_ids")  val genreIds: List<Int>
)

/** 서Server response (e.g. token refresh/profile summary/feed seed, etc.) */
data class OnboardingSubmitResponse(
    val success: Boolean,
    val message: String? = null
)
