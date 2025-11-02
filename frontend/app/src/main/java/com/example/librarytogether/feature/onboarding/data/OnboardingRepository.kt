package com.example.librarytogether.feature.onboarding.data

import android.util.Log
import javax.inject.Inject

class OnboardingRepository @Inject constructor(
    private val api: OnboardingApi
) {
    suspend fun books(q: String?) = runCatching { api.searchBooks(q) }
        .onFailure { Log.e("OnboardingRepo", "books", it) }
        .getOrNull()?.body().orEmpty()

    suspend fun authors(q: String?) = runCatching { api.searchAuthors(q) }
        .onFailure { Log.e("OnboardingRepo", "authors", it) }
        .getOrNull()?.body().orEmpty()

    suspend fun genres(q: String?) = runCatching { api.searchGenres(q) }
        .onFailure { Log.e("OnboardingRepo", "genres", it) }
        .getOrNull()?.body().orEmpty()

    suspend fun submit(req: OnboardingSubmitRequest): Boolean =
        runCatching { api.submit(req) }
            .onFailure { Log.e("OnboardingRepo", "submit", it) }
            .getOrNull()?.isSuccessful == true
}
