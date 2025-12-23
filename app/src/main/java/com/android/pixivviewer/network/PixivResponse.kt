package com.android.pixivviewer.network

import com.google.gson.annotations.SerializedName

// 外層 Response
data class IllustRecommendedResponse(
    val illusts: List<Illust>,
    @SerializedName("next_url") val nextUrl: String?
)

data class IllustDetailResponse(
    val illust: Illust
)

// 核心結構
data class Illust(
    val id: Long,
    val title: String,
    @SerializedName("image_urls") val imageUrls: ImageUrls,
    val width: Int,
    val height: Int,
    @SerializedName("meta_single_page") val metaSinglePage: MetaSinglePage?,
    @SerializedName("page_count") val pageCount: Int = 1,
    @SerializedName("meta_pages") val metaPages: List<MetaPage> = emptyList(), // 這裡原本可能少了逗號

    // 作者資訊
    val user: User,

    // 是否收藏
    @SerializedName("is_bookmarked") val isBookmarked: Boolean = false,
    val tags: List<Tag> = emptyList(),
    val type: String = "illust",

    // ✨ 新增：AI 生成標記 (0=非AI, 1=AI, 2=?)
    @SerializedName("illust_ai_type") val illustAiType: Int = 0,
    @SerializedName("create_date") val createDate: String,
    @SerializedName("total_view") val totalView: Int,
    @SerializedName("total_bookmarks") val totalBookmarks: Int
)

data class Tag(
    val name: String,
    @SerializedName("translated_name") val translatedName: String? // 有時候會有翻譯 (例如日文轉中文)
)

// 作者與圖片結構
data class User(
    val id: Long,
    val name: String,
    val account: String,
    @SerializedName("profile_image_urls") val profileImageUrls: ProfileImageUrls,
    @SerializedName("is_followed") val isFollowed: Boolean = false,
)

data class ProfileImageUrls(
    val medium: String
)

data class ImageUrls(
    @SerializedName("square_medium") val squareMedium: String,
    @SerializedName("medium") val medium: String,
    @SerializedName("large") val large: String?
)

data class MetaSinglePage(
    @SerializedName("original_image_url") val originalImageUrl: String?
)

data class MetaPage(
    @SerializedName("image_urls") val imageUrls: MetaPageImageUrls
)

data class MetaPageImageUrls(
    @SerializedName("square_medium") val squareMedium: String?,
    @SerializedName("medium") val medium: String?,
    @SerializedName("large") val large: String?,
    @SerializedName("original") val original: String?
)

data class TrendingTagsResponse(
    @SerializedName("trend_tags") val trendTags: List<TrendTag>
)

data class TrendTag(
    val tag: String,
    @SerializedName("translated_name") val translatedName: String?,
    val illust: Illust // 每個標籤會附帶一張插畫，我們拿來當背景圖
)

data class UserDetailResponse(
    val user: User,
    val profile: UserProfile
)

// ✨ 新增：用戶統計數據
data class UserProfile(
    @SerializedName("total_follow_users") val totalFollowUsers: Int, // 關注數
    @SerializedName("total_illust_bookmarks_public") val totalBookmarks: Int, // 公開收藏數
    @SerializedName("total_illusts") val totalIllusts: Int = 0
)

data class UserFollowingResponse(
    @SerializedName("user_previews") val userPreviews: List<UserPreview>,
    @SerializedName("next_url") val nextUrl: String?
)

// ✨ 新增：用戶預覽 (包含用戶資訊 + 最近作品)
data class UserPreview(
    val user: User,
    val illusts: List<Illust> // 這裡通常會回傳最近的 3 張圖
)