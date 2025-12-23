# ===================================================================
#         Pixiv Viewer 專案 ProGuard / R8 規則 (最終版)
# ===================================================================
# 說明：
# R8 會對程式碼進行壓縮、混淆和優化。
# 這些規則是為了告訴 R8 哪些程式碼不能被隨意更改，以避免 App 在發布後閃退。
# ===================================================================


# 1. 【最重要】保留所有網路資料模型 (Data Models)
# -------------------------------------------------------------------
# 這是避免 Release 版本閃退的關鍵。
# 如果不保留，R8 會將 data class 的欄位名 (如 isBookmarked) 混淆成 a, b, c...
# 這將導致 Gson 解析 JSON 時找不到對應的欄位，引發空指標或解析錯誤。
-keep class com.android.pixivviewer.network.** { *; }


# 2. 保留 Gson 使用的 @SerializedName 註解
# -------------------------------------------------------------------
# 這是 Gson 官方建議的規則，作為輔助，確保所有被 @SerializedName
# 標註的欄位名在混淆後保持不變。
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}


# 3. 處理 AndroidX 和 Jetpack Compose
# -------------------------------------------------------------------
# a. 保留所有 Composable 函式，防止被 R8 優化掉。
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keep public class * implements androidx.compose.runtime.Composer

# b. 保留所有 ViewModel 的建構子。
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}


# 4. 處理第三方函式庫
# -------------------------------------------------------------------
# 現代函式庫 (如 Retrofit, OkHttp, Coil) 通常自帶 ProGuard 規則，
# 我們主要加上 -dontwarn 來避免因函式庫內部問題導致編譯失敗。

# a. Retrofit & OkHttp (網路請求)
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# b. Coil (圖片載入)
-dontwarn coil.**


# 5. 保留 Kotlin 協程 (Coroutines) 的內部機制
# -------------------------------------------------------------------
-keep class kotlinx.coroutines.debug.internal.DebugProbesKt {
    <fields>;
    <methods>;
}
-dontwarn kotlinx.coroutines.**


# 6. 保留實現 Parcelable 介面的類別 (Android 標準)
# -------------------------------------------------------------------
# 雖然我們目前沒用到，但這是 Android 開發的標準規則，建議保留。
# 它能確保在 Activity 之間傳遞自訂物件時不會出錯。
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
# ===================================================================
#                           規則結束
# ===================================================================