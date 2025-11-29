package com.example.librarytogether.feature.onboarding.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface OnboardingApi {

//    @GET("api/books")
//    suspend fun getBookList(): Response<List<LabelId>>
//
//    @GET("api/authors")
//    suspend fun getAuthorList(): Response<List<LabelId>>
//
//    @GET("api/genres")
//    suspend fun getGenreList(): Response<List<LabelId>>

    @POST("onboarding/submit/")
    suspend fun submit(@Body body: OnboardingSubmitRequest): Response<Unit>
}
