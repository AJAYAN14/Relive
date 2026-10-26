package com.jian.relive.feature.organize.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import com.jian.relive.core.ui.animation.bounceClick
import com.jian.relive.core.ui.animation.staggeredRevealModifier
import com.jian.relive.core.ui.composition.LocalHapticsEnabled
import com.jian.relive.core.ui.theme.ReliveBlack
import com.jian.relive.core.ui.theme.ReliveGray100
import com.jian.relive.core.ui.theme.ReliveWhite
import com.jian.relive.feature.organize.R
import com.jian.relive.feature.organize.model.FilterFolderData
import java.util.UUID

/**
 * 媒体与相册多维过滤面板组件。
 *
 * 支持按特定月份穿梭选择、按媒体类型（全部/照片/视频/实况）横向分段过滤以及按相册文件夹多槽位微缩网格挑选。
 */
@Composable
fun FilterSheet(
    currentFolder: String,
    currentType: String,
    currentMonth: String = "All",
    folderList: List<FilterFolderData>,
    onApply: (String, String, String) -> Unit,
    onBack: () -> Unit,
    onSelectMonth: () -> Unit,
    onClearMonth: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedFolder by remember(currentFolder) { mutableStateOf(currentFolder) }
    var selectedType by remember(currentType) { mutableStateOf(currentType) }
    var selectedMonth by remember(currentMonth) { mutableStateOf(currentMonth) }

    LaunchedEffect(currentMonth) {
        selectedMonth = currentMonth
    }

    val pageId = remember { UUID.randomUUID().toString() }
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ReliveWhite)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clip(CircleShape)
                    .background(ReliveBlack.copy(alpha = 0.05f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.organize_back),
                    tint = ReliveBlack
                )
            }

            Text(
                text = stringResource(R.string.organize_filter_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ReliveBlack,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 56.dp)
            )

            TextButton(
                onClick = {
                    if (hapticsAllowed) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    onApply(selectedFolder, selectedType, selectedMonth)
                },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Text(
                    text = stringResource(R.string.organize_filter_apply),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ReliveBlack
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.organize_filter_month),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ReliveBlack,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .then(staggeredRevealModifier(0, key = pageId + "month_title"))
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(staggeredRevealModifier(1, key = pageId + "month_card"))
                        .clip(RoundedCornerShape(16.dp))
                        .background(ReliveGray100)
                        .bounceClick(onClick = {
                            if (hapticsAllowed) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            onSelectMonth()
                        })
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (selectedMonth.equals("All", ignoreCase = true)) {
                                    stringResource(R.string.organize_filter_all)
                                } else {
                                    selectedMonth
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = ReliveBlack
                            )
                            if (selectedMonth.equals("All", ignoreCase = true)) {
                                Text(
                                    text = stringResource(R.string.organize_select_specific_month),
                                    fontSize = 12.sp,
                                    color = ReliveBlack.copy(alpha = 0.5f)
                                )
                            }
                        }
                        if (!selectedMonth.equals("All", ignoreCase = true)) {
                            IconButton(
                                onClick = {
                                    if (hapticsAllowed) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    selectedMonth = "All"
                                    onClearMonth()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.organize_clear_month),
                                    tint = ReliveBlack.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                contentDescription = stringResource(R.string.organize_select_month),
                                tint = ReliveBlack.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                Text(
                    text = stringResource(R.string.organize_filter_type),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ReliveBlack,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .then(staggeredRevealModifier(2, key = pageId + "type_title"))
                )

                val types = listOf(
                    "All" to stringResource(R.string.organize_filter_all),
                    "Image" to stringResource(R.string.organize_filter_image),
                    "Video" to stringResource(R.string.organize_filter_video),
                    "Live" to stringResource(R.string.organize_filter_live)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(staggeredRevealModifier(3, key = pageId + "type_row"))
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    types.forEach { (key, label) ->
                        val isSelected = selectedType.equals(key, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (hapticsAllowed) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                selectedType = key
                            },
                            label = { Text(label, maxLines = 1, softWrap = false) },
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

                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                Text(
                    text = stringResource(R.string.organize_filter_folder),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ReliveBlack,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .then(staggeredRevealModifier(4, key = pageId + "folder_title"))
                )
            }

            itemsIndexed(folderList) { index, folder ->
                val animIndex = if (index < 6) 5 + index else 1
                FolderItem(
                    data = folder,
                    isSelected = selectedFolder.equals(folder.name, ignoreCase = true),
                    onClick = {
                        if (hapticsAllowed) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        selectedFolder = folder.name
                        onApply(folder.name, selectedType, selectedMonth)
                    },
                    modifier = staggeredRevealModifier(
                        index = animIndex,
                        key = pageId + "folder_${folder.name}"
                    )
                )
            }
        }
    }
}

/**
 * 文件夹与相册微缩概览项。
 */
@Composable
private fun FolderItem(
    data: FilterFolderData,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) ReliveBlack.copy(alpha = 0.12f) else Color.Transparent)
            .bounceClick(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${data.name} (${data.count})",
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = ReliveBlack,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = stringResource(R.string.organize_selected),
                    tint = ReliveBlack,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val totalSlots = 7
            val previewsToShow = minOf(data.previews.size, 6)
            val hasOverflow = data.count > 6

            for (i in 0 until previewsToShow) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    val request = remember(data.previews[i].uri) {
                        ImageRequest.Builder(context)
                            .data(data.previews[i].uri)
                            .decoderFactory(VideoFrameDecoder.Factory())
                            .crossfade(true)
                            .size(200)
                            .build()
                    }
                    AsyncImage(
                        model = request,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (hasOverflow) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    if (data.previews.size > 6) {
                        val overflowRequest = remember(data.previews[6].uri) {
                            ImageRequest.Builder(context)
                                .data(data.previews[6].uri)
                                .decoderFactory(VideoFrameDecoder.Factory())
                                .crossfade(true)
                                .size(200)
                                .build()
                        }
                        AsyncImage(
                            model = overflowRequest,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ReliveBlack.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val remaining = data.count - 6
                        val fontSize = if (remaining.toString().length > 3) 10.sp else 12.sp
                        Text(
                            text = "+$remaining",
                            color = ReliveWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSize,
                            maxLines = 1,
                            overflow = TextOverflow.Visible,
                            softWrap = false
                        )
                    }
                }
            }

            val usedSlots = previewsToShow + (if (hasOverflow) 1 else 0)
            for (i in usedSlots until totalSlots) {
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                )
            }
        }
    }
}
