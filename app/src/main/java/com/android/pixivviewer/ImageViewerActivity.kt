package com.android.pixivviewer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.android.pixivviewer.ui.ImageViewerScreen
import com.android.pixivviewer.ui.theme.PixivViewerTheme
import com.android.pixivviewer.viewmodel.ImageViewerViewModel
import kotlinx.collections.immutable.toImmutableList

class ImageViewerActivity : ComponentActivity() {

    private val viewModel by viewModels<ImageViewerViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✨ 关键 1：完全沉浸式，隐藏系统栏
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // 从 Intent 接收数据并传递给 ViewModel
        viewModel.imageUrls =
            intent.getStringArrayListExtra(EXTRA_IMAGE_URLS)?.toImmutableList() ?: return
        viewModel.initialPage = intent.getIntExtra(EXTRA_INITIAL_PAGE, 0)

        setContent {
            PixivViewerTheme {
                ImageViewerScreen(viewModel = viewModel)
            }
        }
    }

    companion object {
        private const val EXTRA_IMAGE_URLS = "image_urls"
        private const val EXTRA_INITIAL_PAGE = "initial_page"

        fun newIntent(context: Context, imageUrls: List<String>, initialPage: Int): Intent {
            return Intent(context, ImageViewerActivity::class.java).apply {
                putStringArrayListExtra(EXTRA_IMAGE_URLS, ArrayList(imageUrls))
                putExtra(EXTRA_INITIAL_PAGE, initialPage)
            }
        }
    }
}