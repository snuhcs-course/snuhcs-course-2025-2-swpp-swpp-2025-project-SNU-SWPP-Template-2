package com.example.sumdays.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.Response
import com.google.gson.JsonObject
import retrofit2.http.Multipart
import okhttp3.MultipartBody

interface AuthApiService {
    // POST 요청을 /api/login 주소로 보낸다.
    // @Body 어노테이션은 LoginRequest 객체를 JSON 형태로 변환하여 요청 본문에 담아달라는 의미.
    @POST("/api/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>
    // ★★★ 회원가입 API 함수 추가 ★★★
    @POST("api/signup")
    fun signup(@Body request: SignupRequest): Call<SignupResponse>

}
