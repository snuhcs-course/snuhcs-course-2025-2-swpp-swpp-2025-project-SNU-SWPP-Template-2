package com.example.librarytogether.feature.onboarding.data

import retrofit2.Response
import retrofit2.http.*

interface OnboardingApi {

// TODO Endpoint String 을 실제 백 코드 맞춰 바꾸기 (/api/v1/...)

    @GET("onboarding/books/")
    suspend fun searchBooks(@Query("q") q: String? = null): Response<List<BookDto>>

    @GET("onboarding/authors/")
    suspend fun searchAuthors(@Query("q") q: String? = null): Response<List<AuthorDto>>

    @GET("onboarding/genres/")
    suspend fun searchGenres(@Query("q") q: String? = null): Response<List<GenreDto>>

    // survey submit (최초 로그인 1회)
    @POST("onboarding/submit/")
    suspend fun submit(@Body body: OnboardingSubmitRequest): Response<OnboardingSubmitResponse>
}


