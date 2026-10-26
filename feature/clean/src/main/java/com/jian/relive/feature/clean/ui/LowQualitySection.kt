package com.jian.relive.feature.clean.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.relive.core.ui.animation.bounceClick
import com.jian.relive.core.ui.composition.LocalHapticsEnabled
import com.jian.relive.core.ui.theme.ReliveBlack
import com.jian.relive.core.ui.theme.ReliveBlue500
import com.jian.relive.core.ui.theme.ReliveGray100
import com.jian.relive.core.ui.theme.ReliveGray200
import com.jian.relive.core.ui.theme.ReliveGray600
import com.jian.relive.core.ui.theme.ReliveRed200
import com.jian.relive.core.ui.theme.ReliveRed50
import com.jian.relive.core.ui.theme.ReliveRed500
import com.jian.relive.core.ui.theme.ReliveRed600
import com.jian.relive.core.ui.theme.ReliveWhite
import com.jian.relive.feature.clean.CleanViewModel
import com.jian.relive.feature.clean.R
import com.jian.relive.feature.clean.model.LowQualityItem
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 低质照片物理卡片堆栈式审查与清理页面。
 */
@Composable
fun LowQualitySection(
    viewModel: CleanViewModel,
    lowQualityItems: List<LowQualityItem>,
    pendingTrashCount: Int,
    canUndo: Boolean,
    isScanning: Boolean,
    scanProgress: Float,
    modifier: Modifier = Modifier
) {
    if (lowQualityItems.isEmpty()) {
        if (isScanning) {
            CleanScanningWaitView(
                progress = scanProgress,
                onStopClick = { viewModel.stopScan() },
                modifier = modifier
            )
        } else {
            CleanEmptyStateView(
                title = stringResource(R.string.clean_no_low_quality_found),
                description = stringResource(R.string.clean_low_quality_done_description),
                onDoneClick = { viewModel.loadAndStartScan() },
                modifier = modifier
            )
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ReliveGray100)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.clean_tab_low_quality),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ReliveBlack
                    )
                    Text(
                        text = stringResource(R.string.clean_items_count, lowQualityItems.size),
                        fontSize = 13.sp,
                        color = ReliveGray600
                    )
                }

                if (pendingTrashCount > 0) {
                    Button(
                        onClick = { viewModel.setConfirmingDelete(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = ReliveRed50),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, ReliveRed200),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.bounceClick(onClick = { viewModel.setConfirmingDelete(true) })
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = ReliveRed600,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.clean_pending_trash_count, pendingTrashCount),
                            color = ReliveRed600,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (isScanning) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { scanProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape),
                    color = ReliveBlack,
                    trackColor = ReliveGray200
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = BiasAlignment(0f, -0.05f)
        ) {
            val visibleItems = lowQualityItems.take(3).reversed()

            visibleItems.forEachIndexed { index, item ->
                val isTopCard = index == visibleItems.lastIndex
                val stackIndex = visibleItems.lastIndex - index

                key(item.item.id) {
                    DraggableLowQualityCard(
                        item = item,
                        isTopCard = isTopCard,
                        stackIndex = stackIndex,
                        onSwipeLeft = { viewModel.onLowQualitySwipe(item, wasDeleted = true) },
                        onSwipeRight = { viewModel.onLowQualitySwipe(item, wasDeleted = false) },
                        onSwipeUp = { viewModel.onLowQualitySwipe(item, wasDeleted = false) }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = canUndo,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                Button(
                    onClick = { viewModel.undoLastAction() },
                    colors = ButtonDefaults.buttonColors(containerColor = ReliveWhite),
                    shape = RoundedCornerShape(24.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                    border = BorderStroke(1.dp, ReliveGray200),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                    modifier = Modifier.bounceClick(onClick = { viewModel.undoLastAction() })
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Replay,
                        contentDescription = stringResource(R.string.clean_undo),
                        tint = ReliveBlack,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.clean_undo),
                        color = ReliveBlack,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun DraggableLowQualityCard(
    item: LowQualityItem,
    isTopCard: Boolean,
    stackIndex: Int,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeUp: () -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val scale by animateFloatAsState(targetValue = 1f - (stackIndex * 0.04f))
    val yOffset by animateDpAsState(targetValue = (stackIndex * 14).dp)
    val rotation = (offsetX.value / 60f).coerceIn(-10f, 10f)

    val swipeUpScale = if (offsetY.value < 0) {
        (1f + (offsetY.value / 1000f)).coerceAtLeast(0.9f)
    } else {
        1f
    }

    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val threshold = 160f

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.value.roundToInt(), yOffset.roundToPx() + offsetY.value.roundToInt()) }
            .scale(scale * swipeUpScale)
            .rotate(rotation)
            .fillMaxWidth(0.85f)
            .aspectRatio(9f / 14f)
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(ReliveWhite)
            .pointerInput(isTopCard) {
                if (!isTopCard) return@pointerInput

                detectDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (offsetX.value < -threshold) {
                                if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                offsetX.animateTo(-1500f, tween(300, easing = FastOutLinearInEasing))
                                onSwipeLeft()
                            } else if (offsetX.value > threshold) {
                                if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                offsetX.animateTo(1500f, tween(300, easing = FastOutLinearInEasing))
                                onSwipeRight()
                            } else if (offsetY.value < -threshold) {
                                if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                offsetY.animateTo(-2000f, tween(300, easing = FastOutLinearInEasing))
                                onSwipeUp()
                            } else {
                                launch {
                                    offsetX.animateTo(
                                        0f,
                                        spring(
                                            stiffness = Spring.StiffnessMedium,
                                            dampingRatio = Spring.DampingRatioLowBouncy
                                        )
                                    )
                                }
                                launch {
                                    offsetY.animateTo(
                                        0f,
                                        spring(
                                            stiffness = Spring.StiffnessMedium,
                                            dampingRatio = Spring.DampingRatioLowBouncy
                                        )
                                    )
                                }
                            }
                        }
                    },
                    onDrag = { change: PointerInputChange, dragAmount: Offset ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo(offsetY.value + dragAmount.y)
                        }
                    }
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ReliveRed50)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(item.reason.labelRes),
                    color = ReliveRed600,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                CleanMediaThumbnail(
                    item = item.item,
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (offsetX.value < -50) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 20.dp, top = 16.dp)
                            .background(ReliveWhite, RoundedCornerShape(8.dp))
                            .border(2.dp, ReliveRed500, RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.clean_delete),
                            fontSize = 14.sp,
                            color = ReliveRed500,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (offsetX.value > 50 || offsetY.value < -50) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 20.dp, top = 16.dp)
                            .background(ReliveWhite, RoundedCornerShape(8.dp))
                            .border(2.dp, ReliveBlue500, RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.clean_skip),
                            fontSize = 14.sp,
                            color = ReliveBlue500,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
