package com.jian.relive.core.ui.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * 带有开启生命周期重置 Key 的页面可见性过渡包装组件。
 *
 * @param visible 是否可见。
 * @param enter 入场过渡动画。
 * @param exit 离场过渡动画。
 * @param modifier 外部修饰符。
 * @param content 内容作用域。
 */
@Composable
fun PageAnimatedVisibility(
    visible: Boolean,
    enter: EnterTransition,
    exit: ExitTransition,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var openKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(visible) {
        if (visible) {
            openKey++
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = enter,
        exit = exit,
        modifier = modifier
    ) {
        key(openKey) {
            content()
        }
    }
}
