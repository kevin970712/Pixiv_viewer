package com.android.pixivviewer.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pixivviewer.network.Illust
import com.android.pixivviewer.network.NetworkModule
import com.android.pixivviewer.network.UserDetailResponse
import com.android.pixivviewer.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

class ProfileViewModel : ViewModel() {

    private val _userDetail = MutableStateFlow<UserDetailResponse?>(null)
    val userDetail = _userDetail.asStateFlow()

    private val _bookmarks = MutableStateFlow<ImmutableList<Illust>>(persistentListOf())
    val bookmarks = _bookmarks.asStateFlow()

    // ✨ 新增：刷新狀態
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private var nextUrl: String? = null
    private var isLoadingMore = false

    // ✨ 修改：loadProfile 改名為 refreshProfile，並管理 isRefreshing
    fun refreshProfile(context: Context) {
        val userIdString = TokenManager.getUserId(context) ?: return
        val userId = userIdString.toLongOrNull() ?: return

        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val api = NetworkModule.provideApiClient(context)

                // 使用 launch 保持平行請求
                val userDetailJob = launch { _userDetail.value = api.getUserDetail(userId) }
                val bookmarksJob = launch {
                    val response = api.getUserBookmarks(userId)
                    _bookmarks.value = response.illusts.toImmutableList()
                    nextUrl = response.nextUrl
                }

                // 等待兩個請求都完成
                userDetailJob.join()
                bookmarksJob.join()

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    // 第一次載入時呼叫 refresh
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

    // 登出功能
    fun logout(context: Context) {
        // 清除 Token
        TokenManager.saveTokens(context, "", "", "")
        // 這裡通常會由 UI 層監聽狀態來重啟 Activity，或是簡單處理直接殺掉 Process (不推薦但有效)
        // 為了簡單起見，我們這裡只清除，UI 層檢測到沒 Token 會自動跳轉登入頁 (如果有的話)
    }
}