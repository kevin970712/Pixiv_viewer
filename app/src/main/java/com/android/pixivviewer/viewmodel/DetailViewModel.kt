package com.android.pixivviewer.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pixivviewer.network.Illust
import com.android.pixivviewer.network.NetworkModule
import com.android.pixivviewer.utils.ImageSaver
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 定義 UI 的狀態
sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Error(val message: String) : DetailUiState()

    // ✨ 修改 Success 狀態，多帶一個相關作品列表
    data class Success(
        val illust: Illust,
        val relatedIllusts: ImmutableList<Illust> = persistentListOf()
    ) : DetailUiState()
}

class DetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState = _uiState.asStateFlow()

    fun loadIllust(context: Context, id: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = DetailUiState.Loading
                val api = NetworkModule.provideApiClient(context)

                // 1. 抓詳情
                val detailResponse = api.getIllustDetail(id)

                // 2. 成功抓到詳情後，先顯示詳情 (讓使用者不用等相關作品)
                _uiState.value = DetailUiState.Success(illust = detailResponse.illust)

                // 3. 接著抓相關作品 (靜默更新)
                try {
                    val relatedResponse = api.getRelatedIllusts(id)
                    // 更新狀態，把相關作品補進去
                    _uiState.value = DetailUiState.Success(
                        illust = detailResponse.illust,
                        relatedIllusts = relatedResponse.illusts.toImmutableList()
                    )
                } catch (e: Exception) {
                    // 相關作品抓失敗不影響主詳情顯示，印 Log 即可
                    e.printStackTrace()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = DetailUiState.Error(e.message ?: "未知錯誤")
            }
        }
    }

    fun toggleBookmark(context: Context, illustId: Long) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState !is DetailUiState.Success) return@launch

            // 1. 找到要操作的目標作品
            val targetIllust = if (currentState.illust.id == illustId) {
                currentState.illust
            } else {
                currentState.relatedIllusts.find { it.id == illustId }
            } ?: return@launch // 如果找不到就返回

            // 2. 呼叫 API
            try {
                val api = NetworkModule.provideApiClient(context)
                if (targetIllust.isBookmarked) {
                    api.deleteBookmark(illustId)
                } else {
                    api.addBookmark(illustId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@launch // API 失敗則不更新 UI
            }

            // 3. 樂觀更新 UI 狀態
            val updatedIllust = targetIllust.copy(isBookmarked = !targetIllust.isBookmarked)

            // 判斷更新的是主作品還是相關列表中的作品
            val newUiState = if (currentState.illust.id == illustId) {
                currentState.copy(illust = updatedIllust)
            } else {
                // ✨ 关键修正：使用 map 和 toImmutableList 来更新
                val updatedRelatedList = currentState.relatedIllusts.map {
                    if (it.id == illustId) updatedIllust else it
                }.toImmutableList()
                currentState.copy(relatedIllusts = updatedRelatedList)
            }
            _uiState.value = newUiState
        }
    }

    fun toggleUserFollow(context: Context, userId: Long, isFollowed: Boolean) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState !is DetailUiState.Success) return@launch

            // 1. 呼叫 API
            try {
                val api = NetworkModule.provideApiClient(context)
                if (isFollowed) {
                    api.unfollowUser(userId)
                } else {
                    api.followUser(userId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@launch
            }

            // 2. 更新 UI (只更新主作品的作者狀態)
            val updatedUser = currentState.illust.user.copy(isFollowed = !isFollowed)
            val updatedIllust = currentState.illust.copy(user = updatedUser)
            _uiState.value = currentState.copy(illust = updatedIllust)
        }
    }

    fun downloadIllust(context: Context, illust: Illust) {
        viewModelScope.launch {
            val originalImageUrl = if (illust.pageCount == 1) {
                illust.metaSinglePage?.originalImageUrl
            } else {
                illust.metaPages.firstOrNull()?.imageUrls?.original
            }
            // 如果找不到原图，就退而求其次下载 large 或 medium
            val finalUrl = originalImageUrl
                ?: illust.imageUrls.large

            if (finalUrl != null) {
                ImageSaver.saveImage(context, finalUrl, illust.title)
            }
        }
    }

    fun toggleUserFollow(context: Context, illust: Illust) {
        viewModelScope.launch {
            try {
                val api = NetworkModule.provideApiClient(context)
                val user = illust.user

                if (user.isFollowed) {
                    api.unfollowUser(user.id)
                } else {
                    api.followUser(user.id)
                }

                // ✨ 樂觀更新 UI：更新 state 中的 User 資訊
                val currentState = _uiState.value
                if (currentState is DetailUiState.Success) {
                    // 複製並修改深層結構 (Illust -> User -> isFollowed)
                    val updatedUser = user.copy(isFollowed = !user.isFollowed)
                    val updatedIllust = currentState.illust.copy(user = updatedUser)

                    // 保持相關作品不變，只更新主圖資訊
                    _uiState.value = currentState.copy(illust = updatedIllust)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}