package com.jian.relive.core.ui.animation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

/**
 * 卡片或列表项错落入场动画修饰符。
 *
 * @param index 当前组件在列表中的索引位置。
 * @param key 外部控制动画重置的键值。
 * @param baseDelayMs 每一级项递增的基础延时毫秒数。
 * @param durationMs 动画持续时长。
 * @param offsetY 初始垂直偏移量。
 * @param skipAnimation 是否跳过入场动画。
 */
@Composable
fun staggeredRevealModifier(
    index: Int,
    key: Any? = null,
    baseDelayMs: Int = 50,
    durationMs: Int = 400,
    offsetY: Float = 100f,
    skipAnimation: Boolean = false
): Modifier {
    if (skipAnimation) {
        return Modifier
    }

    val actualKey = key ?: index
    var isVisible by remember(actualKey) { mutableStateOf(false) }

    LaunchedEffect(actualKey) {
        isVisible = false
        delay((index * baseDelayMs).toLong())
        isVisible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = (durationMs * 0.8).toInt(), easing = FastOutSlowInEasing),
        label = "staggered_alpha"
    )

    val offset by animateFloatAsState(
        targetValue = if (isVisible) 0f else offsetY,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = 0.8f
        ),
        label = "staggered_offset"
    )

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.9f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = 0.7f
        ),
        label = "staggered_scale"
    )

    return Modifier.graphicsLayer {
        this.alpha = alpha
        this.translationY = offset
        this.scaleX = scale
        this.scaleY = scale
    }
}
