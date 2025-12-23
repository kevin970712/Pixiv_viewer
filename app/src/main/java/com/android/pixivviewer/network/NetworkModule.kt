package com.android.pixivviewer.network

import android.content.Context
import android.util.Log
import com.android.pixivviewer.utils.PixivHeaders
import com.android.pixivviewer.utils.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {

    // 用於刷新的 Auth API 實例
    private val authApi: PixivAuthApi by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .apply { PixivHeaders.getHeaders().forEach { (k, v) -> header(k, v) } }
                    .header("Host", "oauth.secure.pixiv.net")
                    .build()
                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl("https://oauth.secure.pixiv.net/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PixivAuthApi::class.java)
    }

    fun provideApiClient(context: Context): PixivApiClient {
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()

            PixivHeaders.getHeaders().forEach { (k, v) -> builder.header(k, v) }

            val token = TokenManager.getAccessToken(context)
            if (token != null) {
                builder.header("Authorization", "Bearer $token")
            }

            builder.header("Host", "app-api.pixiv.net")
            chain.proceed(builder.build())
        }

        // 自動刷新 Token
        val refreshInterceptor = Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)

            // 檢查是否是 400 錯誤且包含 invalid_grant (代表 Token 過期)
            if (response.code == 400) {
                val errorBody = response.peekBody(Long.MAX_VALUE).string()
                if (errorBody.contains("invalid_grant")) {
                    Log.d("PixivNetwork", "Token expired, trying to refresh...")

                    // 關閉舊回應
                    response.close()

                    // 同步執行刷新
                    val newToken = refreshTokenSynchronously(context)

                    if (newToken != null) {
                        Log.d("PixivNetwork", "Token refreshed successfully!")
                        // 使用新 Token 重建請求
                        val newRequest = request.newBuilder()
                            .header("Authorization", "Bearer $newToken")
                            .build()
                        // 重試請求
                        return@Interceptor chain.proceed(newRequest)
                    } else {
                        Log.e("PixivNetwork", "Failed to refresh token. Force logout.")
                    }
                }
            }
            response
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(refreshInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

        return Retrofit.Builder()
            .baseUrl("https://app-api.pixiv.net/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PixivApiClient::class.java)
    }

    // 同步刷新 Token
    private fun refreshTokenSynchronously(context: Context): String? {
        val refreshToken = TokenManager.getRefreshToken(context) ?: return null

        return try {
            // 使用 runBlocking 強制將 Coroutine 轉為同步執行
            runBlocking {
                val response = authApi.refreshToken(refreshToken = refreshToken)
                // 儲存新 Token
                TokenManager.saveTokens(
                    context,
                    response.accessToken,
                    response.refreshToken,
                    response.user.id
                )
                response.accessToken // 回傳新的 Access Token
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}