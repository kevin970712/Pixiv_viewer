package com.android.pixivviewer.network

import com.android.pixivviewer.viewmodel.FollowingApi
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface PixivApiClient : FollowingApi {
    // 獲取推薦插畫
    @GET("/v1/illust/recommended")
    suspend fun getRecommendedIllusts(
        @Query("filter") filter: String = "for_ios",
        @Query("include_ranking_label") includeRankingLabel: Boolean = true
    ): IllustRecommendedResponse

    @GET("/v1/illust/detail")
    suspend fun getIllustDetail(
        @Query("illust_id") illustId: Long
    ): IllustDetailResponse

    @GET("/v1/search/illust")
    suspend fun searchIllusts(
        @Query("word") word: String,
        @Query("search_target") searchTarget: String = "partial_match_for_tags",
        @Query("sort") sort: String = "date_desc",
        @Query("filter") filter: String = "for_ios"
    ): IllustRecommendedResponse

    @POST("/v2/illust/bookmark/add")
    @FormUrlEncoded
    suspend fun addBookmark(
        @Field("illust_id") illustId: Long,
        @Field("restrict") restrict: String = "public"
    )

    // ✨ 新增：移除收藏
    @POST("/v1/illust/bookmark/delete")
    @FormUrlEncoded
    suspend fun deleteBookmark(
        @Field("illust_id") illustId: Long
    )

    @GET("/v1/trending-tags/illust")
    suspend fun getTrendingTags(
        @Query("filter") filter: String = "for_ios"
    ): TrendingTagsResponse

    @GET("/v1/user/detail")
    suspend fun getUserDetail(
        @Query("user_id") userId: Long,
        @Query("filter") filter: String = "for_ios"
    ): UserDetailResponse

    // ✨ 新增：獲取用戶收藏 (restrict=public 為公開收藏)
    @GET("/v1/user/bookmarks/illust")
    suspend fun getUserBookmarks(
        @Query("user_id") userId: Long,
        @Query("restrict") restrict: String = "public"
    ): IllustRecommendedResponse

    @GET("/v1/illust/ranking")
    suspend fun getIllustRanking(
        @Query("mode") mode: String = "day",
        @Query("filter") filter: String = "for_ios"
    ): IllustRecommendedResponse

    @GET("/v2/illust/related")
    suspend fun getRelatedIllusts(
        @Query("illust_id") illustId: Long,
        @Query("filter") filter: String = "for_ios"
    ): IllustRecommendedResponse

    @GET("/v1/user/illusts")
    suspend fun getUserIllusts(
        @Query("user_id") userId: Long,
        @Query("type") type: String = "illust",
        @Query("filter") filter: String = "for_ios"
    ): IllustRecommendedResponse

    // 關注用戶
    @POST("/v1/user/follow/add")
    @FormUrlEncoded
    suspend fun followUser(
        @Field("user_id") userId: Long,
        @Field("restrict") restrict: String = "public"
    )

    // 取消關注用戶
    @POST("/v1/user/follow/delete")
    @FormUrlEncoded
    suspend fun unfollowUser(
        @Field("user_id") userId: Long
    )

    @GET("/v2/illust/follow")
    suspend fun getFollowIllusts(
        @Query("restrict") restrict: String = "public",
        @Query("filter") filter: String = "for_ios"
    ): IllustRecommendedResponse

    // 獲取正在關注的用戶列表
    @GET("/v1/user/following")
    suspend fun getUserFollowing(
        @Query("user_id") userId: Long,
        @Query("restrict") restrict: String = "public",
        @Query("filter") filter: String = "for_ios"
    ): UserFollowingResponse

    @GET
    suspend fun getNextPage(@Url url: String): IllustRecommendedResponse
}