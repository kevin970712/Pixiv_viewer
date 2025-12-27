package com.android.pixivviewer.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.pixivviewer.DetailActivity
import com.android.pixivviewer.ImageViewerActivity
import com.android.pixivviewer.SearchActivity
import com.android.pixivviewer.UserActivity
import com.android.pixivviewer.network.Illust
import com.android.pixivviewer.network.Tag
import com.android.pixivviewer.utils.ImageLoaderFactory
import com.android.pixivviewer.utils.TimeUtil
import com.android.pixivviewer.viewmodel.DetailUiState
import com.android.pixivviewer.viewmodel.DetailViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

// 主入口
@Composable
fun DetailScreen(viewModel: DetailViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is DetailUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is DetailUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "載入失敗: ${state.message}")
            }
        }

        is DetailUiState.Success -> {
            // ✨ 關鍵修正：將所有 Lambda 事件使用 remember 包裹以提升穩定性

            val onBookmarkClick = remember<(Long) -> Unit>(viewModel, context) {
                { illustId -> viewModel.toggleBookmark(context, illustId) }
            }

            val onDownloadClick = remember<() -> Unit>(viewModel, context, state.illust) {
                { viewModel.downloadIllust(context, state.illust) }
            }

            val onToggleFollow = remember<() -> Unit>(viewModel, context, state.illust) {
                {
                    viewModel.toggleUserFollow(
                        context,
                        state.illust.user.id,
                        state.illust.user.isFollowed
                    )
                }
            }

            val onShareClick = remember<() -> Unit>(context, state.illust) {
                {
                    val shareUrl = "https://www.pixiv.net/artworks/${state.illust.id}"
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareUrl)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                }
            }

            val onIllustClick = remember<(Long) -> Unit>(context) {
                { id ->
                    val intent = DetailActivity.newIntent(context, id)
                    context.startActivity(intent)
                }
            }

            IllustDetailContent(
                illust = state.illust,
                relatedIllusts = state.relatedIllusts,
                onBookmarkClick = onBookmarkClick,
                onDownloadClick = onDownloadClick,
                onToggleFollow = onToggleFollow,
                onShareClick = onShareClick,
                onIllustClick = onIllustClick
            )
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalPermissionsApi::class
)
@Composable
fun IllustDetailContent(
    illust: Illust,
    relatedIllusts: List<Illust>,
    onBookmarkClick: (Long) -> Unit,
    onDownloadClick: () -> Unit,
    onShareClick: () -> Unit,
    onIllustClick: (Long) -> Unit,
    onToggleFollow: () -> Unit
) {
    val context = LocalContext.current

    remember(illust) {
        if (illust.pageCount == 1) {
            // 单图：优先 large，其次 medium
            listOf(
                illust.imageUrls.large
                    ?: illust.imageUrls.medium
            )
        } else {
            illust.metaPages.map { page ->
                page.imageUrls.large
                    ?: page.imageUrls.medium ?: ""
            }
        }
    }
    val writePermissionState = rememberPermissionState(
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    val downloadAction = {
        // 在 Android 10+ 或权限已授予时，直接下载
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || writePermissionState.status.isGranted) {
            onDownloadClick()
        } else {
            // 否则，请求权限
            writePermissionState.launchPermissionRequest()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            BottomActionBar(
                isBookmarked = illust.isBookmarked,
                onBookmarkClick = { onBookmarkClick(illust.id) },
                onDownloadClick = downloadAction,
                onShareClick = onShareClick
            )
        }
    ) { innerPadding ->

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(160.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                ),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
                start = 12.dp,
                end = 12.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 12.dp
        ) {
            // ✨ 关键修正：将图片轮播和作者资讯合并到同一个 item 中，以消除间距
            item(span = StaggeredGridItemSpan.FullLine) {
                Column {
                    // 1. 圖片輪播區塊
                    val imageUrls = remember(illust) {
                        if (illust.pageCount == 1) {
                            listOf(illust.imageUrls.large ?: illust.imageUrls.medium)
                        } else {
                            illust.metaPages.map { it.imageUrls.large ?: it.imageUrls.medium ?: "" }
                        }
                    }
                    val pagerState = rememberPagerState(pageCount = { illust.pageCount })
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(500.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null, // <--- 禁用涟漪
                                onClick = {
                                    val intent = ImageViewerActivity.newIntent(
                                        context,
                                        imageUrls.filterNotNull(),
                                        pagerState.currentPage
                                    )
                                    context.startActivity(intent)
                                }
                            ),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        HorizontalPager(state = pagerState) { pageIndex ->
                            // 預覽時使用 AsyncImage (不可縮放)
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(imageUrls.getOrNull(pageIndex))
                                    .crossfade(true)
                                    .build(),
                                imageLoader = ImageLoaderFactory.getPixivImageLoader(context),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        if (illust.pageCount > 1) {
                            PageIndicator(
                                currentPage = pagerState.currentPage + 1,
                                totalPage = illust.pageCount
                            )
                        }
                    }

                    // 2. 作者資訊區塊 (緊貼圖片下方)
                    AuthorSection(
                        illust = illust,
                        onToggleFollow = onToggleFollow
                    )
                }
            }

            // 3. 标题与标签
            item(span = StaggeredGridItemSpan.FullLine) {
                Column(
                    modifier = Modifier.padding(
                        top = 0.dp,
                        bottom = 0.dp,
                        start = 4.dp,
                        end = 4.dp
                    )
                ) {
                    Text(
                        text = illust.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.longPressToCopy(textToCopy = illust.title)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = TimeUtil.formatPixivDate(illust.createDate),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${illust.totalView} 觀看",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${illust.totalBookmarks} 喜歡",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ID: ${illust.id}  |  ${illust.width}x${illust.height}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TagsSection(tags = illust.tags)
                }
            }

            // 4. "相关作品" 标题
            if (relatedIllusts.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        text = "相關作品",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 1.dp)
                    )
                }
                // 5. 相關作品網格
                itemsIndexed(items = relatedIllusts, key = { _, item -> item.id }) { index, item ->
                    HomeIllustCard(
                        illust = item,
                        onCardClick = { onIllustClick(item.id) },
                        onBookmarkClick = { onBookmarkClick(item.id) }
                    )
                }
            }
        }
    }
}


