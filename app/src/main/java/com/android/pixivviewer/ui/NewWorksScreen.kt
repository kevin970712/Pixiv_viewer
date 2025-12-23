package com.android.pixivviewer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.pixivviewer.network.User
import com.android.pixivviewer.ui.components.FollowingUserCard
import com.android.pixivviewer.ui.components.IllustStaggeredGrid
import com.android.pixivviewer.ui.components.LoadingIndicator
import com.android.pixivviewer.viewmodel.NewWorksPage
import com.android.pixivviewer.viewmodel.NewWorksViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewWorksScreen(viewModel: NewWorksViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState { NewWorksPage.entries.size }
    val pages = remember { NewWorksPage.entries }

    val isRefreshing by viewModel.isRefreshing.collectAsState()

    // ✨ 關鍵修正 1：創建穩定的 Lambda
    val onRefresh = remember<() -> Unit>(viewModel, context, pages, pagerState) {
        {
            when (pages[pagerState.currentPage]) {
                NewWorksPage.Activity -> viewModel.refreshActivity(context)
                NewWorksPage.Following -> viewModel.refreshFollowing(context)
            }
        }
    }
    val onTabClick = remember<(Int) -> Unit>(scope, pagerState) {
        { index ->
            scope.launch { pagerState.animateScrollToPage(index) }
        }
    }
    val onFollowClick = remember<(User) -> Unit>(viewModel, context) {
        { user -> viewModel.toggleFollow(context, user) }
    }
    val onLoadMoreActivity = remember<() -> Unit>(viewModel, context) {
        { viewModel.loadMoreActivity(context) }
    }
    val onLoadMoreFollowing = remember<() -> Unit>(viewModel, context) {
        { viewModel.loadMoreFollowing(context) }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage
        ) {
            pages.forEachIndexed { index, page ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { onTabClick(index) }, // ✨ 使用穩定的 Lambda
                    text = {
                        Text(when(page) {
                            NewWorksPage.Activity -> "動態"
                            NewWorksPage.Following -> "關注"
                        })
                    }
                )
            }
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh // ✨ 使用穩定的 Lambda
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                when (pages[pageIndex]) {
                    NewWorksPage.Activity -> {
                        val illusts by viewModel.activityIllusts.collectAsState()
                        LaunchedEffect(Unit) { viewModel.fetchActivity(context) }

                        IllustStaggeredGrid(
                            illusts = illusts,
                            onLoadMore = onLoadMoreActivity // ✨ 使用穩定的 Lambda
                        )
                    }
                    NewWorksPage.Following -> {
                        val users by viewModel.followingUsers.collectAsState()
                        LaunchedEffect(Unit) { viewModel.fetchFollowing(context) }

                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(users, key = { it.user.id }) { userPreview ->
                                FollowingUserCard(
                                    preview = userPreview,
                                    onFollowClick = onFollowClick // ✨ 使用穩定的 Lambda
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}