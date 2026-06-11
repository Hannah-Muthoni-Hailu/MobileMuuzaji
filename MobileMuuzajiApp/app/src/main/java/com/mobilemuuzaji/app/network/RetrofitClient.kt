package com.mobilemuuzaji.app.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "https://mobilemuuzaji.onrender.com"

    // Logs all network requests and responses in Logcat during development
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // OkHttp is the underlying HTTP client Retrofit uses
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)  // Connection timeout (free tier Render can be slow to spin up)
        .readTimeout(60, TimeUnit.SECONDS)     // Read timeout
        .writeTimeout(60, TimeUnit.SECONDS)    // Write timeout
        .build()

    // Retrofit instance — shared across the whole app
    val instance: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())  // converts JSON to Kotlin objects
        .build()
}