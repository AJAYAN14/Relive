package com.jian.relive.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.jian.relive.core.common.preferences.PreferencesManager
import com.jian.relive.core.media.MediaRepository
import com.jian.relive.feature.clean.CleanRoute
import com.jian.relive.feature.clean.cleanScreen
import com.jian.relive.feature.compress.CompressRoute
import com.jian.relive.feature.compress.compressScreen
import com.jian.relive.feature.feed.LikesRoute
import com.jian.relive.feature.feed.feedScreen
import com.jian.relive.feature.feed.likesScreen
import com.jian.relive.feature.organize.OrganizeRoute
import com.jian.relive.feature.organize.organizeScreen
import com.jian.relive.feature.settings.WelcomeRoute
import com.jian.relive.feature.settings.settingsScreen
import com.jian.relive.feature.settings.ui.WelcomeScreen
import com.jian.relive.feature.stats.StatsRoute
import com.jian.relive.feature.stats.statsScreen
import com.jian.relive.ui.SplashRoute
import com.jian.relive.ui.SplashScreen

@Composable
fun ReliveNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: Any = SplashRoute,
    hasPermission: Boolean = true,
    onRequestPermission: () -> Unit = {},
    hasManageStorage: Boolean = true,
    onRequestManageStorage: () -> Unit = {},
    mediaRepository: MediaRepository? = null,
    onFinishWelcome: () -> Unit = {},
    onOverlayActiveChanged: (Boolean) -> Unit = {},
    onSplashComplete: () -> Unit = {}
) {
    val mainScreenRoutes = remember {
        setOf("OrganizeRoute", "FeedRoute", "StatsRoute", "SettingsRoute")
    }

    fun isMainScreen(route: String?): Boolean {
        if (route == null) return false
        return mainScreenRoutes.any { route.contains(it) }
    }

    val mainEnterTransition: androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> androidx.compose.animation.EnterTransition? = {
        val initialRoute = initialState.destination.route
        val targetRoute = targetState.destination.route
        HorizontalNavTransition.enterTransition(initialRoute, targetRoute) ?:
        if (!isMainScreen(initialRoute)) AppTransitions.popEnterTransition()
        else BottomNavTransition.enterTransition()
    }

    val mainExitTransition: androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> androidx.compose.animation.ExitTransition? = {
        val initialRoute = initialState.destination.route
        val targetRoute = targetState.destination.route
        HorizontalNavTransition.exitTransition(initialRoute, targetRoute) ?:
        if (!isMainScreen(targetRoute)) AppTransitions.exitTransition()
        else BottomNavTransition.exitTransition()
    }

    val mainPopEnterTransition: androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> androidx.compose.animation.EnterTransition? = {
        val initialRoute = initialState.destination.route
        val targetRoute = targetState.destination.route
        HorizontalNavTransition.enterTransition(initialRoute, targetRoute) ?:
        if (!isMainScreen(initialRoute)) AppTransitions.popEnterTransition()
        else BottomNavTransition.enterTransition()
    }

    val mainPopExitTransition: androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> androidx.compose.animation.ExitTransition? = {
        val initialRoute = initialState.destination.route
        val targetRoute = targetState.destination.route
        HorizontalNavTransition.exitTransition(initialRoute, targetRoute) ?:
        BottomNavTransition.exitTransition()
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { AppTransitions.enterTransition() },
        exitTransition = { AppTransitions.exitTransition() },
        popEnterTransition = { AppTransitions.popEnterTransition() },
        popExitTransition = { AppTransitions.popExitTransition() }
    ) {
        composable<SplashRoute> {
            SplashScreen(onSplashComplete = onSplashComplete)
        }

        composable<WelcomeRoute> {
            val context = LocalContext.current
            val prefs = remember { PreferencesManager(context) }
            var currentLanguage by remember { mutableStateOf(prefs.language) }

            WelcomeScreen(
                currentLanguage = currentLanguage,
                onLanguageSelected = { lang ->
                    currentLanguage = lang
                    prefs.language = lang
                },
                hasPermission = hasPermission,
                onRequestPermission = onRequestPermission,
                hasManageStorage = hasManageStorage,
                onRequestManageStorage = onRequestManageStorage,
                onFinish = onFinishWelcome
            )
        }

        organizeScreen(
            mediaRepository = mediaRepository,
            onOverlayActiveChanged = onOverlayActiveChanged,
            onNavigateToStats = {
                navController.navigate(StatsRoute)
            },
            onNavigateToClean = {
                navController.navigate(CleanRoute)
            },
            onNavigateToCompress = {
                navController.navigate(CompressRoute)
            },
            onNavigateToLikes = {
                navController.navigate(LikesRoute)
            },
            enterTransition = mainEnterTransition,
            exitTransition = mainExitTransition,
            popEnterTransition = mainPopEnterTransition,
            popExitTransition = mainPopExitTransition
        )
        feedScreen(
            enterTransition = mainEnterTransition,
            exitTransition = mainExitTransition,
            popEnterTransition = mainPopEnterTransition,
            popExitTransition = mainPopExitTransition
        )
        likesScreen(
            onBack = { navController.popBackStack() }
        )
        statsScreen(
            onBack = { navController.popBackStack() },
            enterTransition = mainEnterTransition,
            exitTransition = mainExitTransition,
            popEnterTransition = mainPopEnterTransition,
            popExitTransition = mainPopExitTransition
        )
        settingsScreen(
            onBack = { navController.popBackStack() },
            enterTransition = mainEnterTransition,
            exitTransition = mainExitTransition,
            popEnterTransition = mainPopEnterTransition,
            popExitTransition = mainPopExitTransition
        )
        cleanScreen(
            onBack = { navController.popBackStack() }
        )
        compressScreen(
            onBack = { navController.popBackStack() }
        )
    }
}
