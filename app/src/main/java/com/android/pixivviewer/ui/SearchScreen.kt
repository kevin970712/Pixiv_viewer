package com.android.pixivviewer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.android.pixivviewer.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val searchResults by viewModel.searchResults.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var query by remember { mutableStateOf(viewModel.initialQuery ?: "") }
    var showResults by remember { mutableStateOf(viewModel.initialQuery != null) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val shouldRequestFocusOnInit = remember { viewModel.initialQuery == null }

    val onSearch = remember<(String) -> Unit>(viewModel, context, focusManager) {
        { searchQuery: String -> // ✨ 指定类型
            if (searchQuery.isNotBlank()) {
                viewModel.search(context, searchQuery)
                showResults = true
                focusManager.clearFocus()
            }
        }
    }
    val onQueryChange = remember<(String) -> Unit> {
        { newQuery: String -> // ✨ 指定类型
            query = newQuery
            if (showResults) {
                showResults = false
                viewModel.clearSearch()
            }
        }
    }
    val onClearQuery = remember {
        {
            query = ""
            showResults = false
            viewModel.clearSearch()
        }
    }
    val onNavigateBack = remember(viewModel, showResults) {
        {
            if (showResults) {
                showResults = false
                viewModel.clearSearch()
                query = ""
            } else {
                onBackClick()
            }
        }
    }
    val onLoadMore = remember(viewModel, context) {
        { viewModel.loadMoreSearchResults(context) }
    }

    LaunchedEffect(Unit) {
        viewModel.loadSearchHistoryAndInitialQuery(context)
        if (shouldRequestFocusOnInit) {
            kotlinx.coroutines.delay(100)
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = { Text("輸入關鍵字") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch(query) })
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { // ✨ 使用穩定的 Lambda
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = onClearQuery) { // ✨ 使用穩定的 Lambda
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                windowInsets = WindowInsets.statusBars
            )
        },
        contentWindowInsets = WindowInsets.navigationBars
    ) { innerPadding ->

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            showResults -> {
                IllustStaggeredGrid(
                    illusts = searchResults,
                    modifier = Modifier.padding(innerPadding),
                    onLoadMore = onLoadMore,
                    onBookmarkClick = { illustId ->
                        viewModel.toggleBookmark(context, illustId)
                    }
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(top = 8.dp)
                ) {
                    item {
                        Text(
                            text = "歷史紀錄",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(searchHistory) { historyItem ->
                        HistoryItem(
                            text = historyItem,
                            onClick = {
                                query = historyItem
                                onSearch(historyItem) // ✨ 修正 2：复用 onSearch Lambda
                            },
                            onDelete = {
                                viewModel.removeSearchHistory(context, historyItem)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    text: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, modifier = Modifier.weight(1f))
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Close, contentDescription = "Delete")
        }
    }
}