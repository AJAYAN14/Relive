package com.jian.relive.core.model

/**
 * 撤销动画目标参数。
 *
 * @property itemId 目标媒体唯一标识。
 * @property direction 飞出或返回的滑动手势方向。
 * @property token 动画时序标记令牌。
 */
data class UndoAnimationTarget(
    val itemId: Long,
    val direction: SwipeDirection,
    val token: Long
)
