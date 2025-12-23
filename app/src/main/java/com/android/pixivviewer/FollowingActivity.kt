package com.android.pixivviewer

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.android.pixivviewer.ui.FollowingScreen
import com.android.pixivviewer.ui.theme.PixivViewerTheme

class FollowingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val userId = intent.getLongExtra(EXTRA_USER_ID, 0L)
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
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FollowingScreen(
                        userId = userId,
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }

    companion object {
        private const val EXTRA_USER_ID = "user_id"
        fun newIntent(context: Context, userId: Long): Intent {
            return Intent(context, FollowingActivity::class.java).apply {
                putExtra(EXTRA_USER_ID, userId)
            }
        }
    }
}