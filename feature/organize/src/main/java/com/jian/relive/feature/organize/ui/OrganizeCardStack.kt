package com.jian.relive.feature.organize.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.relive.core.model.MediaItem
import com.jian.relive.core.model.SwipeDirection
import com.jian.relive.core.model.UndoAnimationTarget
import com.jian.relive.core.ui.composition.LocalHapticsEnabled
import com.jian.relive.core.ui.theme.ReliveBlue500
import com.jian.relive.core.ui.theme.ReliveOrange
import com.jian.relive.core.ui.theme.ReliveRed500
import com.jian.relive.core.ui.theme.ReliveWhite
import com.jian.relive.feature.organize.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 主整理流程的卡片堆栈组件。
 *
 * 管理前 3 层卡片物理叠放层级、手势滑动轨迹与弹性阻尼回弹，分发左滑/右滑/上滑动作并提供实时彩色状态徽标反馈。
 */
@Composable
fun OrganizeCardStack(
    items: List<MediaItem>,
    undoAnimationTarget: UndoAnimationTarget?,
    modifier: Modifier = Modifier,
    onSwipeLeft: (MediaItem) -> Unit,
    onSwipeRight: (MediaItem) -> Unit,
    onSwipeUp: (MediaItem) -> Unit,
    onDetail: (MediaItem) -> Unit = {},
    onClearUndoAnimation: () -> Unit = {}
) {
    if (items.isEmpty()) return

    val visibleItems = items.take(3)
    val topItem = visibleItems.first()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = BiasAlignment(0f, -0.1f)
    ) {
        // 从底往顶逆序渲染，保证顶层卡片盖在最上面
        for (index in visibleItems.indices.reversed()) {
            val item = visibleItems[index]
            val isTopCard = index == 0

            StackCardItem(
                item = item,
                stackIndex = index,
                isTopCard = isTopCard,
                undoAnimationTarget = if (isTopCard) undoAnimationTarget else null,
                onSwipeLeft = { onSwipeLeft(item) },
                onSwipeRight = { onSwipeRight(item) },
                onSwipeUp = { onSwipeUp(item) },
                onDetail = { onDetail(item) },
                onClearUndoAnimation = onClearUndoAnimation
            )
        }
    }
}

