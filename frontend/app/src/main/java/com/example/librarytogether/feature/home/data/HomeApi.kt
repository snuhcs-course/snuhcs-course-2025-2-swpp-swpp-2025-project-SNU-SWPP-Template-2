package com.example.librarytogether.feature.home.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface HomeApi {
    @GET("home/")
    suspend fun feed(): Response<FeedResponse>

}
