package com.jian.relive.feature.compress.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
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
import androidx.compose.ui.input.pointer.pointerInput
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
import com.jian.relive.core.media.MediaAnalysisManager
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
import com.jian.relive.feature.compress.model.CompressionSummary
import com.jian.relive.feature.compress.model.CompressionSummaryItem
import com.jian.relive.feature.compress.model.ImageQualityOption
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ImageCompressionSection(
    viewModel: CompressViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val scope = rememberCoroutineScope()

    val allMedia by viewModel.allMediaItems.collectAsStateWithLifecycle()
    val photos = remember(allMedia) { allMedia.filter { !it.isVideo } }

    val selectedIds by viewModel.imageSelectedIds.collectAsStateWithLifecycle()
    val thresholdMb by viewModel.imageThresholdMb.collectAsStateWithLifecycle()
    val selectedQualityKey by viewModel.imageQualityKey.collectAsStateWithLifecycle()
    val isCompressing by viewModel.isImageCompressing.collectAsStateWithLifecycle()
    val progress by viewModel.imageProgress.collectAsStateWithLifecycle()
    val activeItem by viewModel.activeImageItem.collectAsStateWithLifecycle()
    val errorMessage by viewModel.imageErrorMessage.collectAsStateWithLifecycle()
    val resultSummary by viewModel.imageResultSummary.collectAsStateWithLifecycle()
    val removedIds by viewModel.imageRemovedIds.collectAsStateWithLifecycle()

    val minBytes = thresholdMb * 1024L * 1024L
    val qualityOptions = viewModel.qualityOptions
    val selectedQuality = qualityOptions.firstOrNull { it.key == selectedQualityKey } ?: qualityOptions[1]

    val visiblePhotos by remember(photos, removedIds, minBytes) {
        derivedStateOf {
            photos.filter { item ->
                val effectiveSize = MediaAnalysisManager.getEffectiveSize(item)
                effectiveSize >= minBytes && !removedIds.contains(item.id)
            }
        }
    }

    val monthFormat = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
    val groupedByMonth by remember(visiblePhotos) {
        derivedStateOf {
            visiblePhotos.groupBy { monthFormat.format(Date(it.dateAdded * 1000)) }
                .toSortedMap(compareByDescending { it })
        }
    }

    val gridState = rememberLazyGridState()
    val dragVisitedIds = remember { mutableSetOf<Long>() }
    var dragSelectMode by remember { mutableStateOf(true) }

    var pendingDeleteIds by remember { mutableStateOf<List<Long>?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val currentPending = pendingDeleteIds
        if (result.resultCode == Activity.RESULT_OK && currentPending != null) {
            viewModel.onImageDeleteConfirmed(currentPending)
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
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                state = gridState,
                modifier = Modifier
                    .weight(1f)
                    .pointerInput(gridState, selectedIds) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                dragVisitedIds.clear()
                                val id = findGridItemIdAtOffset(gridState, offset)
                                if (id != null) {
                                    dragSelectMode = !selectedIds.contains(id)
                                    val current = selectedIds.toMutableSet()
                                    if (dragSelectMode) current.add(id) else current.remove(id)
                                    viewModel.setImageSelection(current)
                                    dragVisitedIds.add(id)
                                }
                            },
                            onDrag = { change, _ ->
                                val id = findGridItemIdAtOffset(gridState, change.position)
                                if (id != null && !dragVisitedIds.contains(id)) {
                                    val current = selectedIds.toMutableSet()
                                    if (dragSelectMode) current.add(id) else current.remove(id)
                                    viewModel.setImageSelection(current)
                                    dragVisitedIds.add(id)
                                }
                                change.consume()
                            },
                            onDragEnd = { dragVisitedIds.clear() },
                            onDragCancel = { dragVisitedIds.clear() }
                        )
                    },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        CompressionWarningCard(
                            title = stringResource(R.string.compress_image_warning_title),
                            message = stringResource(R.string.compress_image_warning_body)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ImageCompressionQualityCard(
                            selected = selectedQuality,
                            options = qualityOptions,
                            onSelect = { option -> viewModel.setImageQualityKey(option.key) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ImageCompressionThresholdCard(
                            label = stringResource(R.string.compress_image_threshold_label),
                            thresholdMb = thresholdMb,
                            onThresholdChange = { mb -> viewModel.setImageThreshold(mb) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (visiblePhotos.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.compress_image_select_hint),
                                fontSize = 14.sp,
                                color = ReliveGray600
                            )
                        }
                    }
                } else {
                    groupedByMonth.forEach { (month, itemsInMonth) ->
                        val monthIds = itemsInMonth.map { it.id }
                        val monthSelectedCount = itemsInMonth.count { selectedIds.contains(it.id) }
                        val allSelected = monthSelectedCount == itemsInMonth.size && itemsInMonth.isNotEmpty()

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = month,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ReliveBlack,
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = allSelected,
                                    onClick = {
                                        viewModel.toggleMonthImagesSelection(monthIds, !allSelected)
                                    },
                                    label = {
                                        Text(
                                            text = if (allSelected) {
                                                stringResource(R.string.compress_image_selected)
                                            } else {
                                                stringResource(R.string.compress_image_select_all)
                                            },
                                            maxLines = 1
                                        )
                                    },
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

                        items(itemsInMonth, key = { it.id }) { item ->
                            val isSelected = selectedIds.contains(item.id)
                            val displaySize = MediaAnalysisManager.getEffectiveSize(item)
                            ImageCompressionGridItem(
                                item = item,
                                isSelected = isSelected,
                                displaySize = displaySize,
                                onToggle = { viewModel.toggleImageSelection(item.id) }
                            )
                        }
                    }
                }
            }

            CompressionFooter(
                isCompressing = isCompressing,
                progress = progress,
                activeName = activeItem?.displayName,
                errorMessage = errorMessage,
                actionLabel = stringResource(R.string.compress_image_action),
                compressingLabel = stringResource(R.string.compress_image_compressing),
                enabled = selectedIds.isNotEmpty(),
                onCompress = {
                    if (hapticsAllowed) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    viewModel.startImageCompression(visiblePhotos)
                }
            )
        }

        val summary = resultSummary
        if (summary != null) {
            ImageCompressionResultView(
                summary = summary,
                isDeleting = isDeleting,
                onKeep = { viewModel.dismissImageResultSummary() },
                onDelete = {
                    if (!isDeleting) {
                        isDeleting = true
                        scope.launch {
                            val intentSender = viewModel.deleteOriginalImages(summary.items)
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
private fun ImageCompressionQualityCard(
    selected: ImageQualityOption,
    options: List<ImageQualityOption>,
    onSelect: (ImageQualityOption) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ReliveWhite),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            val description = if (selected.key == "original") {
                "${stringResource(R.string.compress_image_original_size)} · ${selected.quality}%"
            } else {
                "${selected.maxSizePx}px · ${selected.quality}%"
            }
            Text(
                text = description,
                fontSize = 12.sp,
                color = ReliveGray600
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    val label = when (option.key) {
                        "small" -> "S"
                        "balanced" -> "M"
                        "sharp" -> "L"
                        else -> stringResource(R.string.compress_image_original_label)
                    }
                    FilterChip(
                        selected = option.key == selected.key,
                        onClick = { onSelect(option) },
                        label = { Text(text = label, maxLines = 1) },
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
private fun ImageCompressionThresholdCard(
    label: String,
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
                text = label,
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
                    val rounded = value.roundToInt().coerceIn(3, 10)
                    if (rounded != thresholdMb) {
                        if (hapticsAllowed) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onThresholdChange(rounded)
                    }
                },
                valueRange = 3f..10f,
                steps = 6,
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
                Text(text = "3MB", fontSize = 11.sp, color = ReliveGray400)
                Text(text = "10MB", fontSize = 11.sp, color = ReliveGray400)
            }
        }
    }
}

@Composable
private fun ImageCompressionGridItem(
    item: MediaItem,
    isSelected: Boolean,
    displaySize: Long,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(ReliveGray200)
            .bounceClick(onClick = onToggle)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.uri)
                .memoryCacheKey("compress_image_${item.id}")
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(ReliveBlack.copy(alpha = 0.5f))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = formatFileSize(displaySize),
                fontSize = 9.sp,
                color = ReliveWhite,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                softWrap = false
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(if (isSelected) ReliveBlue500 else ReliveWhite.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = ReliveWhite,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun ImageCompressionResultView(
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
                text = stringResource(R.string.compress_image_result_title),
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
                        text = "${stringResource(R.string.compress_image_result_saved)} ${formatFileSize(savedBytes)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ReliveBlack
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${stringResource(R.string.compress_image_result_count)} ${summary.items.size}",
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
                    ImageCompressionResultItem(item = item)
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
                    text = stringResource(R.string.compress_image_delete_original),
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
                    text = stringResource(R.string.compress_image_keep_original),
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
private fun ImageCompressionResultItem(
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
                    .memoryCacheKey("compress_result_${item.id}")
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
                text = item.displayName.ifBlank { "Image ${item.id}" },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = ReliveBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${stringResource(R.string.compress_image_before)} ${formatFileSize(item.originalSize)} · ${stringResource(R.string.compress_image_after)} ${formatFileSize(item.outputSize)}",
                fontSize = 12.sp,
                color = ReliveBlack
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${stringResource(R.string.compress_image_ratio)} ${ratio.toInt()}%",
                fontSize = 12.sp,
                color = ReliveGray600
            )
        }
    }
}