@Composable
private fun StackCardItem(
    item: MediaItem,
    stackIndex: Int,
    isTopCard: Boolean,
    undoAnimationTarget: UndoAnimationTarget?,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeUp: () -> Unit,
    onDetail: () -> Unit,
    onClearUndoAnimation: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val alphaAnim = remember { Animatable(1f) }
    val exitScale = remember { Animatable(1f) }

    var isPressed by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = 800f, dampingRatio = 0.75f),
        label = "press_scale"
    )

    val currentOnSwipeLeft by rememberUpdatedState(onSwipeLeft)
    val currentOnSwipeRight by rememberUpdatedState(onSwipeRight)
    val currentOnSwipeUp by rememberUpdatedState(onSwipeUp)
    val currentOnClearUndoAnimation by rememberUpdatedState(onClearUndoAnimation)

    val effectiveStackIndex = stackIndex.coerceAtMost(2)
    val targetScale = 1f - (effectiveStackIndex * 0.04f)
    val stackScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(stiffness = 350f, dampingRatio = 0.75f),
        label = "stack_scale"
    )

    val targetYOffset = (effectiveStackIndex * 15).dp
    val stackYOffset by animateDpAsState(
        targetValue = targetYOffset,
        animationSpec = spring(stiffness = 350f, dampingRatio = 0.75f),
        label = "stack_y_offset"
    )

    // 监听撤销飞入回弹
    LaunchedEffect(undoAnimationTarget?.token) {
        val target = undoAnimationTarget ?: return@LaunchedEffect
        if (!isTopCard || target.itemId != item.id) return@LaunchedEffect

        val startX = when (target.direction) {
            SwipeDirection.Left -> -screenWidthPx * 1.2f
            SwipeDirection.Right -> screenWidthPx * 1.2f
            else -> 0f
        }
        val startY = when (target.direction) {
            SwipeDirection.Up -> -screenHeightPx * 1.2f
            SwipeDirection.Down -> screenHeightPx * 1.2f
            else -> 0f
        }

        offsetX.snapTo(startX)
        offsetY.snapTo(startY)
        alphaAnim.snapTo(0f)
        exitScale.snapTo(0.96f)

        launch { offsetX.animateTo(0f, tween(260, easing = FastOutSlowInEasing)) }
        launch { offsetY.animateTo(0f, tween(260, easing = FastOutSlowInEasing)) }
        launch { alphaAnim.animateTo(1f, tween(180)) }
        launch { exitScale.animateTo(1f, tween(220, easing = FastOutSlowInEasing)) }

        delay(280)
        currentOnClearUndoAnimation()
    }

    val dynamicScale = if (offsetY.value < 0f) {
        (1f + (offsetY.value / 1200f)).coerceAtLeast(0.92f)
    } else {
        1f
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.value.roundToInt(), (offsetY.value + stackYOffset.toPx()).roundToInt()) }
            .scale(stackScale * dynamicScale * exitScale.value * pressScale)
            .graphicsLayer { alpha = alphaAnim.value }
            .fillMaxWidth(0.85f)
            .fillMaxHeight(0.62f)
            .shadow(
                elevation = if (stackIndex > 2) 0.dp else 12.dp,
                shape = RoundedCornerShape(24.dp)
            )
            .clip(RoundedCornerShape(24.dp))
            .pointerInput(isTopCard, isExpanded) {
                if (!isTopCard || isExpanded) return@pointerInput
                val velocityTracker = VelocityTracker()

                detectDragGestures(
                    onDragStart = {
                        isPressed = true
                        velocityTracker.resetTracking()
                    },
                    onDragCancel = {
                        isPressed = false
                        scope.launch {
                            launch { offsetX.animateTo(0f, spring(stiffness = 2000f, dampingRatio = 0.85f)) }
                            launch { offsetY.animateTo(0f, spring(stiffness = 2000f, dampingRatio = 0.85f)) }
                        }
                    },
                    onDragEnd = {
                        isPressed = false
                        val velocity = velocityTracker.calculateVelocity()
                        val velocityY = velocity.y
                        val threshold = 160f

                        scope.launch {
                            when {
                                offsetX.value < -threshold -> {
                                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    offsetX.animateTo(-1600f, tween(200, easing = FastOutLinearInEasing))
                                    currentOnSwipeLeft()
                                }

                                offsetX.value > threshold -> {
                                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    offsetX.animateTo(1600f, tween(200, easing = FastOutLinearInEasing))
                                    currentOnSwipeRight()
                                }

                                offsetY.value < -threshold || (velocityY < -800f && offsetY.value < 0f) -> {
                                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    offsetY.animateTo(-2200f, tween(200, easing = FastOutLinearInEasing))
                                    currentOnSwipeUp()
                                }

                                else -> {
                                    launch { offsetX.animateTo(0f, spring(stiffness = 2000f, dampingRatio = 0.85f)) }
                                    launch { offsetY.animateTo(0f, spring(stiffness = 2000f, dampingRatio = 0.85f)) }
                                }
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo(offsetY.value + dragAmount.y)
                        }
                    }
                )
            }
            .pointerInput(isTopCard) {
                if (!isTopCard || isExpanded) return@pointerInput
                detectTapGestures(
                    onTap = { onDetail() }
                )
            }
    ) {
        PhotoCard(
            item = item,
            isTopCard = isTopCard,
            stackIndex = stackIndex,
            isExpanded = isExpanded,
            onToggleExpand = { isExpanded = !isExpanded },
            modifier = Modifier.fillMaxSize()
        )

        // 滑动状态微标指示器
        if (isTopCard && !isExpanded) {
            when {
                offsetX.value > 100f -> {
                    // 右滑：喜欢 (LIKE)
                    ActionBadge(
                        text = stringResource(R.string.organize_like),
                        icon = Icons.Filled.Favorite,
                        tintColor = ReliveOrange,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 24.dp)
                    )
                }

                offsetX.value < -100f -> {
                    // 左滑：删除 (TRASH)
                    ActionBadge(
                        text = stringResource(R.string.organize_trash),
                        icon = Icons.Outlined.Delete,
                        tintColor = ReliveRed500,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 24.dp)
                    )
                }

                offsetY.value < -100f -> {
                    // 上滑：保留 (KEEP)
                    ActionBadge(
                        text = stringResource(R.string.organize_keep),
                        icon = Icons.Outlined.ArrowUpward,
                        tintColor = ReliveBlue500,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionBadge(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tintColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                ReliveWhite.copy(alpha = 0.92f),
                RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
            )
            .border(
                2.dp,
                tintColor,
                RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                fontSize = 15.sp,
                color = tintColor,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
