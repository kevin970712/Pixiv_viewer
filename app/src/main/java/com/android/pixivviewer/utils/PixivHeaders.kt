package com.android.pixivviewer.utils

import android.os.Build
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object PixivHeaders {
    // Salt 是固定字串，可以用 const
    private const val HASH_SALT = "28c1fdd170a5204386cb1313c7077b34f83e46ca4d4aba64508ec565638c3e30"

    // ⚠️ 修正點：這裡去掉了 const，因為 Build.MODEL 是執行時變數
    val USER_AGENT = "PixivAndroidApp/6.158.0 (Android ${Build.VERSION.RELEASE}; ${Build.MODEL})"

    // 這些是固定字串，可以用 const
    const val APP_VERSION = "6.158.0"
    const val APP_OS = "android"

    // 取得當前的 ISO 8601 時間字串
    private fun getIso8601Time(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val date = Date()
        val formatted = sdf.format(date)
        return "${formatted}+00:00"
    }

    // MD5 加密
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    // 生成需要的 Headers Map
    fun getHeaders(): Map<String, String> {
        val time = getIso8601Time()
        val hash = md5(time + HASH_SALT)

        return mapOf(
            "User-Agent" to USER_AGENT,
            "App-OS" to APP_OS,
            "App-OS-Version" to Build.VERSION.RELEASE,
            "App-Version" to APP_VERSION,
            "App-Accept-Language" to "zh_tw",
            "X-Client-Time" to time,
            "X-Client-Hash" to hash
        )
    }
}