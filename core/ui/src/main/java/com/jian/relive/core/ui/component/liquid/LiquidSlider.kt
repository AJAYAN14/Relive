package com.jian.relive.core.ui.component.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.jian.relive.core.ui.component.liquid.utils.DampedDragAnimation
import com.jian.relive.core.ui.theme.LocalDarkTheme
import com.jian.relive.core.ui.theme.ReliveBlack
import com.jian.relive.core.ui.theme.RelivePrimary
import com.jian.relive.core.ui.theme.RelivePrimaryLight
import com.jian.relive.core.ui.theme.ReliveWhite
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

/**
 * 通用液态玻璃滑动条组件（标准值参数重载）。
 *
 * @param value 当前滑动条数值。
 * @param onValueChange 数值改变时的回调。
 * @param modifier 外部布局修饰符。
 * @param enabled 是否启用交互。
 * @param valueRange 数值取值范围，默认为 0f..1f。
 * @param steps 离散步长数量，0 表示连续滑动。
 * @param backdrop 外部提供的背板渲染上下文，若为 null 则自动构建画布底板。
 * @param accentColor 滑动条激活轨道与按压强调色，未指定时采用静态主题色。
 */
@Composable
fun LiquidSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    backdrop: Backdrop? = null,
    accentColor: Color = Color.Unspecified
) {
    LiquidSlider(
        value = { value },
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        backdrop = backdrop,
        accentColor = accentColor
    )
}

/**
 * 通用液态玻璃滑动条组件（Lambda 派生状态重载）。
 *
 * @param value 当前滑动条数值获取函数。
 * @param onValueChange 数值改变时的回调。
 * @param modifier 外部布局修饰符。
 * @param enabled 是否启用交互。
 * @param valueRange 数值取值范围，默认为 0f..1f。
 * @param steps 离散步长数量，0 表示连续滑动。
 * @param backdrop 外部提供的背板渲染上下文，若为 null 则自动构建画布底板。
 * @param accentColor 滑动条激活轨道与按压强调色，未指定时采用静态主题色。
 */
@Composable
fun LiquidSlider(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    backdrop: Backdrop? = null,
    accentColor: Color = Color.Unspecified
) {
    val isDarkTheme = LocalDarkTheme.current
    val resolvedAccentColor = if (accentColor != Color.Unspecified) {
        accentColor
    } else {
        if (!isDarkTheme) RelivePrimary else RelivePrimaryLight
    }
    val trackColor = if (!isDarkTheme) {
        Color(0xFF787880).copy(alpha = 0.2f)
    } else {
        Color(0xFF787880).copy(alpha = 0.36f)
    }

    val effectiveBackdrop = backdrop ?: rememberCanvasBackdrop {
        drawRect(if (isDarkTheme) ReliveBlack else ReliveWhite)
    }

    val trackBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = if (enabled) 1f else 0.38f
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidth = constraints.maxWidth
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var didDrag by remember { mutableStateOf(false) }

        val rangeSpan = valueRange.endInclusive - valueRange.start
        val visibilityThreshold = if (steps > 0) rangeSpan / (steps + 1) else 0.001f

        fun snapValue(raw: Float): Float {
            val clamped = raw.coerceIn(valueRange)
            if (steps <= 0) return clamped
            val stepSize = rangeSpan / (steps + 1)
            val stepIndex = ((clamped - valueRange.start) / stepSize).roundToInt()
            return (valueRange.start + stepIndex * stepSize).coerceIn(valueRange)
        }

        val dampedDragAnimation = remember(animationScope, enabled, valueRange, steps) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = snapValue(value()),
                valueRange = valueRange,
                visibilityThreshold = visibilityThreshold,
                initialScale = 1f,
                pressedScale = 1.5f,
                onDragStarted = {},
                onDragStopped = {
                    if (!enabled) return@DampedDragAnimation
                    if (didDrag) {
                        onValueChange(snapValue(targetValue))
                    }
                },
                onDrag = { _, dragAmount ->
                    if (!enabled) return@DampedDragAnimation
                    if (!didDrag) {
                        didDrag = dragAmount.x != 0f
                    }
                    val delta = rangeSpan * (dragAmount.x / trackWidth)
                    val rawTarget = if (isLtr) targetValue + delta else targetValue - delta
                    onValueChange(snapValue(rawTarget))
                }
            )
        }

        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { value() }
                .collectLatest { currentValue ->
                    val snapped = snapValue(currentValue)
                    if (dampedDragAnimation.targetValue != snapped) {
                        dampedDragAnimation.updateValue(snapped)
                    }
                }
        }

        Box(Modifier.layerBackdrop(trackBackdrop)) {
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(trackColor)
                    .pointerInput(animationScope, enabled) {
                        if (!enabled) return@pointerInput
                        detectTapGestures { position ->
                            val delta = rangeSpan * (position.x / trackWidth)
                            val rawTarget = if (isLtr) valueRange.start + delta else valueRange.endInclusive - delta
                            val snappedTarget = snapValue(rawTarget)
                            dampedDragAnimation.animateToValue(snappedTarget)
                            onValueChange(snappedTarget)
                        }
                    }
                    .height(6f.dp)
                    .fillMaxWidth()
            )

            Box(
                Modifier
                    .clip(CircleShape)
                    .background(resolvedAccentColor)
                    .height(6f.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = (constraints.maxWidth * dampedDragAnimation.progress).fastRoundToInt()
                        layout(width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    }
            )
        }

        Box(
            Modifier
                .graphicsLayer {
                    translationX =
                        (-size.width / 2f + trackWidth * dampedDragAnimation.progress)
                            .fastCoerceIn(-size.width / 4f, trackWidth - size.width * 3f / 4f) * if (isLtr) 1f else -1f
                }
                .then(if (enabled) dampedDragAnimation.modifier else Modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        effectiveBackdrop,
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            val progress = dampedDragAnimation.pressProgress
                            val scaleX = lerp(2f / 3f, 1f, progress)
                            val scaleY = lerp(0f, 1f, progress)
                            scale(scaleX, scaleY) {
                                drawBackdrop()
                            }
                        }
                    ),
                    shape = { CircleShape },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        blur(8f.dp.toPx() * (1f - progress))
                        lens(
                            10f.dp.toPx() * progress,
                            14f.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = progress
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 4f.dp,
                            color = Color.Black.copy(alpha = 0.05f)
                        )
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 4f.dp * progress,
                            alpha = progress
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(Color.White.copy(alpha = 1f - progress))
                    }
                )
                .size(40f.dp, 24f.dp)
        )
    }
}
