package com.android.pixivviewer.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pixivviewer.network.Illust
import com.android.pixivviewer.network.NetworkModule
import com.android.pixivviewer.network.UserDetailResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

class UserViewModel : ViewModel() {

    private val _userDetail = MutableStateFlow<UserDetailResponse?>(null)
    val userDetail = _userDetail.asStateFlow()

    private val _userIllusts = MutableStateFlow<ImmutableList<Illust>>(persistentListOf())
    val userIllusts = _userIllusts.asStateFlow()

    private var nextUrl: String? = null
    private var isLoadingMore = false

    // 載入作者資料
    fun loadUser(context: Context, userId: Long) {
        viewModelScope.launch {
            try {
                val api = NetworkModule.provideApiClient(context)

                // 平行載入：詳情 + 作品集
                launch {
                    try {
                        _userDetail.value = api.getUserDetail(userId)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                launch {
                    try {
                        val response = api.getUserIllusts(userId)
                        _userIllusts.value = response.illusts.toImmutableList()
                        nextUrl = response.nextUrl // ✨ 記錄下一頁
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadMoreIllusts(context: Context) {
        if (nextUrl == null || isLoadingMore) return

        isLoadingMore = true
        viewModelScope.launch {
            try {
                val api = NetworkModule.provideApiClient(context)
                val response = api.getNextPage(nextUrl!!)

                _userIllusts.value = (_userIllusts.value + response.illusts).toImmutableList()
                nextUrl = response.nextUrl

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingMore = false
            }
        }
    }

    // 關注/取消關注
    fun toggleFollow(context: Context) {
        val currentDetail = _userDetail.value ?: return
        val isFollowing = currentDetail.user.isFollowed // 注意：需要去 User 結構補上這個欄位

        viewModelScope.launch {
            try {
                val api = NetworkModule.provideApiClient(context)
                if (isFollowing) {
                    api.unfollowUser(currentDetail.user.id)
                } else {
                    api.followUser(currentDetail.user.id)
                }

                // 樂觀更新 UI
                // 我們需要複製並修改深層的 User 物件，這稍微繁瑣一點
                val updatedUser = currentDetail.user.copy(isFollowed = !isFollowing)
                _userDetail.value = currentDetail.copy(user = updatedUser)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}