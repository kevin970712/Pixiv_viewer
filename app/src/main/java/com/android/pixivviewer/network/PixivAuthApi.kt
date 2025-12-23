package com.android.pixivviewer.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

// 定義 Token 回傳的資料結構
data class PixivTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("user") val user: PixivUserResponse
)

data class PixivUserResponse(
    val id: String,
    val name: String,
    @SerializedName("profile_image_urls") val profileImageUrls: Map<String, String>
)

interface PixivAuthApi {
    @FormUrlEncoded
    @POST("/auth/token")
    suspend fun exchangeToken(
        @Field("client_id") clientId: String = "MOBrBDS8blbauoSck0ZfDbtuzpyT", // Pixiv 官方 App 的固定 ID
        @Field("client_secret") clientSecret: String = "lsACyCD94FhDUtGTXi3QzcFE2uU1hqtDaKeqrdwj", // Pixiv 官方 App 的固定 Secret
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("code") code: String,
        @Field("code_verifier") codeVerifier: String,
        @Field("redirect_uri") redirectUri: String = "pixiv://account/login",
        @Field("include_policy") includePolicy: Boolean = true
    ): PixivTokenResponse

    @FormUrlEncoded
    @POST("/auth/token")
    suspend fun refreshToken(
        @Field("client_id") clientId: String = "MOBrBDS8blbauoSck0ZfDbtuzpyT",
        @Field("client_secret") clientSecret: String = "lsACyCD94FhDUtGTXi3QzcFE2uU1hqtDaKeqrdwj",
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("refresh_token") refreshToken: String,
        @Field("include_policy") includePolicy: Boolean = true
    ): PixivTokenResponse
}