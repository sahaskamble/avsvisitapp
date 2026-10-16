package com.avs.visitapp

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS) // connect timeout
    .readTimeout(15, TimeUnit.SECONDS)    // server response read timeout
    .writeTimeout(15, TimeUnit.SECONDS)   // upload/write timeout
    .addInterceptor { chain ->
        val original: Request = chain.request()
        val requestBuilder = original.newBuilder()
            .header("Content-Type", "application/json")
        val request = requestBuilder.build()
        chain.proceed(request)
    }
    .build()
