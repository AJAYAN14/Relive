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

@Composable
fun ReliveNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: Any = OrganizeRoute,
    hasPermission: Boolean = true,
    onRequestPermission: () -> Unit = {},
    hasManageStorage: Boolean = true,
    onRequestManageStorage: () -> Unit = {},
    onFinishWelcome: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
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
            }
        )
        feedScreen()
        likesScreen(
            onBack = { navController.popBackStack() }
        )
        statsScreen(
            onBack = { navController.popBackStack() }
        )
        settingsScreen(
            onBack = { navController.popBackStack() }
        )
        cleanScreen(
            onBack = { navController.popBackStack() }
        )
        compressScreen(
            onBack = { navController.popBackStack() }
        )
    }
}
