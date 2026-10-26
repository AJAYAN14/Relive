package com.jian.relive.feature.organize.ui

import android.location.Geocoder
import android.net.Uri
import android.view.TextureView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Iso
import androidx.compose.material.icons.outlined.Lens
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.exifinterface.media.ExifInterface
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import com.jian.relive.core.common.util.formatFileSize
import com.jian.relive.core.common.util.formatShutterSpeed
import com.jian.relive.core.media.MotionPhotoHelper
import com.jian.relive.core.media.VideoPlayerCache
import com.jian.relive.core.model.MediaItem
import com.jian.relive.core.ui.animation.bounceClick
import com.jian.relive.core.ui.component.SkeletonLoader
import com.jian.relive.core.ui.theme.ReliveBlack
import com.jian.relive.core.ui.theme.ReliveGray100
import com.jian.relive.core.ui.theme.ReliveWhite
import com.jian.relive.feature.organize.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 单张照片/实况/视频渲染卡片。
 *
 * 封装媒体缩略图、实况流静音循环播放、EXIF 拍摄数据提取以及展开面板展示。
 */
@Composable
fun PhotoCard(
    item: MediaItem,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    isTopCard: Boolean = true,
    stackIndex: Int = 0,
    isExpanded: Boolean = false,
    muteVideos: Boolean = true,
    onToggleExpand: () -> Unit = {},
    showActions: Boolean = true,
    onDataLoaded: (MediaItem) -> Unit = {}
) {
    val context = LocalContext.current
    var isMuted by remember { mutableStateOf(muteVideos) }
    var exifLoaded by remember { mutableStateOf(false) }
    var currentItem by remember { mutableStateOf(item) }
    var liveVideoUri by remember { mutableStateOf<Uri?>(item.livePhotoVideoUri) }

    LaunchedEffect(item.id) {
        currentItem = item
        exifLoaded = false
        if (item.type == "photo") {
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(item.uri)?.use { stream ->
                        val exif = ExifInterface(stream)
                        val make = exif.getAttribute(ExifInterface.TAG_MAKE).orEmpty()
                        val model = exif.getAttribute(ExifInterface.TAG_MODEL).orEmpty()
                        val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY) ?: "--"
                        val shutter = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME) ?: "--"
                        val focal = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH) ?: "--"
                        val aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER) ?: "--"
                        val latLong = exif.latLong

                        var locationText = if (latLong != null) {
                            String.format(Locale.US, "%.4f, %.4f", latLong[0], latLong[1])
                        } else {
                            ""
                        }

                        if (latLong != null) {
                            try {
                                val geocoder = Geocoder(context, Locale.getDefault())
                                val addresses = geocoder.getFromLocation(latLong[0], latLong[1], 1)
                                if (!addresses.isNullOrEmpty()) {
                                    val address = addresses[0]
                                    val city = address.locality ?: address.subAdminArea ?: address.adminArea
                                    val country = address.countryName
                                    if (city != null) {
                                        locationText = if (country != null) "$city, $country" else city
                                    }
                                }
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                            }
                        }

                        val camera = if (make.isNotEmpty() || model.isNotEmpty()) {
                            "$make $model".trim()
                        } else {
                            "--"
                        }

                        val fNumber = if (aperture != "--") "f/$aperture" else "--"
                        val focalText = if (focal != "--") {
                            try {
                                val parts = focal.split("/")
                                if (parts.size == 2) {
                                    "${(parts[0].toDouble() / parts[1].toDouble()).toInt()}mm"
                                } else {
                                    "${focal.toDouble().toInt()}mm"
                                }
                            } catch (e: Exception) {
                                focal
                            }
                        } else {
                            "--"
                        }

                        val updated = item.copy(
                            camera = camera,
                            iso = iso,
                            shutter = formatShutterSpeed(shutter),
                            focalLength = focalText,
                            aperture = fNumber,
                            location = locationText
                        )
                        withContext(Dispatchers.Main) {
                            currentItem = updated
                            exifLoaded = true
                            onDataLoaded(updated)
                        }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                }
            }
        }

        if (item.isLivePhoto && liveVideoUri == null) {
            withContext(Dispatchers.IO) {
                val extracted = MotionPhotoHelper.getOrExtractVideoUri(context, item)
                withContext(Dispatchers.Main) {
                    liveVideoUri = extracted
                }
            }
        }
    }

    Box(
        modifier = modifier
            .background(ReliveWhite)
            .clip(shape)
    ) {
        when {
            item.type == "video" -> {
                if (isTopCard && !isExpanded) {
                    CardVideoPlayer(
                        uri = item.uri,
                        mimeType = item.mimeType,
                        isMuted = isMuted,
                        shouldPlay = true
                    )

                    if (showActions) {
                        MuteToggleButton(
                            isMuted = isMuted,
                            onToggleMute = { isMuted = !isMuted },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ReliveGray100)
                    ) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(item.uri)
                                .decoderFactory(VideoFrameDecoder.Factory())
                                .crossfade(true)
                                .build(),
                            loading = { SkeletonLoader(Modifier.fillMaxSize()) },
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Icon(
                            imageVector = Icons.Outlined.PlayCircleOutline,
                            contentDescription = null,
                            tint = ReliveBlack.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(48.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
            }

            item.isLivePhoto -> {
                Box(modifier = Modifier.fillMaxSize().background(ReliveGray100)) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(item.uri)
                            .crossfade(false)
                            .build(),
                        loading = { SkeletonLoader(Modifier.fillMaxSize()) },
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isTopCard && !isExpanded && liveVideoUri != null) {
                        CardVideoPlayer(
                            uri = liveVideoUri!!,
                            mimeType = "video/mp4",
                            isMuted = isMuted,
                            shouldPlay = true
                        )

                        if (showActions) {
                            MuteToggleButton(
                                isMuted = isMuted,
                                onToggleMute = { isMuted = !isMuted },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .background(ReliveWhite.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.organize_live_badge),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ReliveBlack
                        )
                    }
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ReliveGray100),
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(item.uri)
                            .crossfade(true)
                            .build(),
                        loading = { SkeletonLoader(Modifier.fillMaxSize()) },
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (!isExpanded && showActions) {
            IconButton(
                onClick = onToggleExpand,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(32.dp)
                    .bounceClick(onClick = onToggleExpand)
                    .clip(CircleShape)
                    .background(ReliveBlack.copy(alpha = 0.25f))
                    .border(1.dp, ReliveWhite.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.organize_action_info),
                    tint = ReliveWhite,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ExifDetailOverlay(
                item = currentItem,
                onClose = onToggleExpand
            )
        }
    }
}

@Composable
private fun MuteToggleButton(
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onToggleMute,
        modifier = modifier
            .bounceClick(onClick = onToggleMute)
            .clip(CircleShape)
            .background(ReliveBlack.copy(alpha = 0.25f))
            .border(1.dp, ReliveWhite.copy(alpha = 0.2f), CircleShape)
    ) {
        AnimatedContent(
            targetState = isMuted,
            transitionSpec = { scaleIn() togetherWith scaleOut() },
            label = "mute_toggle"
        ) { muted ->
            Icon(
                imageVector = if (muted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp,
                contentDescription = stringResource(R.string.organize_action_mute),
                tint = ReliveWhite,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CardVideoPlayer(
    uri: Uri,
    mimeType: String,
    isMuted: Boolean,
    shouldPlay: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember { VideoPlayerCache.get(context) }

    LaunchedEffect(uri, shouldPlay) {
        if (!shouldPlay) {
            exoPlayer.pause()
            return@LaunchedEffect
        }
        val mediaItem = ExoMediaItem.Builder()
            .setUri(uri)
            .setMimeType(mimeType)
            .build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
        exoPlayer.volume = if (isMuted) 0f else 1f
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.pause()
        }
    }

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                exoPlayer.setVideoTextureView(this)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

@Composable
private fun ExifDetailOverlay(
    item: MediaItem,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ReliveWhite.copy(alpha = 0.96f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {}
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 24.dp)
                .clip(CircleShape)
                .background(ReliveBlack.copy(alpha = 0.05f))
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.organize_action_close),
                tint = ReliveBlack.copy(alpha = 0.6f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp)
                .offset(y = (-30).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val date = Date(item.dateAdded * 1000L)
            val formatter = remember { SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()) }

            Text(
                text = formatter.format(date),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = ReliveBlack.copy(alpha = 0.45f),
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = item.displayName,
                fontFamily = FontFamily.Serif,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 260.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.organize_exif_filename),
                fontSize = 10.sp,
                color = ReliveBlack.copy(alpha = 0.4f),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Row(Modifier.fillMaxWidth()) {
                ExifItemBox(
                    icon = Icons.Outlined.Image,
                    value = item.resolution.ifEmpty { "--" },
                    label = stringResource(R.string.organize_exif_resolution),
                    modifier = Modifier.weight(1f)
                )
                ExifItemBox(
                    icon = Icons.Outlined.Save,
                    value = formatFileSize(item.size),
                    label = stringResource(R.string.organize_exif_size),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(Modifier.fillMaxWidth()) {
                ExifItemBox(
                    icon = Icons.Outlined.PhotoCamera,
                    value = item.camera.ifEmpty { "--" },
                    label = stringResource(R.string.organize_exif_camera),
                    modifier = Modifier.weight(1f)
                )
                ExifItemBox(
                    icon = Icons.Outlined.LocationOn,
                    value = item.location.ifEmpty { stringResource(R.string.organize_exif_unknown_location) },
                    label = stringResource(R.string.organize_exif_location),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(Modifier.fillMaxWidth()) {
                ExifItemBox(
                    icon = Icons.Outlined.Iso,
                    value = item.iso.ifEmpty { "--" },
                    label = stringResource(R.string.organize_exif_iso),
                    modifier = Modifier.weight(1f)
                )
                ExifItemBox(
                    icon = Icons.Outlined.Timer,
                    value = item.shutter.ifEmpty { "--" },
                    label = stringResource(R.string.organize_exif_shutter),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(Modifier.fillMaxWidth()) {
                ExifItemBox(
                    icon = Icons.Outlined.Camera,
                    value = item.focalLength.ifEmpty { "--" },
                    label = stringResource(R.string.organize_exif_focal),
                    modifier = Modifier.weight(1f)
                )
                ExifItemBox(
                    icon = Icons.Outlined.Lens,
                    value = item.aperture.ifEmpty { "--" },
                    label = stringResource(R.string.organize_exif_aperture),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ExifItemBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ReliveBlack.copy(alpha = 0.75f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontFamily = FontFamily.Serif,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = ReliveBlack.copy(alpha = 0.4f),
                letterSpacing = 1.sp
            )
        }
    }
}
