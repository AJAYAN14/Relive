package com.jian.relive.feature.feed.ui

import android.net.Uri
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.jian.relive.core.media.MotionPhotoHelper
import com.jian.relive.core.media.VideoPlayerCache
import com.jian.relive.core.model.MediaItem
import com.jian.relive.core.ui.theme.ReliveWhite

/**
 * 竖向沉浸式流媒体播放器组件。
 *
 * 结合常驻 ExoPlayer 实例池与动态照片按需提取机制，支持实况照片与原生视频的无缝循环播放。
 */
@OptIn(UnstableApi::class)
@Composable
fun LiveFeedPlayer(
    item: MediaItem,
    isMuted: Boolean,
    shouldPlay: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var liveVideoUri by remember(item.id) { mutableStateOf<Uri?>(item.livePhotoVideoUri) }

    LaunchedEffect(item.id, shouldPlay) {
        if (item.isLivePhoto && liveVideoUri == null && shouldPlay) {
            val extracted = MotionPhotoHelper.getOrExtractVideoUri(context, item)
            liveVideoUri = extracted
        }
    }

    val playUri = when {
        item.type == "video" -> item.uri
        item.isLivePhoto -> liveVideoUri
        else -> null
    }

    if (playUri != null) {
        ExoVideoSurface(
            uri = playUri,
            mimeType = item.mimeType,
            isMuted = isMuted,
            shouldPlay = shouldPlay,
            placeholderUri = item.uri,
            modifier = modifier
        )
    } else {
        FeedImagePlaceholder(
            uri = item.uri,
            modifier = modifier
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ExoVideoSurface(
    uri: Uri,
    mimeType: String,
    isMuted: Boolean,
    shouldPlay: Boolean,
    placeholderUri: Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val player = remember { VideoPlayerCache.get(context) }
    var isFirstFrameRendered by remember(uri) { mutableStateOf(false) }
    var aspectRatio by remember { mutableFloatStateOf(0f) }

    DisposableEffect(player) {
        onDispose {
            player.playWhenReady = false
            VideoPlayerCache.release(player)
        }
    }

    LaunchedEffect(uri, shouldPlay) {
        if (shouldPlay) {
            val currentItemUri = player.currentMediaItem?.localConfiguration?.uri
            if (currentItemUri != uri) {
                isFirstFrameRendered = false
                val exoItem = ExoMediaItem.Builder()
                    .setUri(uri)
                    .setMimeType(mimeType)
                    .build()
                player.setMediaItem(exoItem)
                player.repeatMode = Player.REPEAT_MODE_ONE
                player.prepare()
            }
            player.playWhenReady = true
        } else {
            player.playWhenReady = false
        }
    }

    LaunchedEffect(isMuted) {
        player.volume = if (isMuted) 0f else 1f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ReliveWhite)
    ) {
        FeedImagePlaceholder(
            uri = placeholderUri,
            modifier = Modifier.fillMaxSize()
        )

        AndroidView(
            factory = { ctx ->
                AspectRatioFrameLayout(ctx).apply {
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    val textureView = TextureView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                    addView(textureView)
                    player.setVideoTextureView(textureView)
                }
            },
            update = { frameLayout ->
                if (aspectRatio > 0f) {
                    frameLayout.setAspectRatio(aspectRatio)
                }
            },
            onRelease = { frameLayout ->
                val textureView = frameLayout.getChildAt(0) as? TextureView
                player.clearVideoTextureView(textureView)
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = if (isFirstFrameRendered) 1f else 0f
                }
        )

        DisposableEffect(player) {
            val listener = object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    isFirstFrameRendered = true
                }

                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        aspectRatio = videoSize.width.toFloat() / videoSize.height
                    }
                }
            }
            player.addListener(listener)
            val currentSize = player.videoSize
            if (currentSize.width > 0 && currentSize.height > 0) {
                aspectRatio = currentSize.width.toFloat() / currentSize.height
            }
            onDispose {
                player.removeListener(listener)
            }
        }
    }
}

@Composable
private fun FeedImagePlaceholder(
    uri: Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val request = ImageRequest.Builder(context)
        .data(uri)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .crossfade(true)
        .build()

    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxSize()
            .background(ReliveWhite)
    )
}
