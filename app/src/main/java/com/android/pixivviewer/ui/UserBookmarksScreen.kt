package com.android.pixivviewer.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.pixivviewer.ui.components.IllustStaggeredGrid
import com.android.pixivviewer.viewmodel.UserBookmarksViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserBookmarksScreen(
    viewModel: UserBookmarksViewModel = viewModel(),
    userId: Long,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val bookmarks by viewModel.bookmarks.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadUserBookmarks(context, userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("公開收藏") },
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
            illusts = bookmarks,
            modifier = Modifier.padding(innerPadding),
            onLoadMore = { viewModel.loadMoreUserBookmarks(context) }
        )
    }
}