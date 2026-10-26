package com.jian.relive.core.ui.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.jian.relive.core.ui.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 界面交互音效播放辅助类。
 *
 * 基于 SoundPool 提供低延迟交互提示音播放，支持按需加载与生命周期回收。
 */
class SoundEffectHelper(context: Context) {
    private val appContext = context.applicationContext
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(audioAttributes)
        .build()

    private var swipeSoundId: Int = 0
    private var deleteSoundId: Int = 0
    private var likeSoundId: Int = 0
    private var startSoundId: Int = 0

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                swipeSoundId = soundPool.load(appContext, R.raw.cards, 1)
                deleteSoundId = soundPool.load(appContext, R.raw.delete_sound, 1)
                likeSoundId = soundPool.load(appContext, R.raw.magic, 1)
                startSoundId = soundPool.load(appContext, R.raw.start, 1)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * 播放滑卡/翻牌音效。
     */
    fun playSwipe(enabled: Boolean = true) {
        if (!enabled || swipeSoundId == 0) return
        soundPool.play(swipeSoundId, 1f, 1f, 0, 0, 1f)
    }

    /**
     * 播放移入废纸篓/删除音效。
     */
    fun playDelete(enabled: Boolean = true) {
        if (!enabled) return
        if (deleteSoundId != 0) {
            soundPool.play(deleteSoundId, 0.25f, 0.25f, 0, 0, 1f)
        } else {
            playSwipe(enabled)
        }
    }

    /**
     * 播放喜欢/收藏音效。
     */
    fun playLike(enabled: Boolean = true) {
        if (!enabled) return
        if (likeSoundId != 0) {
            soundPool.play(likeSoundId, 1f, 1f, 0, 0, 1f)
        } else {
            playSwipe(enabled)
        }
    }

    /**
     * 播放开场/完成音效。
     */
    fun playStart(enabled: Boolean = true) {
        if (!enabled || startSoundId == 0) return
        soundPool.play(startSoundId, 0.8f, 0.8f, 0, 0, 1f)
    }

    /**
     * 释放音频实例。
     */
    fun release() {
        soundPool.release()
    }
}

/**
 * 记住并绑定当前组件生命周期的 [SoundEffectHelper] 实例。
 */
@Composable
fun rememberSoundEffectHelper(): SoundEffectHelper {
    val context = LocalContext.current
    val helper = remember { SoundEffectHelper(context) }
    DisposableEffect(helper) {
        onDispose {
            helper.release()
        }
    }
    return helper
}
