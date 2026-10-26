package com.jian.relive.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import com.jian.relive.core.common.language.AppLanguage
import com.jian.relive.core.common.preferences.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 设置模块 UI 状态实体。
 *
 * @property batchSize 单次滑卡整理卡片批次大小。
 * @property queueOrder 整理卡片排序方式（"random" 或 "time"）。
 * @property muteVideos 视频默认静音开关。
 * @property soundEffectsEnabled 音效开关。
 * @property hapticsEnabled 触觉震动开关。
 * @property avatarType 整理助手形象类型（"girl" 或 "boy"）。
 * @property language 当前应用语言。
 * @property isFirstLaunch 是否首次启动（欢迎流）。
 */
data class SettingsUiState(
    val batchSize: Int = 16,
    val queueOrder: String = "random",
    val muteVideos: Boolean = true,
    val soundEffectsEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val avatarType: String = "girl",
    val language: AppLanguage = AppLanguage.English,
    val isFirstLaunch: Boolean = false
)

/**
 * 设置模块状态持有与业务处理 ViewModel。
 */
class SettingsViewModel(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            batchSize = preferencesManager.batchSize,
            queueOrder = preferencesManager.queueOrder,
            muteVideos = preferencesManager.muteVideos,
            soundEffectsEnabled = preferencesManager.soundEffectsEnabled,
            hapticsEnabled = preferencesManager.hapticsEnabled,
            avatarType = preferencesManager.avatarType,
            language = preferencesManager.language,
            isFirstLaunch = !preferencesManager.onboardingCompleted
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setBatchSize(size: Int) {
        val coerced = size.coerceIn(10, 30)
        preferencesManager.batchSize = coerced
        _uiState.update { it.copy(batchSize = coerced) }
    }

    fun setQueueOrder(order: String) {
        preferencesManager.queueOrder = order
        _uiState.update { it.copy(queueOrder = order) }
    }

    fun setMuteVideos(mute: Boolean) {
        preferencesManager.muteVideos = mute
        _uiState.update { it.copy(muteVideos = mute) }
    }

    fun setSoundEffectsEnabled(enabled: Boolean) {
        preferencesManager.soundEffectsEnabled = enabled
        _uiState.update { it.copy(soundEffectsEnabled = enabled) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        preferencesManager.hapticsEnabled = enabled
        _uiState.update { it.copy(hapticsEnabled = enabled) }
    }

    fun setAvatarType(type: String) {
        preferencesManager.avatarType = type
        _uiState.update { it.copy(avatarType = type) }
    }

    fun setLanguage(language: AppLanguage) {
        preferencesManager.language = language
        _uiState.update { it.copy(language = language) }
    }

    fun completeOnboarding() {
        preferencesManager.onboardingCompleted = true
        _uiState.update { it.copy(isFirstLaunch = false) }
    }

    /**
     * 清理所有保留照片的标记，使其能重新进入整理队列。
     */
    fun releaseKeptPhotos(onComplete: () -> Unit = {}) {
        val processed = preferencesManager.processedIds
        val liked = preferencesManager.likedIds
        val pendingTrash = preferencesManager.pendingTrashIds
        val moved = preferencesManager.movedIds

        val toRemove = processed.filter { id ->
            !liked.contains(id) && !pendingTrash.contains(id) && !moved.contains(id)
        }.toSet()

        if (toRemove.isNotEmpty()) {
            processed.removeAll(toRemove)
            preferencesManager.processedIds = processed

            val stats = preferencesManager.stats
            val newStats = stats.copy(
                processed = (stats.processed - toRemove.size).coerceAtLeast(0),
                skipped = (stats.skipped - toRemove.size).coerceAtLeast(0)
            )
            preferencesManager.stats = newStats
        }
        onComplete()
    }
}
