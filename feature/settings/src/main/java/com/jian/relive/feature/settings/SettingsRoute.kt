package com.jian.relive.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jian.relive.feature.settings.ui.LanguageScreen
import com.jian.relive.feature.settings.ui.OpenSourceLicensesScreen
import com.jian.relive.feature.settings.ui.PrivacyScreen
import com.jian.relive.feature.settings.ui.SettingsScreen
import com.jian.relive.feature.settings.ui.WelcomeScreen
import kotlinx.serialization.Serializable

/**
 * 设置主页面路由契约。
 */
@Serializable
data object SettingsRoute

/**
 * 语言选择页面路由契约。
 */
@Serializable
data object LanguageRoute

/**
 * 隐私政策页面路由契约。
 */
@Serializable
data object PrivacyRoute

/**
 * 开源许可页面路由契约。
 */
@Serializable
data object OpenSourceLicensesRoute

/**
 * 首次欢迎引导流程路由契约。
 */
@Serializable
data object WelcomeRoute

/**
 * 注册设置功能流的类型安全路由。
 */
fun NavGraphBuilder.settingsGraph(
    viewModel: SettingsViewModel,
    onNavigateToLanguage: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToOpenSource: () -> Unit,
    onBack: () -> Unit,
    hasPermission: Boolean = true,
    onRequestPermission: () -> Unit = {},
    hasManageStorage: Boolean = true,
    onRequestManageStorage: () -> Unit = {},
    onFinishWelcome: () -> Unit = onBack
) {
    composable<SettingsRoute> {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        SettingsScreen(
            batchSize = uiState.batchSize,
            queueOrder = uiState.queueOrder,
            muteVideos = uiState.muteVideos,
            soundEffectsEnabled = uiState.soundEffectsEnabled,
            hapticsEnabled = uiState.hapticsEnabled,
            avatarType = uiState.avatarType,
            currentLanguage = uiState.language,
            onBatchSizeChange = viewModel::setBatchSize,
            onQueueOrderChange = viewModel::setQueueOrder,
            onMuteVideosChange = viewModel::setMuteVideos,
            onSoundEffectsChange = viewModel::setSoundEffectsEnabled,
            onHapticsEnabledChange = viewModel::setHapticsEnabled,
            onAvatarChange = viewModel::setAvatarType,
            onReleaseKept = viewModel::releaseKeptPhotos,
            onLanguageClick = onNavigateToLanguage,
            onPrivacyClick = onNavigateToPrivacy,
            onOpenSourceClick = onNavigateToOpenSource,
            onBack = onBack
        )
    }

    composable<LanguageRoute> {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        LanguageScreen(
            currentLanguage = uiState.language,
            onLanguageSelected = { lang ->
                viewModel.setLanguage(lang)
                onBack()
            },
            onBack = onBack
        )
    }

    composable<PrivacyRoute> {
        PrivacyScreen(onBack = onBack)
    }

    composable<OpenSourceLicensesRoute> {
        OpenSourceLicensesScreen(onBack = onBack)
    }

    composable<WelcomeRoute> {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        WelcomeScreen(
            currentLanguage = uiState.language,
            onLanguageSelected = viewModel::setLanguage,
            hasPermission = hasPermission,
            onRequestPermission = onRequestPermission,
            hasManageStorage = hasManageStorage,
            onRequestManageStorage = onRequestManageStorage,
            onFinish = {
                viewModel.completeOnboarding()
                onFinishWelcome()
            }
        )
    }
}

/**
 * 兼容旧版入口单路由注册函数。
 */
fun NavGraphBuilder.settingsScreen(
    onNavigateToLanguage: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToOpenSource: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    composable<SettingsRoute> {
        val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        SettingsScreen(
            batchSize = uiState.batchSize,
            queueOrder = uiState.queueOrder,
            muteVideos = uiState.muteVideos,
            soundEffectsEnabled = uiState.soundEffectsEnabled,
            hapticsEnabled = uiState.hapticsEnabled,
            avatarType = uiState.avatarType,
            currentLanguage = uiState.language,
            onBatchSizeChange = viewModel::setBatchSize,
            onQueueOrderChange = viewModel::setQueueOrder,
            onMuteVideosChange = viewModel::setMuteVideos,
            onSoundEffectsChange = viewModel::setSoundEffectsEnabled,
            onHapticsEnabledChange = viewModel::setHapticsEnabled,
            onAvatarChange = viewModel::setAvatarType,
            onReleaseKept = viewModel::releaseKeptPhotos,
            onLanguageClick = onNavigateToLanguage,
            onPrivacyClick = onNavigateToPrivacy,
            onOpenSourceClick = onNavigateToOpenSource,
            onBack = onBack
        )
    }
}

@Composable
private fun SettingsRoutePlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = stringResource(R.string.settings_title))
    }
}
