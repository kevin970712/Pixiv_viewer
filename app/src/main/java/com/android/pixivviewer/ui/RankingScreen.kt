package com.android.pixivviewer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.pixivviewer.ui.components.IllustStaggeredGrid
import com.android.pixivviewer.viewmodel.HomeViewModel

@Composable
fun RankingScreen(
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val illusts by viewModel.rankingIllusts.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }

    val rankingModes = remember {
        listOf(
            "day" to "每日",
            "week" to "每週",
            "month" to "每月",
            "day_male" to "男性向",
            "day_female" to "女性向",
            "week_rookie" to "新人", // ✨ 额外增加一些，让滚动效果更明显
            "week_original" to "原创",
            "day_r18" to "R-18"
        )
    }

    val onTabClick = remember<(Int) -> Unit> {
        { index -> selectedTabIndex = index }
    }

    // ✨ 關鍵修正 2：創建穩定的 onLoadMore Lambda
    val onLoadMore = remember(viewModel, context) {
        { viewModel.loadMoreRanking(context) }
    }

    LaunchedEffect(selectedTabIndex) {
        val selectedMode = rankingModes[selectedTabIndex].first
        viewModel.refreshRanking(context, selectedMode)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // ✨ 為整個頁面加上狀態列的 padding，確保 TabRow 不會被遮擋
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // ✨ 关键修正：使用 ScrollableTabRow
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 0.dp, // 让左右两边留出一些间距
        ) {
            rankingModes.forEachIndexed { index, (_, label) ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { onTabClick(index) },
                    text = { Text(label) }
                )
            }
        }

        // 内容区域
        if (illusts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            IllustStaggeredGrid(
                illusts = illusts
            )
        }
    }
}