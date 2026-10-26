package com.jian.relive

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jian.relive.core.common.preferences.PreferencesManager
import com.jian.relive.core.ui.theme.ReliveTheme
import com.jian.relive.feature.feed.FeedRoute
import com.jian.relive.feature.organize.OrganizeRoute
import com.jian.relive.feature.settings.SettingsRoute
import com.jian.relive.feature.settings.WelcomeRoute
import com.jian.relive.feature.stats.StatsRoute
import com.jian.relive.navigation.AppBottomNavigationBar
import com.jian.relive.navigation.MainNavTab
import com.jian.relive.navigation.ReliveNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val appGraph = remember { AppGraph(applicationContext) }
            val prefs = remember { PreferencesManager(context) }
            val navController = rememberNavController()

            var onboardingCompleted by remember { mutableStateOf(prefs.onboardingCompleted) }
            var currentTab by remember { mutableStateOf(MainNavTab.ORGANIZE) }

            fun checkMediaPermission(): Boolean {
                val hasImages = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
                val hasVideo = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_VIDEO
                ) == PackageManager.PERMISSION_GRANTED
                return hasImages && hasVideo
            }

            fun checkManageStoragePermission(): Boolean {
                return Environment.isExternalStorageManager()
            }

            var hasPermission by remember { mutableStateOf(checkMediaPermission()) }
            var hasManageStorage by remember { mutableStateOf(checkManageStoragePermission()) }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        hasPermission = checkMediaPermission()
                        hasManageStorage = checkManageStoragePermission()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions(),
                onResult = {
                    hasPermission = checkMediaPermission()
                }
            )

            val requestPermissions = {
                if (!hasPermission) {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO,
                            Manifest.permission.ACCESS_MEDIA_LOCATION
                        )
                    )
                }
            }

            val requestManageStorage = {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    context.startActivity(intent)
                }
            }

            LaunchedEffect(hasPermission, onboardingCompleted) {
                if (onboardingCompleted && !hasPermission) {
                    requestPermissions()
                }
            }

            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val isWelcomeRoute = navBackStackEntry?.destination?.hasRoute<WelcomeRoute>() == true
            val showBottomBar = onboardingCompleted && !isWelcomeRoute

            CompositionLocalProvider(LocalAppGraph provides appGraph) {
                ReliveTheme {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        contentWindowInsets = WindowInsets(0.dp)
                    ) { _ ->
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            ReliveNavHost(
                                navController = navController,
                                startDestination = if (onboardingCompleted) OrganizeRoute else WelcomeRoute,
                                hasPermission = hasPermission,
                                onRequestPermission = requestPermissions,
                                hasManageStorage = hasManageStorage,
                                onRequestManageStorage = requestManageStorage,
                                onFinishWelcome = {
                                    prefs.onboardingCompleted = true
                                    onboardingCompleted = true
                                    navController.navigate(OrganizeRoute) {
                                        popUpTo(WelcomeRoute) {
                                            inclusive = true
                                        }
                                        launchSingleTop = true
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            if (showBottomBar) {
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
}