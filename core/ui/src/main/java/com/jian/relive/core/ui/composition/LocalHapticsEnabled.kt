package com.jian.relive.core.ui.composition

import androidx.compose.runtime.compositionLocalOf

/**
 * 控制应用程序全局触觉反馈开关的 CompositionLocal。
 */
val LocalHapticsEnabled = compositionLocalOf { true }
