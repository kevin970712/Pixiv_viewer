package com.android.pixivviewer.utils

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeUtil {
    // 預先定義好輸入和輸出的格式
    private val inputFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    private val outputFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun formatPixivDate(dateString: String?): String {
        if (dateString.isNullOrBlank()) {
            return ""
        }
        return try {
            // 解析 ISO 8601 字串
            val offsetDateTime = OffsetDateTime.parse(dateString, inputFormatter)
            // 格式化成目標樣式
            offsetDateTime.format(outputFormatter)
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果解析失敗，回傳原始字串或空字串
            dateString
        }
    }
}