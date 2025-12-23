package com.android.pixivviewer

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.android.pixivviewer.ui.HomeScreen
import com.android.pixivviewer.ui.LoginScreen
import com.android.pixivviewer.ui.NewWorksScreen
import com.android.pixivviewer.ui.ProfileScreen
import com.android.pixivviewer.ui.RankingScreen
import com.android.pixivviewer.ui.theme.PixivViewerTheme
import com.android.pixivviewer.utils.TokenManager
import com.android.pixivviewer.viewmodel.HomeViewModel
import com.android.pixivviewer.viewmodel.LoginViewModel

class MainActivity : ComponentActivity() {
    private val loginViewModel by viewModels<LoginViewModel>()
    private val homeViewModel by viewModels<HomeViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // ✨ 關鍵：處理 App 冷啟動時帶來的 Intent
        handleLoginIntent(intent)

        setContent {
            // ✨ 第二步：在 Composable 中設置透明系統欄
            // 這會讓系統欄背景變成透明，以便看到後面的 App 內容
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
                MainContent(loginViewModel, homeViewModel)
            }
        }
    }

    /**
     * ✨ 關鍵：處理 App 已經在背景時，接收到的新 Intent
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLoginIntent(intent)
    }

    /**
     * ✨ 統一處理登入回調的 Intent
     */
    private fun handleLoginIntent(intent: Intent?) {
        val uri = intent?.data
        // 檢查是否是我們定義的 pixiv://account/login 回調
        if (uri != null && uri.scheme == "pixiv" && uri.host == "account" && uri.path == "/login") {
            loginViewModel.handleCallback(this, uri)
        }
    }
}


@Composable
fun MainContent(loginViewModel: LoginViewModel, homeViewModel: HomeViewModel) {
    val context = LocalContext.current

    var isLoggedIn by remember { mutableStateOf(TokenManager.isLoggedIn(context)) }
    val isLoginSuccess by loginViewModel.isLoginSuccess.collectAsState()

    LaunchedEffect(isLoginSuccess) {
        if (isLoginSuccess) {
            isLoggedIn = true
        }
    }

    if (isLoggedIn) {
        MainAppStructure(homeViewModel)
    } else {
        LoginScreen(loginViewModel)
    }
}

enum class MainTab(val label: String, val icon: ImageVector) {
    Home("首頁", Icons.Default.Home),
    Ranking("排行", Icons.Default.Star),
    NewWorks("新作", Icons.Default.AutoAwesome),
    Profile("我的", Icons.Default.Person)
}

@Composable
fun MainAppStructure(homeViewModel: HomeViewModel) {
    var currentTab by remember { mutableStateOf(MainTab.Home) }

    // ✨ 新增：觸發搜尋焦點的時間戳記

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            NavigationBar {
                MainTab.values().forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                        selected = currentTab == tab,
                        onClick = { currentTab = tab }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                // ✨ 關鍵修正 2：消費掉 insets，防止子元件重複處理
                .consumeWindowInsets(innerPadding)
                .fillMaxSize()
        ) {
            when (currentTab) {
                MainTab.Home -> HomeScreen(homeViewModel)
                MainTab.Ranking -> RankingScreen(homeViewModel)
                MainTab.NewWorks -> NewWorksScreen()
                MainTab.Profile -> ProfileScreen()
            }
        }
    }
}

@Composable
fun ProfilePlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("個人頁面 (待開發)")
    }
}