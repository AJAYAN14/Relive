package com.jian.relive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.jian.relive.core.ui.theme.ReliveTheme
import com.jian.relive.feature.feed.FeedRoute
import com.jian.relive.feature.organize.OrganizeRoute
import com.jian.relive.feature.settings.SettingsRoute
import com.jian.relive.feature.stats.StatsRoute
import com.jian.relive.navigation.AppBottomNavigationBar
import com.jian.relive.navigation.MainNavTab
import com.jian.relive.navigation.ReliveNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appGraph = remember { AppGraph(applicationContext) }
            val navController = rememberNavController()
            var currentTab by remember { mutableStateOf(MainNavTab.ORGANIZE) }

            CompositionLocalProvider(LocalAppGraph provides appGraph) {
                ReliveTheme {
                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            ReliveNavHost(
                                navController = navController,
                                modifier = Modifier.fillMaxSize()
                            )
                            AppBottomNavigationBar(
                                currentTab = currentTab,
                                onTabSelected = { tab ->
                                    currentTab = tab
                                    val destination = when (tab) {
                                        MainNavTab.ORGANIZE -> OrganizeRoute
                                        MainNavTab.LIVE_FEED -> FeedRoute
                                        MainNavTab.STATS -> StatsRoute
                                        MainNavTab.SETTINGS -> SettingsRoute
                                    }
                                    navController.navigate(destination) {
                                        popUpTo(OrganizeRoute) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .padding(bottom = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}