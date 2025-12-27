package com.android.pixivviewer.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pixivviewer.network.Illust
import com.android.pixivviewer.network.NetworkModule
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object SearchHistoryManager {
    private const val PREFS_NAME = "search_history_prefs"
    private const val KEY_HISTORY = "history"

    fun getHistory(context: Context): ImmutableList<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // ✨ 优化：按添加顺序显示，最新的在最上面
        return prefs.getStringSet(KEY_HISTORY, emptySet())?.toList()?.reversed()?.toImmutableList()
            ?: persistentListOf()
    }

    fun addHistory(context: Context, query: String) {
        if (query.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentHistory = getHistory(context).toMutableSet()
        currentHistory.remove(query)
        val newHistory = (listOf(query) + currentHistory.toList()).take(10) // 最多保存15条
        prefs.edit().putStringSet(KEY_HISTORY, newHistory.toSet()).apply()
    }

    fun removeHistory(context: Context, query: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentHistory = getHistory(context).toMutableSet()
        currentHistory.remove(query)
        prefs.edit().putStringSet(KEY_HISTORY, currentHistory).apply()
    }
}


class SearchViewModel : ViewModel() {
    private val _searchResults = MutableStateFlow<ImmutableList<Illust>>(persistentListOf())
    val searchResults = _searchResults.asStateFlow()

    private val _searchHistory = MutableStateFlow<ImmutableList<String>>(persistentListOf())

    // ✨ 关键修正：将 _search_history 改为 _searchHistory
    val searchHistory = _searchHistory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    private var nextUrl: String? = null
    private var isLoadingMore = false
    var initialQuery: String? = null

    fun loadSearchHistoryAndInitialQuery(context: Context) {
        _searchHistory.value = SearchHistoryManager.getHistory(context)

        // 如果有初始搜索词，就执行它
        initialQuery?.let { query ->
            if (query.isNotBlank()) {
                search(context, query)
                // 清除，防止屏幕旋转后重复搜索
                initialQuery = null
            }
        }
    }

    fun loadSearchHistory(context: Context) {
        _searchHistory.value = SearchHistoryManager.getHistory(context)
    }

    fun removeSearchHistory(context: Context, query: String) {
        SearchHistoryManager.removeHistory(context, query)
        loadSearchHistory(context)
    }

    fun search(context: Context, query: String) {
        if (query.isBlank()) return

        SearchHistoryManager.addHistory(context, query)
        loadSearchHistory(context)

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _searchResults.value = persistentListOf()
                val api = NetworkModule.provideApiClient(context)
                val response = api.searchIllusts(word = query)
                _searchResults.value = response.illusts.toImmutableList()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMoreSearchResults(context: Context) {
        if (nextUrl == null || isLoadingMore) return
        isLoadingMore = true

        viewModelScope.launch {
            try {
                val api = NetworkModule.provideApiClient(context)
                val response = api.getNextPage(nextUrl!!)
                _searchResults.value = (_searchResults.value + response.illusts).toImmutableList()
                nextUrl = response.nextUrl
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingMore = false
            }
        }
    }

    fun clearSearch() {
        _searchResults.value = persistentListOf()
    }

    fun toggleBookmark(context: Context, illustId: Long) {
        viewModelScope.launch {
            val targetIllust = _searchResults.value.find { it.id == illustId } ?: return@launch
            try {
                val api = NetworkModule.provideApiClient(context)
                if (targetIllust.isBookmarked) {
                    api.deleteBookmark(illustId)
                } else {
                    api.addBookmark(illustId)
                }
                // 更新 UI
                val updatedList = _searchResults.value.map {
                    if (it.id == illustId) it.copy(isBookmarked = !it.isBookmarked) else it
                }.toImmutableList()
                _searchResults.value = updatedList
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}