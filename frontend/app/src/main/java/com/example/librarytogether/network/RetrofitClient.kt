package com.example.librarytogether.network

import java.util.concurrent.TimeUnit
import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8000/"

    @Volatile
    private var retrofit: Retrofit? = null

    fun getClient(context: Context): Retrofit {
        retrofit?.let { return it }

        synchronized(this) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val okHttp: OkHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .addInterceptor(AuthInterceptor(context.applicationContext))
                .authenticator(TokenAuthenticator(context.applicationContext, BASE_URL))
                .build()

            val built = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttp)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            retrofit = built
            return built
        }
    }
}