// --- 輔助元件 ---

@Composable
fun AuthorSection(illust: Illust, onToggleFollow: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = UserActivity.newIntent(context, illust.user.id)
                context.startActivity(intent)
            }
            // ✨ 修正 1：只保留垂直 padding，移除水平 padding
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ✨ 修正 2：將水平 padding 直接應用在頭像上
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(illust.user.profileImageUrls.medium)
                .crossfade(true)
                .build(),
            imageLoader = ImageLoaderFactory.getPixivImageLoader(context),
            contentDescription = "Avatar",
            modifier = Modifier
                .padding(start = 4.dp) // <--- 讓頭像從 16dp 開始
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // ✨ 修正 3：讓文字區塊佔據所有可用空間
        Column(modifier = Modifier.weight(1f)) {
            // ✨ 修正 4：為使用者名稱加上 maxLines 和 overflow
            Text(
                text = illust.user.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // ✨ 修正 5：為使用者 ID 也加上，以防萬一
            Text(
                text = "@${illust.user.account}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 移除 Spacer(modifier = Modifier.weight(1f))，因為 Column 已經有 weight

        Button(
            onClick = onToggleFollow,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (illust.user.isFollowed) Color.Gray else MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .height(36.dp)
                // ✨ 修正 6：將水平 padding 應用在按鈕上
                .padding(end = 16.dp), // <--- 讓按鈕距離右邊緣 16dp
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Text(
                text = if (illust.user.isFollowed) "已關注" else "關注",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsSection(tags: List<Tag>) {
    val context = LocalContext.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        tags.forEach { tag ->
            val displayText = tag.translatedName ?: tag.name ?: ""
            if (displayText.isNotBlank()) {
                AssistChip(
                    onClick = {
                        val intent = SearchActivity.newIntent(context, displayText)
                        context.startActivity(intent)
                    },
                    label = {
                        Text(
                            text = "#$displayText",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.5f
                        ), labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = null
                )
            }
        }
    }
}

@Composable
fun BottomActionBar(
    isBookmarked: Boolean,
    onBookmarkClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onShareClick: () -> Unit
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionItem(
                icon = if (isBookmarked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                label = if (isBookmarked) "已收藏" else "收藏",
                onClick = onBookmarkClick,
                tint = if (isBookmarked) Color.Red else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            ActionItem(
                icon = Icons.Outlined.Download,
                label = "下載",
                onClick = onDownloadClick,
                modifier = Modifier.weight(1f)
            )
            ActionItem(
                icon = Icons.Filled.Share,
                label = "分享",
                onClick = onShareClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RowScope.ActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = tint)
            Text(text = label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun PageIndicator(currentPage: Int, totalPage: Int) {
    Box(
        modifier = Modifier
            .padding(16.dp)
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "$currentPage / $totalPage",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium
        )
    }
}