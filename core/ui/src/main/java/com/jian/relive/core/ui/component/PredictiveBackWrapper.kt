package com.jian.relive.core.ui.component

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.flow.collectLatest

/**
 * 响应系统预测性返回手势的页面包装组件，平滑驱动缩放、圆角与位移反馈。
 *
 * @param isVisible 当前内容是否可见。
 * @param onBack 当手势滑满并提交时的回退回调。
 * @param content 内容作用域。
 */
@Composable
fun PredictiveBackWrapper(
    isVisible: Boolean,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var isBackGestureActive by remember { mutableStateOf(false) }

    PredictiveBackHandler(enabled = isVisible) { progressFlow ->
        isBackGestureActive = true
        try {
            progressFlow.collectLatest { event ->
                progress = event.progress
            }
            onBack()
        } catch (_: Exception) {
            // 手势中途取消
        } finally {
            isBackGestureActive = false
            progress = 0f
        }
    }

    val scale = if (isBackGestureActive) {
        lerp(1f, 0.9f, progress)
    } else {
        1f
    }

    val translationX = if (isBackGestureActive) {
        lerp(0f, 48f, progress)
    } else {
        0f
    }

    val cornerRadius = if (isBackGestureActive) {
        lerp(0f, 32f, progress)
    } else {
        0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = 1f
                this.translationX = translationX
                this.shape = RoundedCornerShape(cornerRadius.dp)
                this.clip = true
            }
    ) {
        content()
    }
}
