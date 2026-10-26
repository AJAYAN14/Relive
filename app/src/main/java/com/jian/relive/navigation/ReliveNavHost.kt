package com.jian.relive.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.jian.relive.feature.clean.cleanScreen
import com.jian.relive.feature.compress.compressScreen
import com.jian.relive.feature.feed.feedScreen
import com.jian.relive.feature.organize.OrganizeRoute
import com.jian.relive.feature.organize.organizeScreen
import com.jian.relive.feature.settings.settingsScreen
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
        organizeScreen()
        feedScreen()
        statsScreen()
        settingsScreen()
        cleanScreen()
        compressScreen()
    }
}
