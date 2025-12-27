package com.android.pixivviewer.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pixivviewer.network.Illust
import com.android.pixivviewer.network.NetworkModule
import com.android.pixivviewer.network.UserFollowingResponse
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.http.GET
import retrofit2.http.Url

interface FollowingApi {
    @GET
    suspend fun getNextUserFollowingPage(@Url url: String): UserFollowingResponse
}

class UserBookmarksViewModel : ViewModel() {

    private val _bookmarks = MutableStateFlow<ImmutableList<Illust>>(persistentListOf())
    val bookmarks = _bookmarks.asStateFlow()

    private var nextUrl: String? = null
    private var isLoadingMore = false

    fun loadUserBookmarks(context: Context, userId: Long) {
        if (_bookmarks.value.isNotEmpty()) return

        viewModelScope.launch {
            try {
                val api = NetworkModule.provideApiClient(context)
                // getUserBookmarks 回传的是 IllustRecommendedResponse，它的列表是 illusts
                val response = api.getUserBookmarks(userId)
                _bookmarks.value = response.illusts.toImmutableList()
                nextUrl = response.nextUrl
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadMoreUserBookmarks(context: Context) {
        if (nextUrl == null || isLoadingMore) return
        isLoadingMore = true

        viewModelScope.launch {
            try {
                // ✨ 关键修正：虽然复用了 FollowingApi，但 getUserBookmarks 的下一页
                // 回传的仍然是 IllustRecommendedResponse 结构
                val api = NetworkModule.provideApiClient(context)
                val response = api.getNextPage(nextUrl!!) // 使用通用的 getNextPage

                // ✨ response.illusts 是正确的
                _bookmarks.value = (_bookmarks.value + response.illusts).toImmutableList()
                nextUrl = response.nextUrl
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingMore = false
            }
        }
    }
}