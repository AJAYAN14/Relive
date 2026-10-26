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
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.jian.relive.core.media.MotionPhotoHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import com.jian.relive.ui.SplashRoute
import com.jian.relive.navigation.AppBottomNavigationBar
import com.jian.relive.navigation.MainNavTab
import com.jian.relive.navigation.ReliveNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestHighestRefreshRate()

        val appGraph = AppGraph(applicationContext)
        val initialPrefs = appGraph.preferencesManager
        var isAppReady = false

        splashScreen.setKeepOnScreenCondition { !isAppReady }
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val fadeOut = android.animation.ObjectAnimator.ofFloat(
                splashScreenViewProvider.view,
                android.view.View.ALPHA,
                1f,
                0f
            ).apply {
                interpolator = android.view.animation.DecelerateInterpolator()
                duration = 200L
                doOnEnd {
                    splashScreenViewProvider.remove()
                }
            }
            fadeOut.start()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            MotionPhotoHelper.initCache(applicationContext)

            val hasImages = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
            val hasVideo = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED

            if (initialPrefs.onboardingCompleted && hasImages && hasVideo) {
                appGraph.mediaRepository.loadMedia(applicationContext)
            }
            withContext(Dispatchers.Main) {
                isAppReady = true
            }
        }

        setContent {
            val context = LocalContext.current
            val prefs = remember { appGraph.preferencesManager }
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

            var isOrganizeOverlayActive by remember { mutableStateOf(false) }

            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            LaunchedEffect(currentDestination) {
                when {
                    currentDestination?.hasRoute<OrganizeRoute>() == true -> currentTab = MainNavTab.ORGANIZE
                    currentDestination?.hasRoute<FeedRoute>() == true -> currentTab = MainNavTab.LIVE_FEED
                    currentDestination?.hasRoute<StatsRoute>() == true -> currentTab = MainNavTab.STATS
                    currentDestination?.hasRoute<SettingsRoute>() == true -> currentTab = MainNavTab.SETTINGS
                }
            }

            val isTopLevelRoute = currentDestination?.let { dest ->
                dest.hasRoute<OrganizeRoute>() ||
                dest.hasRoute<FeedRoute>() ||
                dest.hasRoute<StatsRoute>() ||
                dest.hasRoute<SettingsRoute>()
            } ?: false

            val showBottomBar = onboardingCompleted && isTopLevelRoute && !isOrganizeOverlayActive

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
                                startDestination = SplashRoute,
                                hasPermission = hasPermission,
                                onRequestPermission = requestPermissions,
                                hasManageStorage = hasManageStorage,
                                onRequestManageStorage = requestManageStorage,
                                mediaRepository = appGraph.mediaRepository,
                                onSplashComplete = {
                                    val destination = if (onboardingCompleted) OrganizeRoute else WelcomeRoute
                                    navController.navigate(destination) {
                                        popUpTo(SplashRoute) {
                                            inclusive = true
                                        }
                                        launchSingleTop = true
                                    }
                                },
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
                                onOverlayActiveChanged = { isOrganizeOverlayActive = it },
                                modifier = Modifier.fillMaxSize()
                            )

                            AnimatedVisibility(
                                visible = showBottomBar,
                                enter = fadeIn(animationSpec = tween(200)) + slideInVertically { it },
                                exit = fadeOut(animationSpec = tween(200)) + slideOutVertically { it },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .padding(bottom = 16.dp)
                            ) {
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
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestHighestRefreshRate() {
        val currentDisplay = display ?: return
        val currentMode = currentDisplay.mode
        val targetMode = currentDisplay.supportedModes
            .filter { it.physicalWidth == currentMode.physicalWidth && it.physicalHeight == currentMode.physicalHeight }
            .maxByOrNull { it.refreshRate }

        if (targetMode != null) {
            val layoutParams = window.attributes
            layoutParams.preferredDisplayModeId = targetMode.modeId
            window.attributes = layoutParams
        }
    }
}