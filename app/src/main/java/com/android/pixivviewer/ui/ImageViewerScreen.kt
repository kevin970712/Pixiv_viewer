package com.android.pixivviewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import com.android.pixivviewer.utils.ImageLoaderFactory
import com.android.pixivviewer.viewmodel.ImageViewerViewModel
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState


@Composable
fun ImageViewerScreen(viewModel: ImageViewerViewModel) {
    val context = LocalContext.current
    val pagerState =
        rememberPagerState(initialPage = viewModel.initialPage) { viewModel.imageUrls.size }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val zoomableState = rememberZoomableState()
            val imageState = rememberZoomableImageState(zoomableState)

            Box(modifier = Modifier.fillMaxSize()) {
                ZoomableAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(viewModel.imageUrls.getOrNull(pageIndex))
                        .crossfade(true)
                        .build(),
                    imageLoader = ImageLoaderFactory.getPixivImageLoader(context),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    state = imageState
                )

                if (!imageState.isImageDisplayed) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}