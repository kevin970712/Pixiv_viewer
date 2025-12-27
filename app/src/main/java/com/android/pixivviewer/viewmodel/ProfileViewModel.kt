package com.android.pixivviewer.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pixivviewer.network.Illust
import com.android.pixivviewer.network.NetworkModule
import com.android.pixivviewer.network.UserDetailResponse
import com.android.pixivviewer.utils.TokenManager
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val _userDetail = MutableStateFlow<UserDetailResponse?>(null)
    val userDetail = _userDetail.asStateFlow()

    private val _bookmarks = MutableStateFlow<ImmutableList<Illust>>(persistentListOf())
    val bookmarks = _bookmarks.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private var nextUrl: String? = null
    private var isLoadingMore = false

    fun refreshProfile(context: Context) {
        val userId = TokenManager.getUserId(context)?.toLongOrNull() ?: return

        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val api = NetworkModule.provideApiClient(context)

                // ✨ 关键修正：为每个独立的网路请求加上 try-catch
                val userDetailJob = launch {
                    try {
                        _userDetail.value = api.getUserDetail(userId)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // 请求失败时，可以考虑将 userDetail 设为 null 或保持原状
                    }
                }

                val bookmarksJob = launch {
                    try {
                        val response = api.getUserBookmarks(userId)
                        _bookmarks.value = response.illusts.toImmutableList()
                        nextUrl = response.nextUrl
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // 请求失败时，清空列表或保持原状
                        _bookmarks.value = persistentListOf()
                    }
                }

                // 等待两个请求都完成（无论成功或失败）
                joinAll(userDetailJob, bookmarksJob)

            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loadProfile(context: Context) {
        if (_bookmarks.value.isNotEmpty()) return
        refreshProfile(context)
    }

    fun loadMoreBookmarks(context: Context) {
        if (nextUrl == null || isLoadingMore) return // 沒有下一頁或正在載入中

        isLoadingMore = true
        viewModelScope.launch {
            try {
                val api = NetworkModule.provideApiClient(context)
                val response = api.getNextPage(nextUrl!!) // 呼叫萬用接口

                // ✨ 將新資料附加到舊資料後面
                _bookmarks.value = (_bookmarks.value + response.illusts).toImmutableList()
                nextUrl = response.nextUrl // 更新下一頁網址

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingMore = false
            }
        }
    }

    fun toggleBookmark(context: Context, illustId: Long) {
        viewModelScope.launch {
            // 找到要操作的作品
            val targetIllust = _bookmarks.value.find { it.id == illustId } ?: return@launch

            try {
                val api = NetworkModule.provideApiClient(context)
                if (targetIllust.isBookmarked) {
                    // 对于收藏列表，我们只处理“取消收藏”
                    api.deleteBookmark(illustId)
                } else {
                    // (可选) 如果想在这里也能“重新收藏”，可以加上 addBookmark
                    api.addBookmark(illustId)
                }

                // 乐观点更新 UI：从列表中移除或修改状态
                // 这里我们采用更简单的逻辑：直接从列表中移除该项目
                // 因为在“我的收藏”页面，取消收藏就意味着它应该消失
                val updatedList = _bookmarks.value.filterNot { it.id == illustId }.toImmutableList()
                _bookmarks.value = updatedList

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 登出功能
    fun logout(context: Context) {
        // 清除 Token
        TokenManager.saveTokens(context, "", "", "")
        // 這裡通常會由 UI 層監聽狀態來重啟 Activity，或是簡單處理直接殺掉 Process (不推薦但有效)
        // 為了簡單起見，我們這裡只清除，UI 層檢測到沒 Token 會自動跳轉登入頁 (如果有的話)
    }
}