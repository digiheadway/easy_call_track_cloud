package com.miniclick.calltrackmanage.network

import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object NetworkClient {
    private const val BASE_URL = "https://api.miniclickcrm.com/api/"

    // Only log in debug builds - reduces overhead significantly in production
    private val logging = HttpLoggingInterceptor().apply {
        level = if (com.miniclick.calltrackmanage.BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    // Retry interceptor for transient network failures
    private val retryInterceptor = Interceptor { chain ->
        val request = chain.request()
        var response = chain.proceed(request)
        var tryCount = 0
        val maxRetries = 3

        while (!response.isSuccessful && tryCount < maxRetries) {
            tryCount++
            // Only retry on server errors (5xx) or timeout-like errors
            if (response.code in 500..599) {
                response.close()
                Thread.sleep(1000L * tryCount) // Exponential-ish backoff
                response = chain.proceed(request)
            } else {
                break
            }
        }
        response
    }

    // Connection pool for reusing HTTP connections
    private val connectionPool = ConnectionPool(
        maxIdleConnections = 5,
        keepAliveDuration = 5,
        timeUnit = TimeUnit.MINUTES
    )

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor(retryInterceptor)
        .connectionPool(connectionPool)
        .connectTimeout(30, TimeUnit.SECONDS)  // Reduced from 60s for faster failure detection
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)     // Increased for large file uploads
        .retryOnConnectionFailure(true)         // Auto-retry on connection failures
        .build()

    val api: CallCloudApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(CallCloudApi::class.java)
    }
}
