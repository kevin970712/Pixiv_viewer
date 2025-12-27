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

class HomeViewModel : ViewModel() {

    // --- State ---
    private val _recommendIllusts = MutableStateFlow<ImmutableList<Illust>>(persistentListOf())
    val recommendIllusts = _recommendIllusts.asStateFlow()

    private val _rankingIllusts = MutableStateFlow<ImmutableList<Illust>>(persistentListOf())
    val rankingIllusts = _rankingIllusts.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private var recommendNextUrl: String? = null
    private var rankingNextUrl: String? = null
    private var isLoadingMore = false

    // --- Events for Recommended Page ---
    fun fetchRecommended(context: Context) {
        if (_recommendIllusts.value.isNotEmpty()) return
        refreshRecommended(context)
    }

    fun refreshRecommended(context: Context) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val api = NetworkModule.provideApiClient(context)
                val response = api.getRecommendedIllusts()
                _recommendIllusts.value = response.illusts.toImmutableList()
                recommendNextUrl = response.nextUrl
            } catch (e: Exception) {
                e.printStackTrace()
                _recommendIllusts.value = persistentListOf() // 發生錯誤時清空
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loadMoreRecommended(context: Context) {
        if (recommendNextUrl == null || isLoadingMore) return
        isLoadingMore = true
        viewModelScope.launch {
            try {
                val api = NetworkModule.provideApiClient(context)
                val response = api.getNextPage(recommendNextUrl!!)
                _recommendIllusts.value =
                    (_recommendIllusts.value + response.illusts).toImmutableList()
                recommendNextUrl = response.nextUrl
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingMore = false
            }
        }
    }

    // --- Events for Ranking Page ---
    fun fetchRanking(context: Context, mode: String = "day") {
        if (_rankingIllusts.value.isNotEmpty()) return
        refreshRanking(context, mode)
    }

    fun refreshRanking(context: Context, mode: String) {
        viewModelScope.launch {
            try {
                _rankingIllusts.value = persistentListOf() // 每次刷新都清空
                val api = NetworkModule.provideApiClient(context)
                val response = api.getIllustRanking(mode = mode)
                _rankingIllusts.value = response.illusts.toImmutableList()
                rankingNextUrl = response.nextUrl
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadMoreRanking(context: Context) {
        if (rankingNextUrl == null || isLoadingMore) return
        isLoadingMore = true
        viewModelScope.launch {
            try {
                val api = NetworkModule.provideApiClient(context)
                val response = api.getNextPage(rankingNextUrl!!)
                _rankingIllusts.value = (_rankingIllusts.value + response.illusts).toImmutableList()
                rankingNextUrl = response.nextUrl
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingMore = false
            }
        }
    }

    fun toggleBookmark(context: Context, illustId: Long, isRanking: Boolean) {
        viewModelScope.launch {
            val listToUpdate = if (isRanking) _rankingIllusts else _recommendIllusts
            val targetIllust = listToUpdate.value.find { it.id == illustId } ?: return@launch

            try {
                val api = NetworkModule.provideApiClient(context)
                if (targetIllust.isBookmarked) {
                    api.deleteBookmark(illustId)
                } else {
                    api.addBookmark(illustId)
                }

                // 更新 UI
                val updatedList = listToUpdate.value.map {
                    if (it.id == illustId) it.copy(isBookmarked = !it.isBookmarked) else it
                }.toImmutableList()
                listToUpdate.value = updatedList

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}