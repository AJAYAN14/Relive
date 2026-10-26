package com.jian.relive.feature.organize.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.relive.core.model.MediaItem
import com.jian.relive.core.model.SwipeDirection
import com.jian.relive.core.model.UndoAction
import com.jian.relive.core.model.UndoAnimationTarget
import com.jian.relive.core.ui.animation.bounceClick
import com.jian.relive.core.ui.animation.staggeredRevealModifier
import com.jian.relive.core.ui.theme.ReliveBlack
import com.jian.relive.core.ui.theme.ReliveGray100
import com.jian.relive.core.ui.theme.ReliveGreen500
import com.jian.relive.core.ui.theme.ReliveMonthCream
import com.jian.relive.core.ui.theme.ReliveMonthDeepBlue
import com.jian.relive.core.ui.theme.ReliveMonthIndigo
import com.jian.relive.core.ui.theme.ReliveMonthOlive
import com.jian.relive.core.ui.theme.ReliveMonthPurple
import com.jian.relive.core.ui.theme.ReliveParticleBlue
import com.jian.relive.core.ui.theme.ReliveParticleGold
import com.jian.relive.core.ui.theme.ReliveParticleGreen
import com.jian.relive.core.ui.theme.ReliveParticleLightBlue
import com.jian.relive.core.ui.theme.ReliveParticleOrange
import com.jian.relive.core.ui.theme.ReliveParticleRed
import com.jian.relive.core.ui.theme.ReliveParticleTeal
import com.jian.relive.core.ui.theme.ReliveRed200
import com.jian.relive.core.ui.theme.ReliveRed50
import com.jian.relive.core.ui.theme.ReliveRed600
import com.jian.relive.core.ui.theme.ReliveReviewBackground
import com.jian.relive.core.ui.theme.ReliveWhite
import com.jian.relive.feature.organize.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.Month
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

/**
 * 月度相册聚合网格列表组件。
 *
 * 按年月分组汇聚媒体数据，提供年份粘性吸顶标题、12 阶杂志色块卡片、整理进度指示及月份快速跳转穿梭。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MonthlyListView(
    allMedia: List<MediaItem>,
    processedIds: Set<Long>,
    currentMonth: String = "All",
    onMonthClick: (String, List<MediaItem>) -> Unit,
    onAllClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val groupedMedia by produceState<Map<String, List<MediaItem>>>(
        initialValue = emptyMap(),
        key1 = allMedia
    ) {
        value = withContext(Dispatchers.Default) {
            val format = SimpleDateFormat("yyyy-MM", Locale.US)
            allMedia.groupBy {
                format.format(Date(it.dateAdded * 1000))
            }.toSortedMap(compareByDescending { it })
        }
    }

    val monthColors = remember {
        listOf(
            ReliveParticleGold,
            ReliveParticleRed,
            ReliveParticleTeal,
            ReliveParticleBlue,
            ReliveParticleGreen,
            ReliveParticleOrange,
            ReliveParticleLightBlue,
            ReliveMonthPurple,
            ReliveMonthIndigo,
            ReliveMonthDeepBlue,
            ReliveMonthCream,
            ReliveMonthOlive
        )
    }

    val groupedByYear = remember(groupedMedia) {
        groupedMedia.entries
            .filter { it.value.isNotEmpty() }
            .groupBy { entry ->
                entry.key.substringBefore("-")
            }
    }

    val monthColorMap = remember(groupedMedia, processedIds) {
        val map = mutableMapOf<String, Color>()
        var colorIndex = 0
        groupedMedia.forEach { (key, items) ->
            if (items.isNotEmpty()) {
                val total = items.size
                val processedCount = items.count { processedIds.contains(it.id) }
                val isDone = total > 0 && processedCount == total

                if (isDone) {
                    map[key] = ReliveGray100
                } else {
                    map[key] = monthColors[colorIndex % monthColors.size]
                    colorIndex++
                }
            }
        }
        map
    }

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
                    .bounceClick(onClick = onBack)
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
                text = stringResource(R.string.organize_monthly_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ReliveBlack,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                val isSelected = currentMonth.equals("All", ignoreCase = true)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(staggeredRevealModifier(1))
                        .height(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(ReliveGray100)
                        .clickable { onAllClick() }
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.organize_filter_all),
                        fontSize = 24.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = ReliveBlack
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = stringResource(R.string.organize_selected),
                            tint = ReliveBlack,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            groupedByYear.forEach { (year, months) ->
                stickyHeader {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ReliveWhite)
                            .padding(top = 32.dp, bottom = 16.dp)
                    ) {
                        Text(
                            text = year,
                            fontSize = 60.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = ReliveBlack
                        )
                    }
                }

                itemsIndexed(months) { index, (key, items) ->
                    val monthNumber = key.substringAfter("-").toIntOrNull() ?: 1
                    val localizedMonthName = runCatching {
                        Month.of(monthNumber).getDisplayName(TextStyle.FULL, Locale.getDefault())
                    }.getOrDefault(key)

                    val total = items.size
                    val processedCount = items.count { processedIds.contains(it.id) }
                    val isDone = total > 0 && processedCount == total
                    val cardColor = monthColorMap[key] ?: ReliveGray100
                    val isSelected = key.equals(currentMonth, ignoreCase = true)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(staggeredRevealModifier(2 + (index % 4)))
                            .height(110.dp)
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(cardColor)
                            .bounceClick(onClick = { onMonthClick(key, items) })
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = localizedMonthName,
                            fontSize = 32.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = if (isDone && !isSelected) ReliveBlack.copy(alpha = 0.3f) else ReliveBlack
                        )

                        if (isSelected) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = stringResource(R.string.organize_selected),
                                tint = ReliveBlack,
                                modifier = Modifier.size(28.dp)
                            )
                        } else if (isDone) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = stringResource(R.string.organize_done),
                                tint = ReliveGreen500,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$processedCount/$total",
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = ReliveBlack.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

/**
 * 月度整理专场复查页面。
 *
 * 针对所选特定月份进行相片滑动整理、废纸篓标记、即时撤销回滚与整理完成庆祝展示。
 */
