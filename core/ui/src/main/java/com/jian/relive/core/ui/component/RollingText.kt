package com.jian.relive.core.ui.component

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import com.jian.relive.core.ui.composition.LocalHapticsEnabled

/**
 * 从 0 平滑翻滚至目标数值的动态数字文本组件，支持节流触觉震动反馈。
 *
 * @param targetValue 目标数值。
 * @param format 格式化函数。
 * @param style 排版字阶样式。
 * @param color 文本颜色。
 * @param modifier 外部修饰符。
 * @param durationMillis 动画持续毫秒数。
 * @param shouldAnimate 是否执行入场翻滚动画。
 * @param stopAnimation 是否立即终止动画并跳至终态。
 * @param enableHaptics 是否允许伴随触觉震动。
 */
@Composable
fun RollingText(
    targetValue: Number,
    format: (Number) -> String = { it.toString() },
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    durationMillis: Int = 1500,
    shouldAnimate: Boolean = true,
    stopAnimation: Boolean = false,
    enableHaptics: Boolean = true
) {
    val view = LocalView.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val animatedValue = remember { Animatable(if (shouldAnimate) 0f else targetValue.toFloat()) }
    val targetFloat = targetValue.toFloat()

    val dynamicDuration = remember(targetFloat, durationMillis) {
        if (targetFloat < 50) {
            (targetFloat * 20).toInt().coerceIn(300, 1000)
        } else {
            durationMillis
        }
    }

    LaunchedEffect(targetFloat, shouldAnimate) {
        if (shouldAnimate) {
            animatedValue.snapTo(0f)
            animatedValue.animateTo(
                targetValue = targetFloat,
                animationSpec = tween(durationMillis = dynamicDuration, easing = FastOutSlowInEasing)
            )
        } else {
            animatedValue.snapTo(targetFloat)
        }
    }

    LaunchedEffect(stopAnimation) {
        if (stopAnimation) {
            animatedValue.snapTo(targetFloat)
        }
    }

    var lastHapticTime by remember { mutableLongStateOf(0L) }
    val currentFloat = animatedValue.value
    val isAnimating = currentFloat != targetFloat && !stopAnimation && enableHaptics && hapticsAllowed

    LaunchedEffect(currentFloat) {
        if (isAnimating) {
            val now = System.currentTimeMillis()
            if (now - lastHapticTime >= 60) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                lastHapticTime = now
            }
        }
    }

    Text(
        text = format(currentFloat),
        style = style,
        color = color,
        modifier = modifier
    )
}
