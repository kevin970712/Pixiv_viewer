package com.android.pixivviewer

import android.content.Context // ✨ Import
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels // ✨ Import
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import com.android.pixivviewer.ui.SearchScreen
import com.android.pixivviewer.ui.theme.PixivViewerTheme
import com.android.pixivviewer.viewmodel.SearchViewModel

class SearchActivity : ComponentActivity() {
    // ✨ 取得 ViewModel 实例
    private val viewModel by viewModels<SearchViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // ✨ 在 setContent 之前，将 Intent 中的搜索词传递给 ViewModel
        intent.getStringExtra(EXTRA_SEARCH_QUERY)?.let {
            viewModel.initialQuery = it
        }

        setContent {
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
                // ✨ 将 ViewModel 实例传入 Screen
                SearchScreen(
                    viewModel = viewModel,
                    onBackClick = { finish() }
                )
            }
        }
    }

    // ✨ 新增 companion object 以便外部调用
    companion object {
        private const val EXTRA_SEARCH_QUERY = "search_query"
        fun newIntent(context: Context, query: String? = null): Intent {
            return Intent(context, SearchActivity::class.java).apply {
                if (query != null) {
                    putExtra(EXTRA_SEARCH_QUERY, query)
                }
            }
        }
    }
}