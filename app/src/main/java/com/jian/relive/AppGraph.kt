package com.jian.relive

import android.content.Context
import androidx.compose.runtime.compositionLocalOf

/**
 * 应用程序级全局共享依赖装配容器。
 */
class AppGraph(
    val context: Context
)

val LocalAppGraph = compositionLocalOf<AppGraph> {
    error("LocalAppGraph not provided")
}
