package com.android.pixivviewer.viewmodel

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pixivviewer.network.PixivAuthApi
import com.android.pixivviewer.utils.PixivHeaders
import com.android.pixivviewer.utils.PkceGenerator
import com.android.pixivviewer.utils.TokenManager
import com.android.pixivviewer.utils.VerifierStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoginViewModel : ViewModel() {

    private val _loginState = MutableStateFlow("尚未登入")
    val loginState = _loginState.asStateFlow()

    private val _isLoginSuccess = MutableStateFlow(false)
    val isLoginSuccess = _isLoginSuccess.asStateFlow()

    /**
     * ✨ 新增：使用 Custom Tabs 啟動登入
     */
    fun startLoginWithCustomTabs(context: Context) {
        // 1. 生成 URL (邏輯與之前相同)
        val verifier = PkceGenerator.generateVerifier()
        VerifierStore.saveVerifier(context, verifier)
        val codeChallenge = PkceGenerator.generateChallenge(verifier)
        val loginUrl = "https://app-api.pixiv.net/web/v1/login" +
                "?code_challenge=$codeChallenge" +
                "&code_challenge_method=S256" +
                "&client=pixiv-android"

        // 2. 建立並啟動 CustomTabsIntent
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()

        customTabsIntent.launchUrl(context, Uri.parse(loginUrl))
    }

    /**
     * ✨ 接收來自 MainActivity 的回調
     */
    fun handleCallback(context: Context, uri: Uri) {
        val code = uri.getQueryParameter("code")
        if (code != null) {
            _loginState.value = "獲取 Code 成功，正在交換 Token..."
            exchangeToken(context, code)
        } else {
            _loginState.value = "登入失敗：未獲取到 Code"
        }
    }

    // 3. 核心：用 Code 換 Token
    private fun exchangeToken(context: Context, code: String) {
        // 從儲存空間讀回 Verifier
        val savedVerifier = VerifierStore.getVerifier(context)

        if (savedVerifier.isEmpty()) {
            _loginState.value = "錯誤：找不到 Code Verifier，請重新嘗試"
            return
        }

        // 定義 Header Interceptor (解決 HTTP 400 錯誤)
        val headerInterceptor = okhttp3.Interceptor { chain ->
            val originalRequest = chain.request()

            // 取得計算好的 Headers (包含 Hash)
            val pixivHeaders = PixivHeaders.getHeaders()

            val newRequestBuilder = originalRequest.newBuilder()

            // 注入 Header
            pixivHeaders.forEach { (key, value) ->
                newRequestBuilder.header(key, value)
            }

            // 指定 Host
            newRequestBuilder.header("Host", "oauth.secure.pixiv.net")

            chain.proceed(newRequestBuilder.build())
        }

        // 建立 OkHttp Client
        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
            .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
                level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
            })
            .build()

        // 建立 Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("https://oauth.secure.pixiv.net/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(PixivAuthApi::class.java)

        viewModelScope.launch {
            try {
                val response = api.exchangeToken(
                    code = code,
                    codeVerifier = savedVerifier,
                    redirectUri = "https://app-api.pixiv.net/web/v1/users/auth/pixiv/callback"
                )

                _loginState.value = "登入成功！ID: ${response.user.id}"

                // ✨ 關鍵修改 1：把 Token 存起來！
                TokenManager.saveTokens(
                    context = context,
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken,
                    userId = response.user.id
                )

                // ✨ 關鍵修改 2：通知 UI 切換畫面
                _isLoginSuccess.value = true

            } catch (e: Exception) {
                _loginState.value = "錯誤: ${e.message}"
                e.printStackTrace()
            }
        }
    }
}