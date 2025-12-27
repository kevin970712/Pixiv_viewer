package com.android.pixivviewer.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

class ImageViewerViewModel : ViewModel() {
    // 我们可以用一个简单的 List 来接收传入的图片 URL
    var imageUrls: ImmutableList<String> = persistentListOf()
    var initialPage: Int = 0
}