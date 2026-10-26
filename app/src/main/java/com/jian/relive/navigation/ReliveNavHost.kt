package com.jian.relive.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.jian.relive.feature.clean.CleanRoute
import com.jian.relive.feature.clean.cleanScreen
import com.jian.relive.feature.compress.CompressRoute
import com.jian.relive.feature.compress.compressScreen
import com.jian.relive.feature.feed.LikesRoute
import com.jian.relive.feature.feed.feedScreen
import com.jian.relive.feature.feed.likesScreen
import com.jian.relive.feature.organize.OrganizeRoute
import com.jian.relive.feature.organize.organizeScreen
import com.jian.relive.feature.settings.settingsScreen
import com.jian.relive.feature.stats.StatsRoute
import com.jian.relive.feature.stats.statsScreen

@Composable
fun ReliveNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = OrganizeRoute,
        modifier = modifier
    ) {
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
