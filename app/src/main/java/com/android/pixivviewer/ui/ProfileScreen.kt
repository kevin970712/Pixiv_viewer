package com.android.pixivviewer.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.pixivviewer.FollowingActivity
import com.android.pixivviewer.SettingsActivity
import com.android.pixivviewer.network.UserDetailResponse
import com.android.pixivviewer.ui.components.IllustStaggeredGrid
import com.android.pixivviewer.ui.components.StatItem
import com.android.pixivviewer.utils.ImageLoaderFactory
import com.android.pixivviewer.utils.TokenManager
import com.android.pixivviewer.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val userDetail by viewModel.userDetail.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val onRefresh = remember(viewModel, context) {
        { viewModel.refreshProfile(context) }
    }
    val onLoadMore = remember(viewModel, context) {
        { viewModel.loadMoreBookmarks(context) }
    }
    val onSettingsClick = remember(context) {
        { context.startActivity(Intent(context, SettingsActivity::class.java)) }
    }

    LaunchedEffect(Unit) {
        viewModel.loadProfile(context)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onSettingsClick, // ✨ 使用穩定的 Lambda
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ✨ 关键修正：移除 modifier 上的 horizontal padding
            // ✨ 将 contentPadding 设为标准值，并加上顶部状态栏的 padding
            IllustStaggeredGrid(
                illusts = bookmarks,
                onLoadMore = onLoadMore,
                contentPadding = PaddingValues(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                    start = 12.dp,
                    end = 12.dp,
                    bottom = 80.dp // 为 FAB 预留足够空间
                ),
                headerContent = {
                    Column {
                        if (userDetail != null) {
                            UserProfileHeader(userDetail!!)
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                if (isRefreshing) CircularProgressIndicator()
                            }
                        }
                        Text(
                            text = "收藏的作品 (${userDetail?.profile?.totalBookmarks ?: 0})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 1.dp, start = 4.dp, end = 8.dp) // Header 内部的 padding
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun UserProfileHeader(detail: UserDetailResponse) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 頭像
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(detail.user.profileImageUrls.medium)
                .crossfade(true)
                .build(),
            imageLoader = ImageLoaderFactory.getPixivImageLoader(context),
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 名字
        Text(
            text = detail.user.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // 帳號 ID
        Text(
            text = "@${detail.user.account}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 數據統計 (關注/收藏數)
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ✨ 讓「關注中」可以點擊
            StatItem(
                count = detail.profile.totalFollowUsers,
                label = "關注中",
                modifier = Modifier.clickable {
                    val myId = TokenManager.getUserId(context)?.toLongOrNull() ?: 0L
                    val intent = FollowingActivity.newIntent(context, myId)
                    context.startActivity(intent)
                }
            )
            StatItem(count = detail.profile.totalBookmarks, label = "公開收藏")
        }
    }
}