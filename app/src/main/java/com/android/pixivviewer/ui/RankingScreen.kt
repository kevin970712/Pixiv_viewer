package com.android.pixivviewer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.pixivviewer.viewmodel.HomeViewModel

@Composable
fun RankingScreen(
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val illusts by viewModel.rankingIllusts.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    rememberCoroutineScope()

    val rankingModes = remember {
        listOf(
            "day" to "每日",
            "week" to "每週",
            "month" to "每月",
            "day_male" to "男性向",
            "day_female" to "女性向",
            "week_rookie" to "新人",
            "week_original" to "原創",
            "day_r18" to "R-18"
        )
    }

    // --- 稳定的 Lambdas ---
    val onTabClick = remember<(Int) -> Unit> {
        { index -> selectedTabIndex = index }
    }

    val onLoadMore = remember(viewModel, context) {
        { viewModel.loadMoreRanking(context) }
    }

    val onBookmarkClick = remember(viewModel, context) {
        { illustId: Long -> viewModel.toggleBookmark(context, illustId, isRanking = true) }
    }

    LaunchedEffect(selectedTabIndex) {
        val selectedMode = rankingModes[selectedTabIndex].first
        viewModel.refreshRanking(context, selectedMode)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 0.dp
        ) {
            rankingModes.forEachIndexed { index, (_, label) ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { onTabClick(index) },
                    text = { Text(label) }
                )
            }
        }

        if (illusts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            IllustStaggeredGrid(
                illusts = illusts,
                onLoadMore = onLoadMore,
                onBookmarkClick = onBookmarkClick
            )
        }
    }
}