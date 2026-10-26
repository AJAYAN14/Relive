package com.jian.relive.core.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * 带有里程表/打字机效果的数字上下滚动文本组件。
 *
 * @param targetValue 目标数字。
 * @param style 排版字阶样式。
 * @param color 文本颜色。
 * @param modifier 外部修饰符。
 */
@Composable
fun SlideText(
    targetValue: Int,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    val countString = targetValue.toString()
    val oldString = remember(targetValue) { mutableStateOf(countString) }

    Row(modifier = modifier) {
        countString.forEachIndexed { index, char ->
            val oldChar = oldString.value.getOrNull(index) ?: ' '
            val direction = if (char > oldChar) 1 else -1

            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    slideInVertically { height -> height * direction } togetherWith
                        slideOutVertically { height -> -height * direction }
                },
                label = "slide_text_char"
            ) { targetChar ->
                Text(
                    text = targetChar.toString(),
                    style = style,
                    color = color
                )
            }
        }
    }
}
