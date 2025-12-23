package com.android.pixivviewer.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

object ImageSaver {

    suspend fun saveImage(context: Context, imageUrl: String, title: String) {
        withContext(Dispatchers.IO) {
            try {
                // 1. 先用 Coil 下載圖片變成 Bitmap
                val loader = ImageLoaderFactory.getPixivImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false) // 必須關閉硬體加速才能轉 Bitmap
                    .build()

                val result = loader.execute(request).drawable ?: return@withContext
                val bitmap = result.toBitmap()

                // 2. 儲存到相簿
                saveBitmapToGallery(context, bitmap, "pixiv_${System.currentTimeMillis()}", title)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "圖片已儲存", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "下載失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        filename: String,
        description: String
    ) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$filename.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DESCRIPTION, description)
            // 存到 Pictures/PixivViewer 資料夾
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/PixivViewer"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1) // 標記為寫入中
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            val outputStream: OutputStream? = resolver.openOutputStream(it)
            outputStream?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            }

            // 寫入完成，解除 Pending 狀態
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(it, contentValues, null, null)
            }
        }
    }
}