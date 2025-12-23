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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.android.pixivviewer.ui.UserScreen
import com.android.pixivviewer.ui.theme.PixivViewerTheme
import com.android.pixivviewer.viewmodel.UserViewModel

class UserActivity : ComponentActivity() {
    private val viewModel by viewModels<UserViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        // ✨ 啟用 Edge-to-Edge
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val userId = getUserIdFromIntent(intent)
        if (userId != 0L) {
            viewModel.loadUser(this, userId)
        } else {
            finish()
            return
        }

        setContent {
            // ✨ 設置系統欄透明
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
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UserScreen(viewModel = viewModel, onBackClick = { finish() })
                }
            }
        }
    }

    companion object {
        private const val EXTRA_USER_ID = "user_id"
        fun newIntent(context: Context, userId: Long): Intent {
            return Intent(context, UserActivity::class.java).apply {
                putExtra(EXTRA_USER_ID, userId)
            }
        }

        fun getUserIdFromIntent(intent: Intent?): Long {
            if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
                return intent.data?.lastPathSegment?.toLongOrNull() ?: 0L
            }
            return intent?.getLongExtra(EXTRA_USER_ID, 0L) ?: 0L
        }
    }
}