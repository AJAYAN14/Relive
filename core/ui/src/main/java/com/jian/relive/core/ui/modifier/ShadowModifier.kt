package com.jian.relive.core.ui.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

/**
 * 通用阴影修饰符扩展。
 *
 * @param elevation 阴影高度。
 * @param shape 阴影轮廓形状。
 */
fun Modifier.shadow(
    elevation: Dp,
    shape: Shape
): Modifier = this.graphicsLayer {
    this.shadowElevation = elevation.toPx()
    this.shape = shape
    this.clip = false
}
