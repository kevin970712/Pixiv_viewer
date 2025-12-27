package com.android.pixivviewer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.pixivviewer.viewmodel.NewWorksPage
import com.android.pixivviewer.viewmodel.NewWorksViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewWorksScreen(viewModel: NewWorksViewModel = viewModel()) {
    val context = LocalContext.current

    // ✨ 1. PagerState 不再需要，改回 TabIndex
    var selectedTabIndex by remember { mutableStateOf(0) }
    val pages = remember { NewWorksPage.entries }

    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val onRefresh = {
        when (pages[selectedTabIndex]) {
            NewWorksPage.Activity -> viewModel.refreshActivity(context)
            NewWorksPage.Following -> viewModel.refreshFollowing(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        SecondaryTabRow(
            selectedTabIndex = selectedTabIndex
        ) {
            pages.forEachIndexed { index, page ->
                Tab(
                    selected = selectedTabIndex == index,
                    // ✨ 2. onClick 直接更新 TabIndex
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            when (page) {
                                NewWorksPage.Activity -> "動態"
                                NewWorksPage.Following -> "關注"
                            }
                        )
                    }
                )
            }
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh
        ) {
            // ✨ 3. 移除 HorizontalPager，直接使用 Box + when 判斷
            Box(modifier = Modifier.fillMaxSize()) {
                when (pages[selectedTabIndex]) {
                    NewWorksPage.Activity -> {
                        val illusts by viewModel.activityIllusts.collectAsState()
                        // ✨ 當 Tab 第一次被選中時，觸發載入
                        LaunchedEffect(Unit) { viewModel.fetchActivity(context) }

                        if (illusts.isEmpty() && !isRefreshing) {
                            LoadingIndicator()
                        } else {
                            IllustStaggeredGrid(
                                illusts = illusts,
                                onLoadMore = { viewModel.loadMoreActivity(context) },
                                onBookmarkClick = { illustId ->
                                    viewModel.toggleBookmark(
                                        context,
                                        illustId
                                    )
                                }
                            )
                        }

                    }

                    NewWorksPage.Following -> {
                        val users by viewModel.followingUsers.collectAsState()
                        LaunchedEffect(Unit) { viewModel.fetchFollowing(context) }

                        if (users.isEmpty() && !isRefreshing) {
                            LoadingIndicator()
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(users, key = { it.user.id }) { userPreview ->
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
                }
            }
        }
    }
}