package com.android.pixivviewer.utils

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.request.CachePolicy
import okhttp3.OkHttpClient

// ✨ 修正：物件名稱改回 ImageLoaderFactory
object ImageLoaderFactory {
    fun getPixivImageLoader(context: Context): ImageLoader {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Referer", "https://app-api.pixiv.net/")
                    .build()
                chain.proceed(request)
            }
            .build()

        // 建立一個有大小限制的磁碟快取
        val diskCache = DiskCache.Builder()
            .directory(context.cacheDir.resolve("image_cache"))
            .maxSizeBytes(100L * 1024 * 1024) // 50 MB
            .build()

        return ImageLoader.Builder(context)
            .okHttpClient(client)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCache(diskCache) // 使用我們自訂的 diskCache
            .crossfade(true)
            .build()
    }
}