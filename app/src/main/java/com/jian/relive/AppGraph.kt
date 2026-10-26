package com.jian.relive

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import com.jian.relive.core.common.preferences.PreferencesManager
import com.jian.relive.core.media.MediaRepository

/**
 * 应用程序级全局共享依赖装配容器。
 */
class AppGraph(
    val context: Context
) {
    val mediaRepository = MediaRepository()
    val preferencesManager = PreferencesManager(context)
}

val LocalAppGraph = compositionLocalOf<AppGraph> {
    error("LocalAppGraph not provided")
}
