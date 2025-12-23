package com.android.pixivviewer.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pixivviewer.network.Illust
import com.android.pixivviewer.network.NetworkModule
import com.android.pixivviewer.network.User
import com.android.pixivviewer.network.UserPreview
import com.android.pixivviewer.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

enum class NewWorksPage {
    Activity,
    Following
}

class NewWorksViewModel : ViewModel() {

    // 分頁 1：動態 (畫作列表)
    private val _activityIllusts = MutableStateFlow<ImmutableList<Illust>>(persistentListOf())
    val activityIllusts = _activityIllusts.asStateFlow()

    // 分頁 2：關注 (用戶列表)
    private val _followingUsers = MutableStateFlow<ImmutableList<UserPreview>>(persistentListOf())
    val followingUsers = _followingUsers.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    // ✨ 新增：分頁狀態
    private var activityNextUrl: String? = null
    private var followingNextUrl: String? = null
    private var isLoadingMore = false

    fun toggleFollow(context: Context, userToToggle: User) {
        viewModelScope.launch {
            try {
                val api = NetworkModule.provideApiClient(context)
                if (userToToggle.isFollowed) {
                    api.unfollowUser(userToToggle.id)
                } else {
                    api.followUser(userToToggle.id)
                }

                // 乐观点更新 UI
                val updatedList = _followingUsers.value.map { preview ->
                    if (preview.user.id == userToToggle.id) {
                        preview.copy(user = preview.user.copy(isFollowed = !userToToggle.isFollowed))
                    } else {
                        preview
                    }
                }.toImmutableList()
                _followingUsers.value = updatedList

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- 動態 (Activity) 相關 ---
    fun fetchActivity(context: Context) {
        if (_activityIllusts.value.isNotEmpty()) return
        refreshActivity(context)
    }

    fun refreshActivity(context: Context) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val api = NetworkModule.provideApiClient(context)
                val response = api.getFollowIllusts()
                _activityIllusts.value = response.illusts.toImmutableList()
                activityNextUrl = response.nextUrl // 記錄 nextUrl
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loadMoreActivity(context: Context) {
        if (activityNextUrl == null || isLoadingMore) return
        isLoadingMore = true
        viewModelScope.launch {
            try {
                val api = NetworkModule.provideApiClient(context)
                val response = api.getNextPage(activityNextUrl!!)
                _activityIllusts.value = (_activityIllusts.value + response.illusts).toImmutableList()
                activityNextUrl = response.nextUrl
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingMore = false
            }
        }
    }

    // --- 關注 (Following) 相關 ---
    fun fetchFollowing(context: Context) {
        if (_followingUsers.value.isNotEmpty()) return
        refreshFollowing(context)
    }

    fun refreshFollowing(context: Context) {
        val userId = TokenManager.getUserId(context)?.toLongOrNull() ?: return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val api = NetworkModule.provideApiClient(context)
                val response = api.getUserFollowing(userId)
                _followingUsers.value = response.userPreviews.toImmutableList()
                followingNextUrl = response.nextUrl // 記錄 nextUrl
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loadMoreFollowing(context: Context) {
        if (followingNextUrl == null || isLoadingMore) return
        isLoadingMore = true
        viewModelScope.launch {
            try {
                val api = NetworkModule.provideApiClient(context) as FollowingApi
                val response = api.getNextUserFollowingPage(followingNextUrl!!)
                _followingUsers.value = (_followingUsers.value + response.userPreviews).toImmutableList()
                followingNextUrl = response.nextUrl
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingMore = false
            }
        }
    }
}