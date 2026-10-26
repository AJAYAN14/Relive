package com.jian.relive.core.ui.animation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.unit.IntOffset

/**
 * 基于弹簧物理与平滑过渡的页面出入场动画集合。
 */
object FluidTransitions {

    private val SpringStiff = spring<Float>(stiffness = Spring.StiffnessMedium)
    private val SpringBouncy = spring<Float>(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.75f)
    private val SpringGentle = spring<Float>(stiffness = Spring.StiffnessLow)
    private val SpringOffset = spring<IntOffset>(stiffness = Spring.StiffnessMediumLow)

    val SheetEnter: EnterTransition =
        slideInVertically(
            initialOffsetY = { it },
            animationSpec = SpringOffset
        ) + fadeIn(
            animationSpec = tween(300)
        ) + scaleIn(
            initialScale = 0.92f,
            animationSpec = SpringBouncy
        )

    val SheetExit: ExitTransition =
        slideOutVertically(
            targetOffsetY = { it },
            animationSpec = SpringOffset
        ) + fadeOut(
            animationSpec = tween(300)
        ) + scaleOut(
            targetScale = 0.92f,
            animationSpec = SpringStiff
        )

    val ParallaxPushEnter: EnterTransition =
        slideInHorizontally(
            initialOffsetX = { (it * 1.0).toInt() },
            animationSpec = SpringOffset
        ) + fadeIn(
            animationSpec = tween(200)
        ) + scaleIn(
            initialScale = 0.96f,
            animationSpec = SpringBouncy
        )

    val ParallaxPopExit: ExitTransition =
        slideOutHorizontally(
            targetOffsetX = { (it * 1.0).toInt() },
            animationSpec = SpringOffset
        ) + fadeOut(
            animationSpec = tween(200)
        ) + scaleOut(
            targetScale = 0.96f,
            animationSpec = SpringStiff
        )

    val PopEnter: EnterTransition =
        scaleIn(
            initialScale = 0.9f,
            animationSpec = SpringBouncy
        ) + fadeIn(
            animationSpec = tween(200)
        )

    val PopExit: ExitTransition =
        scaleOut(
            targetScale = 0.9f,
            animationSpec = SpringStiff
        ) + fadeOut(
            animationSpec = tween(200)
        )

    val SubtleSlideIn: EnterTransition =
        slideInVertically(
            initialOffsetY = { (it * 0.1).toInt() },
            animationSpec = SpringOffset
        ) + fadeIn(
            animationSpec = tween(300)
        )

    val SubtleSlideOut: ExitTransition =
        slideOutVertically(
            targetOffsetY = { (it * 0.1).toInt() },
            animationSpec = SpringOffset
        ) + fadeOut(
            animationSpec = tween(300)
        )
}
