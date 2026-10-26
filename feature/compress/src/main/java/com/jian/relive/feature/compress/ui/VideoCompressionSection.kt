package com.jian.relive.feature.compress.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.jian.relive.core.common.util.formatFileSize
import com.jian.relive.core.model.MediaItem
import com.jian.relive.core.ui.animation.bounceClick
import com.jian.relive.core.ui.component.CelebrationConfetti
import com.jian.relive.core.ui.composition.LocalHapticsEnabled
import com.jian.relive.core.ui.theme.ReliveBlack
import com.jian.relive.core.ui.theme.ReliveBlue500
import com.jian.relive.core.ui.theme.ReliveGray100
import com.jian.relive.core.ui.theme.ReliveGray200
import com.jian.relive.core.ui.theme.ReliveGray400
import com.jian.relive.core.ui.theme.ReliveGray600
import com.jian.relive.core.ui.theme.ReliveGray900
import com.jian.relive.core.ui.theme.ReliveSlate100
import com.jian.relive.core.ui.theme.ReliveWhite
import com.jian.relive.feature.compress.CompressViewModel
import com.jian.relive.feature.compress.R
import com.jian.relive.feature.compress.model.BitrateOption
import com.jian.relive.feature.compress.model.CompressionSummary
import com.jian.relive.feature.compress.model.CompressionSummaryItem
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun VideoCompressionSection(
    viewModel: CompressViewModel,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val scope = rememberCoroutineScope()

    val allMedia by viewModel.allMediaItems.collectAsStateWithLifecycle()
    val videos = remember(allMedia) { allMedia.filter { it.isVideo } }

    val selectedIds by viewModel.videoSelectedIds.collectAsStateWithLifecycle()
    val thresholdMb by viewModel.videoThresholdMb.collectAsStateWithLifecycle()
    val selectedBitrateKey by viewModel.videoBitrateKey.collectAsStateWithLifecycle()
    val isCompressing by viewModel.isVideoCompressing.collectAsStateWithLifecycle()
    val progress by viewModel.videoProgress.collectAsStateWithLifecycle()
    val activeItem by viewModel.activeVideoItem.collectAsStateWithLifecycle()
    val errorMessage by viewModel.videoErrorMessage.collectAsStateWithLifecycle()
    val resultSummary by viewModel.videoResultSummary.collectAsStateWithLifecycle()
    val removedIds by viewModel.videoRemovedIds.collectAsStateWithLifecycle()
    val durationMap by viewModel.videoDurationMap.collectAsStateWithLifecycle()

    val bitrateOptions = viewModel.bitrateOptions
    val selectedBitrate = bitrateOptions.firstOrNull { it.key == selectedBitrateKey } ?: bitrateOptions[1]

    val visibleVideos by remember(videos, removedIds, durationMap, selectedBitrateKey, thresholdMb) {
        derivedStateOf {
            videos
                .filter { !removedIds.contains(it.id) }
                .filter { item ->
                    if (item.size < thresholdMb * 1024L * 1024L) return@filter false
                    val durationMs = durationMap[item.id]
                    if (durationMs == null || durationMs <= 0) {
                        true
                    } else {
                        val originalBitrate = viewModel.estimateOriginalVideoBitrate(item.size, durationMs)
                        if (originalBitrate == null) {
                            true
                        } else {
                            val targetVideoBitrate = viewModel.deriveTargetVideoBitrate(item.size, durationMs, selectedBitrate)
                            targetVideoBitrate < originalBitrate
                        }
                    }
                }
                .sortedWith(
                    compareByDescending<MediaItem> { it.dateAdded }
                        .thenByDescending { it.id }
                )
        }
    }

    LaunchedEffect(visibleVideos) {
        viewModel.warmVideos(visibleVideos)
    }

    var pendingDeleteIds by remember { mutableStateOf<List<Long>?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val currentPending = pendingDeleteIds
        if (result.resultCode == Activity.RESULT_OK && currentPending != null) {
            viewModel.onVideoDeleteConfirmed(currentPending)
        }
        pendingDeleteIds = null
        isDeleting = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ReliveGray100)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column {
                        CompressionWarningCard(
                            title = stringResource(R.string.compress_video_warning_title),
                            message = stringResource(R.string.compress_video_warning_body)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        VideoCompressionTargetCard(
                            options = bitrateOptions,
                            selectedKey = selectedBitrateKey,
                            onSelect = { option -> viewModel.setVideoBitrateKey(option.key) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        VideoCompressionThresholdCard(
                            thresholdMb = thresholdMb,
                            onThresholdChange = { mb -> viewModel.setVideoThreshold(mb) }
                        )
                    }
                }

                if (visibleVideos.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.compress_video_select_hint),
                                fontSize = 14.sp,
                                color = ReliveGray600
                            )
                        }
                    }
                } else {
                    items(visibleVideos, key = { it.id }) { item ->
                        val isSelected = selectedIds.contains(item.id)
                        val durationMs = durationMap[item.id]
                        val estimatedSize = if (durationMs != null && durationMs > 0) {
                            val targetVideoBitrate = viewModel.deriveTargetVideoBitrate(item.size, durationMs, selectedBitrate)
                            val totalBitrate = targetVideoBitrate + selectedBitrate.audioBitrate
                            val seconds = durationMs / 1000.0
                            ((totalBitrate * seconds) / 8.0).toLong().coerceAtLeast(1L)
                        } else null

                        VideoCompressionListItem(
                            item = item,
                            isSelected = isSelected,
                            estimatedSize = estimatedSize,
                            onToggle = { viewModel.toggleVideoSelection(item.id) }
                        )
                    }
                }
            }

            CompressionFooter(
                isCompressing = isCompressing,
                progress = progress,
                activeName = activeItem?.displayName,
                errorMessage = errorMessage,
                actionLabel = stringResource(R.string.compress_video_action),
                compressingLabel = stringResource(R.string.compress_video_compressing),
                enabled = selectedIds.isNotEmpty(),
                onCompress = {
                    if (hapticsAllowed) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    viewModel.startVideoCompression(visibleVideos)
                }
            )
        }

        val summary = resultSummary
        if (summary != null) {
            VideoCompressionResultView(
                summary = summary,
                isDeleting = isDeleting,
                onKeep = { viewModel.dismissVideoResultSummary() },
                onDelete = {
                    if (!isDeleting) {
                        isDeleting = true
                        scope.launch {
                            val intentSender = viewModel.deleteOriginalVideos(summary.items)
                            if (intentSender != null) {
                                pendingDeleteIds = summary.items.map { it.id }
                                deleteLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                            } else {
                                isDeleting = false
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun VideoCompressionTargetCard(
    options: List<BitrateOption>,
    selectedKey: String,
    onSelect: (BitrateOption) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ReliveWhite),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = stringResource(R.string.compress_video_target_label),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ReliveBlack
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    FilterChip(
                        selected = option.key == selectedKey,
                        onClick = { onSelect(option) },
                        label = { Text(text = stringResource(option.labelRes), maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ReliveBlack,
                            selectedLabelColor = ReliveWhite,
                            containerColor = ReliveGray100,
                            labelColor = ReliveBlack
                        ),
                        border = null,
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoCompressionThresholdCard(
    thresholdMb: Int,
    onThresholdChange: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current

    Card(
        colors = CardDefaults.cardColors(containerColor = ReliveWhite),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = stringResource(R.string.compress_video_threshold_label),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ReliveBlack
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "≥ ${formatFileSize(thresholdMb * 1024L * 1024L)}",
                fontSize = 12.sp,
                color = ReliveGray600
            )
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = thresholdMb.toFloat(),
                onValueChange = { value ->
                    val rounded = (value / 10).roundToInt() * 10
                    val clamped = rounded.coerceIn(10, 200)
                    if (clamped != thresholdMb) {
                        if (hapticsAllowed) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onThresholdChange(clamped)
                    }
                },
                valueRange = 10f..200f,
                steps = 18,
                colors = SliderDefaults.colors(
                    thumbColor = ReliveBlack,
                    activeTrackColor = ReliveBlack,
                    inactiveTrackColor = ReliveGray200
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "10MB", fontSize = 11.sp, color = ReliveGray400)
                Text(text = "200MB", fontSize = 11.sp, color = ReliveGray400)
            }
        }
    }
}

@Composable
private fun VideoCompressionListItem(
    item: MediaItem,
    isSelected: Boolean,
    estimatedSize: Long?,
    onToggle: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ReliveWhite),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ReliveGray200)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.uri)
                        .memoryCacheKey("compress_video_${item.id}")
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ReliveBlack.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayCircleOutline,
                        contentDescription = null,
                        tint = ReliveWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.displayName.ifBlank { "Video ${item.id}" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = ReliveBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val estimateText = if (estimatedSize != null) {
                    " · ${stringResource(R.string.compress_video_estimate)} ~${formatFileSize(estimatedSize)}"
                } else ""
                Text(
                    text = "${formatFileSize(item.size)}$estimateText",
                    fontSize = 12.sp,
                    color = ReliveGray600
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) ReliveBlue500 else ReliveGray200),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = ReliveWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoCompressionResultView(
    summary: CompressionSummary,
    isDeleting: Boolean,
    onKeep: () -> Unit,
    onDelete: () -> Unit
) {
    val savedBytes = (summary.totalOriginal - summary.totalOutput).coerceAtLeast(0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ReliveGray100)
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.compress_video_result_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ReliveBlack
            )
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = ReliveWhite),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${stringResource(R.string.compress_video_result_saved)} ${formatFileSize(savedBytes)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ReliveBlack
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${stringResource(R.string.compress_video_result_count)} ${summary.items.size}",
                        fontSize = 12.sp,
                        color = ReliveGray600
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(summary.items, key = { it.id }) { item ->
                    VideoCompressionResultItem(item = item)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onDelete,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(containerColor = ReliveGray900),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .bounceClick(onClick = onDelete)
            ) {
                Text(
                    text = stringResource(R.string.compress_video_delete_original),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ReliveWhite
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onKeep,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(containerColor = ReliveWhite),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .bounceClick(onClick = onKeep)
            ) {
                Text(
                    text = stringResource(R.string.compress_video_keep_original),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ReliveBlack
                )
            }
        }
        CelebrationConfetti()
    }
}

@Composable
private fun VideoCompressionResultItem(
    item: CompressionSummaryItem
) {
    val ratio = if (item.originalSize > 0) {
        (item.outputSize.toFloat() / item.originalSize.toFloat()) * 100f
    } else {
        0f
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ReliveSlate100, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.originalUri)
                    .memoryCacheKey("compress_result_video_${item.id}")
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayName.ifBlank { "Video ${item.id}" },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = ReliveBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${stringResource(R.string.compress_video_before)} ${formatFileSize(item.originalSize)} · ${stringResource(R.string.compress_video_after)} ${formatFileSize(item.outputSize)}",
                fontSize = 12.sp,
                color = ReliveBlack
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${stringResource(R.string.compress_video_ratio)} ${ratio.toInt()}%",
                fontSize = 12.sp,
                color = ReliveGray600
            )
        }
    }
}
