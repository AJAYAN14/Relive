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
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.jian.relive.core.ui.sound.rememberSoundEffectHelper
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
    animationToken: Any? = null,
    onSwipeLeft: (MediaItem) -> Unit,
    onSwipeRight: (MediaItem) -> Unit,
    onSwipeUp: (MediaItem) -> Unit,
    onDetail: (MediaItem) -> Unit = {},
    onLongClick: (MediaItem) -> Unit = {},
    onClearUndoAnimation: () -> Unit = {},
    onDragOffsetChange: ((Offset) -> Unit)? = null
) {
    if (items.isEmpty()) return

    val visibleItems = items.take(3)
    val soundEffectHelper = rememberSoundEffectHelper()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val cardHeightPx = screenWidthPx * 0.85f * (14f / 9f)
    val entryOffscreenY = screenHeightPx + cardHeightPx + 120f

    val entryRotationSeeds = remember { listOf(-16f, 12f, -10f, 18f, -14f, 9f) }
    var isInitialLoad by rememberSaveable(animationToken) { mutableStateOf(true) }

    val cardEntryOffsets = remember(animationToken, entryOffscreenY) {
        List(6) { Animatable(if (isInitialLoad) entryOffscreenY else 0f) }
    }
    val cardEntryRotations = remember(animationToken) {
        entryRotationSeeds.map { Animatable(if (isInitialLoad) it else 0f) }
    }
    val entryStaggerMs = 120L

    LaunchedEffect(animationToken) {
        if (isInitialLoad && visibleItems.isNotEmpty()) {
            delay(280)
            soundEffectHelper.playStart()
            cardEntryOffsets.forEachIndexed { index, anim ->
                launch {
                    delay(index * entryStaggerMs)
                    anim.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.6f,
                            stiffness = 600f
                        )
                    )
                }
            }
            cardEntryRotations.forEachIndexed { index, anim ->
                launch {
                    delay(index * entryStaggerMs)
                    anim.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.7f,
                            stiffness = 380f
                        )
                    )
                }
            }
            delay(entryStaggerMs * visibleItems.size.coerceAtLeast(3) + 600)
            isInitialLoad = false
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = BiasAlignment(0f, -0.1f)
    ) {
        for (index in visibleItems.indices.reversed()) {
            val item = visibleItems[index]
            val isTopCard = index == 0
            val entranceTranslationY = if (isInitialLoad) cardEntryOffsets.getOrElse(index) { Animatable(0f) }.value else 0f
            val entranceRotation = if (isInitialLoad) cardEntryRotations.getOrElse(index) { Animatable(0f) }.value else 0f
            val isEntryAnimating = isInitialLoad && entranceTranslationY != 0f

            key(item.id) {
                Box(
                    modifier = Modifier.graphicsLayer {
                        translationY = entranceTranslationY
                        rotationZ = entranceRotation
                    }
                ) {
                    StackCardItem(
                        item = item,
                        stackIndex = index,
                        isTopCard = isTopCard,
                        isEntryAnimating = isEntryAnimating,
                        undoAnimationTarget = if (isTopCard) undoAnimationTarget else null,
                        onDragOffsetChange = if (isTopCard) onDragOffsetChange else null,
                        onSwipeLeft = {
                            onDragOffsetChange?.invoke(Offset.Zero)
                            onSwipeLeft(item)
                        },
                        onSwipeRight = {
                            onDragOffsetChange?.invoke(Offset.Zero)
                            onSwipeRight(item)
                        },
                        onSwipeUp = {
                            onDragOffsetChange?.invoke(Offset.Zero)
                            onSwipeUp(item)
                        },
                        onDetail = { onDetail(item) },
                        onLongClick = { onLongClick(item) },
                        onClearUndoAnimation = onClearUndoAnimation
                    )
                }
            }
        }
    }
}

@Composable
private fun StackCardItem(
    item: MediaItem,
    stackIndex: Int,
    isTopCard: Boolean,
    isEntryAnimating: Boolean = false,
    undoAnimationTarget: UndoAnimationTarget?,
    onDragOffsetChange: ((Offset) -> Unit)? = null,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeUp: () -> Unit,
    onDetail: () -> Unit,
    onLongClick: () -> Unit,
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
    val currentOnDragOffsetChange by rememberUpdatedState(onDragOffsetChange)

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
            .pointerInput(isTopCard, isExpanded, isEntryAnimating) {
                if (!isTopCard || isExpanded || isEntryAnimating) return@pointerInput
                val velocityTracker = VelocityTracker()

                detectDragGestures(
                    onDragStart = {
                        isPressed = true
                        velocityTracker.resetTracking()
                    },
                    onDragCancel = {
                        isPressed = false
                        currentOnDragOffsetChange?.invoke(Offset.Zero)
                        scope.launch {
                            launch { offsetX.animateTo(0f, spring(stiffness = 2000f, dampingRatio = 0.85f)) }
                            launch { offsetY.animateTo(0f, spring(stiffness = 2000f, dampingRatio = 0.85f)) }
                        }
                    },
                    onDragEnd = {
                        isPressed = false
                        currentOnDragOffsetChange?.invoke(Offset.Zero)
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
                            val newX = offsetX.value + dragAmount.x
                            val newY = offsetY.value + dragAmount.y
                            offsetX.snapTo(newX)
                            offsetY.snapTo(newY)
                            currentOnDragOffsetChange?.invoke(Offset(newX, newY))
                        }
                    }
                )
            }
            .pointerInput(isTopCard) {
                if (!isTopCard || isExpanded) return@pointerInput
                detectTapGestures(
                    onTap = { onDetail() },
                    onLongPress = { onLongClick() }
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

@Composable
internal fun BoxScope.EdgeGlowEffects(
    offset: Offset,
    isUndoAnimating: Boolean
) {
    if (offset == Offset.Zero || isUndoAnimating) return

    val threshold = 200f
    val dragX = offset.x
    val dragY = offset.y
    val absX = kotlin.math.abs(dragX)
    val absY = kotlin.math.abs(dragY)

    val isHorizontal = absX > absY
    val isVertical = absY > absX

    val maxOpacity = 0.6f
    val fadeScale = 2f
    val tX = (absX / (threshold * fadeScale)).coerceIn(0f, 1f)
    val easedX = tX * tX * (3f - 2f * tX)
    val tY = (absY / (threshold * fadeScale)).coerceIn(0f, 1f)
    val easedY = tY * tY * (3f - 2f * tY)

    if (isHorizontal && dragX > 0) {
        val opacity = easedX * maxOpacity
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.8f)
                .graphicsLayer { alpha = opacity }
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            ReliveOrange.copy(alpha = 0.9f)
                        )
                    )
                )
        )
    } else if (isHorizontal && dragX < 0) {
        val opacity = easedX * maxOpacity
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(0.8f)
                .graphicsLayer { alpha = opacity }
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            ReliveRed500.copy(alpha = 0.9f),
                            Color.Transparent
                        )
                    )
                )
        )
    } else if (isVertical && dragY < 0) {
        val opacity = easedY * maxOpacity
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
                .graphicsLayer { alpha = opacity }
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(ReliveBlue500, Color.Transparent)
                    )
                )
        )
    }
}
