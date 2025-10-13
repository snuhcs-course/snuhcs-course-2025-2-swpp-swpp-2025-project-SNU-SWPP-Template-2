package com.example.librarytogether.network

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(val context: Context) : Interceptor {
    private val publicPaths = setOf(
        "/auth/signup/",
        "/auth/login/",
        "/auth/refresh/"
    )
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val original = chain.request()
        val url = original.url
        val path = url.encodedPath
        if (publicPaths.any { path.startsWith(it) }) {
            return chain.proceed(original)
        }
        val token = AuthManager.getAccessToken(context)
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}