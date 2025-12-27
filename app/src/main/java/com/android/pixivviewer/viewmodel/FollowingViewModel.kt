package com.android.pixivviewer.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pixivviewer.network.NetworkModule
import com.android.pixivviewer.network.User
import com.android.pixivviewer.network.UserPreview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ✨ 我們需要一個新的 API 接口來處理分頁


class FollowingViewModel : ViewModel() {

    private val _followingUsers = MutableStateFlow<ImmutableList<UserPreview>>(persistentListOf())
    val followingUsers = _followingUsers.asStateFlow()

    private var nextUrl: String? = null
    private var isLoadingMore = false

    fun loadFollowing(context: Context, userId: Long) {
        if (userId == 0L) return // 无效 ID

        viewModelScope.launch {
            try {
                val api = NetworkModule.provideApiClient(context)
                val response = api.getUserFollowing(userId)
                _followingUsers.value = response.userPreviews.toImmutableList()
                nextUrl = response.nextUrl
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleFollow(context: Context, userToToggle: User) {
        viewModelScope.launch {
            try {
                val api = NetworkModule.provideApiClient(context)
                if (userToToggle.isFollowed) {
                    api.unfollowUser(userToToggle.id)
                } else {
                    api.followUser(userToToggle.id)
                }

                // 乐观点更新 UI：在列表中找到对应的用户并更新其状态
                val updatedList = _followingUsers.value.map { preview ->
                    if (preview.user.id == userToToggle.id) {
                        preview.copy(user = preview.user.copy(isFollowed = !userToToggle.isFollowed))
                    } else {
                        preview
                    }
                }.toImmutableList() // ✨ 加上 .toImmutableList()
                _followingUsers.value = updatedList

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 載入更多
    fun loadMoreFollowing(context: Context) {
        if (nextUrl == null || isLoadingMore) return
        isLoadingMore = true

        viewModelScope.launch {
            try {
                // 這裡我們需要一個能處理 UserFollowingResponse 的 Retrofit 實例
                // 為了簡單起見，我們直接使用主 API Client
                val api = NetworkModule.provideApiClient(context) as FollowingApi
                val response = api.getNextUserFollowingPage(nextUrl!!)
                _followingUsers.value =
                    (_followingUsers.value + response.userPreviews).toImmutableList()
                nextUrl = response.nextUrl
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingMore = false
            }
        }
    }
}