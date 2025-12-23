package com.android.pixivviewer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.pixivviewer.ui.components.FollowingUserCard
import com.android.pixivviewer.viewmodel.FollowingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowingScreen(
    viewModel: FollowingViewModel = viewModel(),
    userId: Long,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val followingUsers by viewModel.followingUsers.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadFollowing(context, userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("關注中") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.navigationBars
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(
                items = followingUsers,
                key = { _, item -> item.user.id }
            ) { index, userPreview ->
                // ✨ 觸發無限捲動
                if (index >= followingUsers.size - 3) {
                    viewModel.loadMoreFollowing(context)
                }

                // ✨ 直接複用 NewWorksScreen 的卡片元件
                FollowingUserCard(
                    preview = userPreview,
                    onFollowClick = { user ->
                        viewModel.toggleFollow(context, user)
                    }
                )
            }
        }
    }
}