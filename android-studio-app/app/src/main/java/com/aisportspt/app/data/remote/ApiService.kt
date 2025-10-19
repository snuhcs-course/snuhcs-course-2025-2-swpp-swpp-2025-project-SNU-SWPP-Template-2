package com.aisportspt.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET

data class SignupRequest(
    val name: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String?,
    val name: String?,
    val error: String?
)

interface ApiService {
    @POST("/api/accounts/signup/")
    suspend fun signup(@Body request: SignupRequest): Response<Map<String, String>>

    @POST("/api/accounts/login/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("/api/recommendation/")
    suspend fun getRecommendations(): RecommendationResponse

//    @GET("/api/recommendation/")
//    suspend fun getRecommendations(@Query("userId") userId: String): RecommendationResponse


//    @GET("/api/recommendation/")
//    suspend fun getRecommendations(@Header("Authorization") token: String): RecommendationResponse

}
