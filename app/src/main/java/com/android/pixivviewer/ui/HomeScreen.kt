package com.android.pixivviewer.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.android.pixivviewer.SearchActivity
import com.android.pixivviewer.ui.components.IllustStaggeredGrid // ✨ 確保 Import 正確
import com.android.pixivviewer.viewmodel.HomeViewModel
import kotlinx.collections.immutable.ImmutableList // ✨ 確保 Import
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val illusts by viewModel.recommendIllusts.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val scope = rememberCoroutineScope()
    val gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState()
    val showFab by remember { derivedStateOf { gridState.firstVisibleItemIndex > 5 } }

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
                        onRefresh() // ✨ 使用穩定的 Lambda
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(onClick = {
                    scope.launch { gridState.animateScrollToItem(0) }
                }) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "回到頂部")
                }
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
                    onLoadMore = onLoadMore // ✨ 使用穩定的 Lambda
                )
            }
        }
    }
}