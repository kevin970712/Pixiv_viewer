package com.android.pixivviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.pixivviewer.DetailActivity
import com.android.pixivviewer.UserActivity
import com.android.pixivviewer.network.Illust
import com.android.pixivviewer.network.User
import kotlinx.collections.immutable.ImmutableList
import com.android.pixivviewer.network.UserPreview
import com.android.pixivviewer.utils.ImageLoaderFactory

// ===== 1. 瀑布流網格 (從 IllustComponents.kt 搬過來) =====
@Composable
fun IllustStaggeredGrid(
    illusts: ImmutableList<Illust>,
    modifier: Modifier = Modifier,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    contentPadding: PaddingValues = PaddingValues(12.dp),
    headerContent: (@Composable () -> Unit)? = null,
    onLoadMore: () -> Unit = {}
) {
    val context = LocalContext.current
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(160.dp),
        state = gridState,
        verticalItemSpacing = 12.dp,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = contentPadding,
        modifier = modifier.fillMaxSize()
    ) {
        if (headerContent != null) {
            item(span = StaggeredGridItemSpan.FullLine) { headerContent() }
        }
        itemsIndexed(items = illusts, key = { _, illust -> illust.id }) { index, illust ->
            if (index >= illusts.size - 5) {
                onLoadMore()
            }
            HomeIllustCard(
                illust = illust,
                onCardClick = {
                    val intent = DetailActivity.newIntent(context, illust.id)
                    context.startActivity(intent)
                },
                onBookmarkClick = { /* TODO */ }
            )
        }
        item(span = StaggeredGridItemSpan.FullLine) { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

// ===== 2. 插畫卡片 (從 IllustComponents.kt 搬過來) =====
@Composable
fun HomeIllustCard(
    illust: Illust,
    onCardClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(12.dp)
    val aspectRatio =
        if (illust.height > 0) illust.width.toFloat() / illust.height.toFloat() else 1f
    val clampedRatio = aspectRatio.coerceIn(0.5f, 2.0f)

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = shape,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(illust.imageUrls.medium).build(),
                    imageLoader = ImageLoaderFactory.getPixivImageLoader(context),
                    contentDescription = illust.title,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(clampedRatio)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = illust.title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = illust.user.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onBookmarkClick) {
                    Icon(
                        imageVector = if (illust.isBookmarked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (illust.isBookmarked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// ===== 3. 關注用戶卡片 (從 NewWorksScreen.kt 搬過來) =====
@Composable
fun FollowingUserCard(
    preview: UserPreview,
    onFollowClick: (User) -> Unit
) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.3f
            )
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 0 until 3) {
                    val illust = preview.illusts.getOrNull(i)
                    if (illust != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(illust.imageUrls.squareMedium).crossfade(true).build(),
                            imageLoader = ImageLoaderFactory.getPixivImageLoader(context),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val intent = DetailActivity.newIntent(context, illust.id)
                                    context.startActivity(intent)
                                }
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    val intent = UserActivity.newIntent(context, preview.user.id)
                    context.startActivity(intent)
                }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(preview.user.profileImageUrls.medium)
                        .crossfade(true).build(),
                    imageLoader = ImageLoaderFactory.getPixivImageLoader(context),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = preview.user.name, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { onFollowClick(preview.user) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (preview.user.isFollowed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = if (preview.user.isFollowed) "已關注" else "關注",
                        color = if (preview.user.isFollowed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

// ===== 4. 統計項目元件 (從 ProfileScreen.kt 搬過來) =====
@Composable
fun StatItem(count: Int, label: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ===== 5. 其他小型元件 =====
@Composable
fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}