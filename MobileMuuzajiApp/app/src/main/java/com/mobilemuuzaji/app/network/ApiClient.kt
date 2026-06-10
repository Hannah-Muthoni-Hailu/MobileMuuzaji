package com.mobilemuuzaji.app.network

object ApiClient {
    val apiService: ApiService by lazy {
        RetrofitClient.instance.create(ApiService::class.java)
    }
}