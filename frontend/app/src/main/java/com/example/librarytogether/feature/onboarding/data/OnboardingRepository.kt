package com.example.librarytogether.feature.onboarding.data

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

class OnboardingRepository @Inject constructor(
    private val api: OnboardingApi
) {

    private val selectedBooks = mutableListOf<String>()
    private val selectedAuthors = mutableListOf<String>()
    private val selectedGenres = mutableListOf<String>()

    suspend fun getBooks(): List<LabelId> {
        return api.getBooks().map { LabelId(it.id, it.name) }
    }

    suspend fun getAuthors(): List<LabelId> {
        return api.getAuthors().map { LabelId(it.id, it.name) }
    }

    suspend fun getGenres(): List<LabelId> {
        return api.getGenres().map { LabelId(it.id, it.name) }
    }

    fun saveSelection(step: Int, ids: List<String>) {
        when (step) {
            0 -> {
                selectedBooks.clear()
                selectedBooks.addAll(ids)
            }
            1 -> {
                selectedAuthors.clear()
                selectedAuthors.addAll(ids)
            }
            2 -> {
                selectedGenres.clear()
                selectedGenres.addAll(ids)
            }
        }
    }

    suspend fun submitSelections(): Boolean {
        val req = OnboardingSubmitRequest(
            // 백엔드 Serializer가 String과 Int를 모두 처리하므로, 그대로 전송
            book_ids = selectedBooks,
            author_ids = selectedAuthors.mapNotNull { it.toIntOrNull() },
            genre_ids = selectedGenres.mapNotNull { it.toIntOrNull() }
        )
        Log.d("OnboardingRepository", "Submitting: $req")
        val response = api.submit(req)
        return response.isSuccessful
    }
}
