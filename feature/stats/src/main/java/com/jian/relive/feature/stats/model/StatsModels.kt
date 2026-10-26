package com.jian.relive.feature.stats.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.jian.relive.feature.stats.R

/**
 * 统计页面时间跨度模式枚举。
 */
enum class StatsPeriod(
    @get:StringRes val titleRes: Int
) {
    YEAR(R.string.stats_period_year),
    MONTH(R.string.stats_period_month)
}

/**
 * 拍摄多维度指标分析结果。
 */
@Immutable
data class ShootingStatsData(
    val photoCount: Int = 0,
    val videoCount: Int = 0,
    val totalCount: Int = 0,
    val totalDurationMins: Int = 0,
    val screenshotCount: Int = 0,
    val likedCount: Int = 0,
    val cameraPhotoCount: Int = 0,
    val mostActiveDate: String? = null,
    val mostActiveCount: Int = 0,
    val locationCount: Int = 0,
    val heatmapData: Map<String, Int> = emptyMap(),
    val movieComparisonText: String = ""
)

/**
 * 统计模块整体响应式 UI 状态容器。
 */
@Immutable
data class StatsUiState(
    val period: StatsPeriod = StatsPeriod.YEAR,
    val targetYear: Int = 2026,
    val targetMonth: Int = 0,
    val canGoNext: Boolean = false,
    val canGoPrev: Boolean = true,
    val stats: ShootingStatsData = ShootingStatsData(),
    val isLoading: Boolean = false
)
