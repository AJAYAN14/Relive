package com.jian.relive.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

object AppTransitions {
    private const val ANIMATION_DURATION = 300
    private const val FADE_DURATION = 150
    private const val ENTER_INITIAL_SCALE = 1.02f
    private const val EXIT_TARGET_SCALE = 0.98f

    fun enterTransition(): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = FastOutSlowInEasing
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = FADE_DURATION
            )
        ) + scaleIn(
            initialScale = ENTER_INITIAL_SCALE,
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = FastOutSlowInEasing
            )
        )
    }

    fun exitTransition(): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = FastOutSlowInEasing
            )
        ) + scaleOut(
            targetScale = EXIT_TARGET_SCALE,
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = FastOutSlowInEasing
            )
        )
    }

    fun popEnterTransition(): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = FastOutSlowInEasing
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = FADE_DURATION
            )
        ) + scaleIn(
            initialScale = EXIT_TARGET_SCALE,
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = FastOutSlowInEasing
            )
        )
    }

    fun popExitTransition(): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = FastOutSlowInEasing
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = FADE_DURATION
            )
        ) + scaleOut(
            targetScale = ENTER_INITIAL_SCALE,
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = FastOutSlowInEasing
            )
        )
    }
}

object BottomNavTransition {
    private const val TRANSITION_DURATION = 300

    fun enterTransition(): EnterTransition {
        return fadeIn(
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = LinearOutSlowInEasing
            )
        )
    }

    fun exitTransition(): ExitTransition {
        return fadeOut(
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = FastOutLinearInEasing
            )
        )
    }
}

object HorizontalNavTransition {
    private const val ANIMATION_DURATION = 300

    fun getTabIndex(route: String?): Int {
        if (route == null) return -1
        return when {
            route.contains("OrganizeRoute") -> 0
            route.contains("FeedRoute") -> 1
            route.contains("StatsRoute") -> 2
            route.contains("SettingsRoute") -> 3
            else -> -1
        }
    }

    fun enterTransition(initialRoute: String?, targetRoute: String?): EnterTransition? {
        val fromIndex = getTabIndex(initialRoute)
        val toIndex = getTabIndex(targetRoute)

        if (fromIndex != -1 && toIndex != -1) {
            return if (toIndex > fromIndex) {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(ANIMATION_DURATION, easing = FastOutSlowInEasing)
                )
            } else {
                slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(ANIMATION_DURATION, easing = FastOutSlowInEasing)
                )
            }
        }
        return null
    }

    fun exitTransition(initialRoute: String?, targetRoute: String?): ExitTransition? {
        val fromIndex = getTabIndex(initialRoute)
        val toIndex = getTabIndex(targetRoute)

        if (fromIndex != -1 && toIndex != -1) {
            return if (toIndex > fromIndex) {
                slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(ANIMATION_DURATION, easing = FastOutSlowInEasing)
                )
            } else {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(ANIMATION_DURATION, easing = FastOutSlowInEasing)
                )
            }
        }
        return null
    }
}
