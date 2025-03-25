package com.example.picktimeapp.di

import com.example.picktimeapp.auth.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenManager.getAccessToken().first() }
        val originalRequest = chain.request()

        // 👇 login 요청에는 Authorization 헤더를 붙이지 않음
        if (originalRequest.url.encodedPath.endsWith("/login")) {
            return chain.proceed(originalRequest)
        }

        val newRequest = chain.request().newBuilder().apply {
            token?.let {
                addHeader("Authorization", it)
            }
        }.build()

        return chain.proceed(newRequest)
    }
}
