package com.android.pixivviewer

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import com.android.pixivviewer.ui.DetailScreen
import com.android.pixivviewer.ui.theme.PixivViewerTheme
import com.android.pixivviewer.viewmodel.DetailViewModel

class DetailActivity : ComponentActivity() {

    private val viewModel by viewModels<DetailViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        // ✨ 啟用 Edge-to-Edge
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val illustId = getIllustIdFromIntent(intent)
        if (illustId != 0L) {
            viewModel.loadIllust(this, illustId)
        } else {
            finish()
            return
        }

        setContent {
            // ✨ 設置系統欄為透明
            val isDarkTheme = isSystemInDarkTheme()
            LaunchedEffect(isDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT
                    ) { isDarkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT
                    ) { isDarkTheme }
                )
            }

            PixivViewerTheme {
                DetailScreen(viewModel = viewModel)
            }
        }
    }

    companion object {
        private const val EXTRA_ILLUST_ID = "extra_illust_id"

        fun newIntent(context: Context, illustId: Long): Intent {
            return Intent(context, DetailActivity::class.java).apply {
                putExtra(EXTRA_ILLUST_ID, illustId)
            }
        }

        fun getIllustIdFromIntent(intent: Intent?): Long {
            if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
                return intent.data?.lastPathSegment?.toLongOrNull() ?: 0L
            }
            return intent?.getLongExtra(EXTRA_ILLUST_ID, 0L) ?: 0L
        }
    }
}