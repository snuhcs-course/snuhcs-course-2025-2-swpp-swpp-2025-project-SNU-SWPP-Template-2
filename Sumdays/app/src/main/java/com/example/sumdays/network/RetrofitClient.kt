package com.example.sumdays.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // AWS 서버의 Public IP 주소 또는 도메인 주소
    private const val BASE_URL = "http://15.164.103.159:3000/"

    // Retrofit 인스턴스를 생성. 외부에서 직접 접근할 필요가 없으므로 private으로 설정합니다.
    private val retrofitInstance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // AuthApiService의 싱글톤 인스턴스.
    // 이제 앱의 모든 곳에서 RetrofitClient.authApiService 로 API 서비스를 호출할 수 있습니다.
    val authApiService: AuthApiService by lazy {
        retrofitInstance.create(AuthApiService::class.java)
    }
}

