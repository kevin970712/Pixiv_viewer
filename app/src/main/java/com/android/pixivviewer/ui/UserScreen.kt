package com.android.pixivviewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.pixivviewer.FollowingActivity
import com.android.pixivviewer.UserBookmarksActivity
import com.android.pixivviewer.network.UserDetailResponse
import com.android.pixivviewer.ui.components.IllustStaggeredGrid
import com.android.pixivviewer.ui.components.StatItem
import com.android.pixivviewer.utils.ImageLoaderFactory
import com.android.pixivviewer.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(viewModel: UserViewModel, onBackClick: () -> Unit) {
    val context = LocalContext.current
    val userDetail by viewModel.userDetail.collectAsState()
    val illusts by viewModel.userIllusts.collectAsState()
    val onToggleFollow = remember(viewModel, context) {
        { viewModel.toggleFollow(context) }
    }
    val onLoadMore = remember(viewModel, context) {
        { viewModel.loadMoreIllusts(context) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(userDetail?.user?.name ?: "載入中...") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.navigationBars
    ) { innerPadding ->

        IllustStaggeredGrid(
            illusts = illusts,
            modifier = Modifier.padding(innerPadding),
            headerContent = {
                userDetail?.let { detail ->
                    UserProfileContent(
                        detail = detail,
                        onToggleFollow = onToggleFollow
                    )
                }
            },
            onLoadMore = onLoadMore
        )
    }
}

@Composable
fun UserProfileContent(
    detail: UserDetailResponse,
    onToggleFollow: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(detail.user.profileImageUrls.medium)
                .crossfade(true).build(),
            imageLoader = ImageLoaderFactory.getPixivImageLoader(context),
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = detail.user.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "@${detail.user.account}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onToggleFollow,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (detail.user.isFollowed) Color.Gray else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(if (detail.user.isFollowed) "已關注" else "關注")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            // 作品 (不可點擊)
            StatItem(
                count = detail.profile.totalIllusts,
                label = "作品"
            )

            // 關注中 (可點擊)
            StatItem(
                count = detail.profile.totalFollowUsers,
                label = "關注",
                modifier = Modifier.clickable {
                    val intent = FollowingActivity.newIntent(context, detail.user.id)
                    context.startActivity(intent)
                }
            )

            // 公開收藏 (可點擊)
            StatItem(
                count = detail.profile.totalBookmarks,
                label = "公開收藏",
                modifier = Modifier.clickable {
                    val intent = UserBookmarksActivity.newIntent(context, detail.user.id)
                    context.startActivity(intent)
                }
            )
        }
    }
}