@Composable
fun MonthlyReviewView(
    monthItems: List<MediaItem>,
    processedIds: Set<Long>,
    pendingTrashCount: Int,
    muteVideos: Boolean,
    onTrashClick: () -> Unit,
    onSwipe: (MediaItem, Boolean, Boolean) -> Unit,
    onUndo: (List<UndoAction>, (List<UndoAction>) -> Unit, (MediaItem) -> Unit) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var queue by remember(monthItems, processedIds) {
        mutableStateOf(monthItems.filter { !processedIds.contains(it.id) })
    }
    var undoStack by remember { mutableStateOf(listOf<UndoAction>()) }
    var currentUndoTarget by remember { mutableStateOf<UndoAnimationTarget?>(null) }
    val isDone = queue.isEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ReliveReviewBackground)
    ) {
        if (isDone) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = stringResource(R.string.organize_done),
                    tint = ReliveGreen500,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.organize_monthly_done_title),
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = ReliveBlack
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = ReliveBlack),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.organize_monthly_done_button),
                        color = ReliveWhite
                    )
                }
            }
        } else {
            OrganizeCardStack(
                items = queue,
                undoAnimationTarget = currentUndoTarget,
                modifier = Modifier.fillMaxSize(),
                onSwipeLeft = { item ->
                    onSwipe(item, true, false)
                    undoStack = undoStack + UndoAction.Trash(item)
                    queue = queue.drop(1)
                },
                onSwipeRight = { item ->
                    onSwipe(item, false, true)
                    undoStack = undoStack + UndoAction.Like(item)
                    queue = queue.drop(1)
                },
                onSwipeUp = { item ->
                    onSwipe(item, false, false)
                    undoStack = undoStack + UndoAction.Keep(item)
                    queue = queue.drop(1)
                },
                onClearUndoAnimation = {
                    currentUndoTarget = null
                }
            )

            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 48.dp, start = 16.dp)
                    .background(ReliveBlack.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.organize_back),
                    tint = ReliveWhite
                )
            }

            Text(
                text = stringResource(R.string.organize_monthly_remaining, queue.size),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp),
                color = ReliveBlack.copy(alpha = 0.5f),
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )

            AnimatedVisibility(
                visible = undoStack.isNotEmpty(),
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 60.dp)
            ) {
                Button(
                    onClick = {
                        val lastAction = undoStack.lastOrNull() ?: return@Button
                        currentUndoTarget = UndoAnimationTarget(
                            itemId = lastAction.item.id,
                            direction = lastAction.direction,
                            token = System.currentTimeMillis()
                        )
                        onUndo(
                            undoStack,
                            { updated -> undoStack = updated },
                            { restoredItem -> queue = listOf(restoredItem) + queue }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ReliveWhite.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(30.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, ReliveBlack.copy(alpha = 0.1f)),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Replay,
                        contentDescription = stringResource(R.string.organize_undo),
                        tint = ReliveBlack.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.organize_undo),
                        color = ReliveBlack.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (pendingTrashCount > 0) {
                Button(
                    onClick = onTrashClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 48.dp, end = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ReliveRed50),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, ReliveRed200),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.organize_trash_clean),
                        tint = ReliveRed600,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = pendingTrashCount.toString(),
                        color = ReliveRed600,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
