package com.android.pixivviewer.utils

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.request.CachePolicy
import okhttp3.OkHttpClient

object ImageLoaderFactory {

    // ✨ 1. 宣告一個私有的、可為 null 的變數來儲存 ImageLoader 實例
    @Volatile
    private var instance: ImageLoader? = null

    // ✨ 2. 修改 get 方法，加入單例邏輯
    fun getPixivImageLoader(context: Context): ImageLoader {
        // 如果 instance 已經存在，就直接回傳它
        // 否則，進入同步區塊創建
        return instance ?: synchronized(this) {
            instance ?: buildImageLoader(context).also { instance = it }
        }
    }

    // ✨ 3. 將建立邏輯抽離到一個私有函式中，保持程式碼整潔
    private fun buildImageLoader(context: Context): ImageLoader {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Referer", "https://app-api.pixiv.net/")
                    .build()
                chain.proceed(request)
            }
            .build()

        val diskCache = DiskCache.Builder()
            .directory(context.cacheDir.resolve("image_cache"))
            .maxSizeBytes(100L * 1024 * 1024) // 100 MB
            .build()

        return ImageLoader.Builder(context)
            .okHttpClient(client)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCache(diskCache)
            .crossfade(true)
            .build()
    }
}