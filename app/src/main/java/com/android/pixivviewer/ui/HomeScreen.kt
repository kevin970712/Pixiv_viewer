package com.android.pixivviewer.ui

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.android.pixivviewer.SearchActivity
import com.android.pixivviewer.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val illusts by viewModel.recommendIllusts.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val scope = rememberCoroutineScope()
    val gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState()

    // ✨ 關鍵修正 1：使用 remember 包裹所有會傳遞給子元件的 Lambda
    val onRefresh = remember(viewModel, context) {
        { viewModel.refreshRecommended(context) }
    }
    val onLoadMore = remember(viewModel, context) {
        { viewModel.loadMoreRecommended(context) }
    }
    val onSearchClick = remember(context) {
        { context.startActivity(Intent(context, SearchActivity::class.java)) }
    }

    LaunchedEffect(Unit) {
        if (illusts.isEmpty()) viewModel.fetchRecommended(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pixiv Viewer") },
                actions = {
                    IconButton(onClick = onSearchClick) { // ✨ 使用穩定的 Lambda
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = {
                        scope.launch { gridState.scrollToItem(0) }
                        onRefresh()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        gridState.animateScrollToItem(0)
                    }
                }
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "回到頂部")
            }
        },
        contentWindowInsets = WindowInsets.navigationBars
    ) { innerPadding ->

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh, // ✨ 使用穩定的 Lambda
            modifier = Modifier.padding(innerPadding)
        ) {
            if (illusts.isEmpty() && !isRefreshing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                IllustStaggeredGrid(
                    illusts = illusts,
                    gridState = gridState,
                    onLoadMore = onLoadMore,
                    onBookmarkClick = { illustId ->
                        viewModel.toggleBookmark(context, illustId, isRanking = false)
                    }
                )
            }
        }
    }
}