package com.jian.relive.feature.stats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.relive.core.ui.composition.LocalHapticsEnabled
import com.jian.relive.core.ui.theme.ReliveAmber100
import com.jian.relive.core.ui.theme.ReliveAmber800
import com.jian.relive.core.ui.theme.ReliveAmber900
import com.jian.relive.core.ui.theme.ReliveBlack
import com.jian.relive.core.ui.theme.ReliveBlue400
import com.jian.relive.core.ui.theme.ReliveBlue50
import com.jian.relive.core.ui.theme.ReliveBlue800
import com.jian.relive.core.ui.theme.ReliveBlue900
import com.jian.relive.core.ui.theme.ReliveGray100
import com.jian.relive.core.ui.theme.ReliveGray400
import com.jian.relive.core.ui.theme.ReliveGray500
import com.jian.relive.core.ui.theme.ReliveGray600
import com.jian.relive.core.ui.theme.ReliveGreen50
import com.jian.relive.core.ui.theme.ReliveGreen800
import com.jian.relive.core.ui.theme.ReliveGreen900
import com.jian.relive.core.ui.theme.ReliveHeatmapGreen1
import com.jian.relive.core.ui.theme.ReliveHeatmapGreen2
import com.jian.relive.core.ui.theme.ReliveHeatmapGreen3
import com.jian.relive.core.ui.theme.ReliveHeatmapGreen4
import com.jian.relive.core.ui.theme.ReliveHeatmapGreen5
import com.jian.relive.core.ui.theme.RelivePurple50
import com.jian.relive.core.ui.theme.RelivePurple800
import com.jian.relive.core.ui.theme.RelivePurple900
import com.jian.relive.core.ui.theme.ReliveWarmOrangeBg
import com.jian.relive.core.ui.theme.ReliveWarmOrangeDark
import com.jian.relive.core.ui.theme.ReliveWarmOrangeMedium
import com.jian.relive.core.ui.theme.ReliveWhite
import com.jian.relive.feature.stats.R
import com.jian.relive.feature.stats.model.ShootingStatsData
import com.jian.relive.feature.stats.model.StatsPeriod
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 年/月切换分段胶囊组件。
 */
@Composable
fun StatsPeriodSegmentedControl(
    selectedPeriod: StatsPeriod,
    onPeriodSelected: (StatsPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current

    Row(
        modifier = modifier
            .background(ReliveGray100, RoundedCornerShape(50))
            .padding(4.dp)
    ) {
        StatsPeriod.entries.forEach { period ->
            val isSelected = selectedPeriod == period
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) ReliveWhite else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable {
                        if (!isSelected) {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPeriodSelected(period)
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(period.titleRes),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) ReliveBlack else ReliveGray500
                )
            }
        }
    }
}

/**
 * 统计总览大卡片，展示当前周期的标题、总项数及翻页箭头。
 */
@Composable
fun StatsSummaryBanner(
    period: StatsPeriod,
    year: Int,
    month: Int,
    canGoNext: Boolean,
    canGoPrev: Boolean,
    stats: ShootingStatsData,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current

    val titleText = remember(period, year, month) {
        if (period == StatsPeriod.YEAR) {
            year.toString()
        } else {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
            }
            val isZh = Locale.getDefault().language.startsWith("zh")
            if (isZh) {
                SimpleDateFormat("yyyy年 MMMM", Locale.getDefault()).format(cal.time)
            } else {
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(ReliveWarmOrangeBg)
            .padding(24.dp)
    ) {
        Column {
            val formatRes = if (period == StatsPeriod.YEAR) R.string.stats_summary_year else R.string.stats_summary_month
            Text(
                text = stringResource(formatRes, titleText, stats.totalCount),
                fontSize = 30.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = ReliveWarmOrangeDark,
                lineHeight = 38.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            val itemsDetail = stringResource(R.string.stats_items_detail, stats.cameraPhotoCount, stats.videoCount)
            val screenshotsDetail = stringResource(R.string.stats_screenshots_count, stats.screenshotCount)

            Text(
                text = "$itemsDetail | $screenshotsDetail",
                fontSize = 13.sp,
                color = ReliveWarmOrangeMedium,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (canGoPrev) {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPrevClick()
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.stats_back),
                        tint = ReliveWarmOrangeDark
                    )
                }

                IconButton(
                    onClick = {
                        if (canGoNext) {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNextClick()
                        }
                    },
                    enabled = canGoNext,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = if (canGoNext) ReliveWarmOrangeDark else ReliveWarmOrangeDark.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

/**
 * 2x2 统计指标矩阵（视频时长、拍摄足迹、喜欢相片、最活跃日）。
 */
@Composable
fun StatsMetricGrid(
    stats: ShootingStatsData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(ReliveBlue50)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Videocam,
                        contentDescription = null,
                        tint = ReliveBlue800,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.stats_video_title),
                        fontSize = 12.sp,
                        color = ReliveBlue800,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val durationText = if (stats.totalDurationMins == 0 && stats.videoCount > 0) {
                    stringResource(R.string.stats_video_duration_less_than_one)
                } else {
                    stringResource(R.string.stats_video_duration_mins, stats.totalDurationMins)
                }

                Text(
                    text = durationText,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = ReliveBlue900
                )

                if (stats.movieComparisonText.isNotEmpty()) {
                    Text(
                        text = stats.movieComparisonText,
                        fontSize = 11.sp,
                        color = ReliveBlue400,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(ReliveAmber100)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Place,
                        contentDescription = null,
                        tint = ReliveAmber800,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.stats_location_title),
                        fontSize = 12.sp,
                        color = ReliveAmber800,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.stats_location_count, stats.locationCount),
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = ReliveAmber900
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .heightIn(min = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(ReliveGreen50)
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = ReliveGreen800,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.stats_likes_title),
                            fontSize = 12.sp,
                            color = ReliveGreen800,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stats.likedCount.toString(),
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = ReliveGreen900
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(RelivePurple50)
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Whatshot,
                            contentDescription = null,
                            tint = RelivePurple800,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.stats_active_day_title),
                            fontSize = 12.sp,
                            color = RelivePurple800,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (stats.mostActiveDate != null) {
                        Text(
                            text = stringResource(R.string.stats_most_active_detail, stats.mostActiveDate, stats.mostActiveCount),
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = RelivePurple900
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.stats_active_empty),
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = RelivePurple900
                        )
                    }
                }
            }
        }
    }
}

/**
 * 拍摄活跃度贡献热力图矩阵。
 */
@Composable
fun StatsContributionHeatmap(
    period: StatsPeriod,
    year: Int,
    month: Int,
    heatmapData: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val heatmapColors = listOf(
        ReliveGray100,
        ReliveHeatmapGreen1,
        ReliveHeatmapGreen2,
        ReliveHeatmapGreen3,
        ReliveHeatmapGreen4,
        ReliveHeatmapGreen5
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.stats_heatmap_title),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = ReliveBlack.copy(alpha = 0.75f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (period == StatsPeriod.MONTH) {
            val calendarGrid = remember(year, month) {
                val grid = mutableListOf<Date>()
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, 1)
                    val dayOfWeek = get(Calendar.DAY_OF_WEEK)
                    add(Calendar.DATE, -(dayOfWeek - 1))
                }

                for (i in 0 until 42) {
                    grid.add(cal.time)
                    cal.add(Calendar.DATE, 1)
                }
                grid
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    for (w in 0 until 6) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            for (d in 0 until 7) {
                                val index = w * 7 + d
                                if (index < calendarGrid.size) {
                                    val cellDate = calendarGrid[index]
                                    val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cellDate)
                                    val count = heatmapData[dateKey] ?: 0

                                    val color = when {
                                        count == 0 -> heatmapColors[0]
                                        count <= 2 -> heatmapColors[1]
                                        count <= 5 -> heatmapColors[2]
                                        count <= 10 -> heatmapColors[3]
                                        count <= 20 -> heatmapColors[4]
                                        else -> heatmapColors[5]
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(color)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val weeks = 53
            val scrollState = rememberScrollState()

            LaunchedEffect(period, year) {
                scrollState.scrollTo(0)
            }

            val weekCellSize = 10.dp
            val weekColumnWidth = 16.dp
            val weekRowSpacing = 3.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .heightIn(min = 90.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                for (w in 0 until weeks) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(weekRowSpacing),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(weekColumnWidth)
                    ) {
                        for (dayOfWeek in 0 until 7) {
                            val cellDate = calculateYearDate(year, w, dayOfWeek)
                            val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cellDate)
                            val count = heatmapData[dateKey] ?: 0

                            val color = when {
                                count == 0 -> heatmapColors[0]
                                count <= 2 -> heatmapColors[1]
                                count <= 5 -> heatmapColors[2]
                                count <= 10 -> heatmapColors[3]
                                count <= 20 -> heatmapColors[4]
                                else -> heatmapColors[5]
                            }

                            Box(
                                modifier = Modifier
                                    .size(weekCellSize)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(color)
                            )
                        }
                    }
                }
            }

            val startCal = remember(year) {
                Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, Calendar.JANUARY)
                    set(Calendar.DAY_OF_MONTH, 1)
                    val dayOfWeek = get(Calendar.DAY_OF_WEEK)
                    add(Calendar.DATE, -(dayOfWeek - 1))
                }
            }

            val monthLabelIndices = remember(year) {
                listOf(2, 5, 8, 11).associate { monthIndex ->
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, monthIndex)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                    val diffDays = ((cal.timeInMillis - startCal.timeInMillis) / (1000L * 60 * 60 * 24)).toInt()
                    val weekIndex = (diffDays / 7).coerceIn(0, weeks - 1)
                    weekIndex to (monthIndex + 1).toString()
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                for (w in 0 until weeks) {
                    Box(
                        modifier = Modifier.width(weekColumnWidth),
                        contentAlignment = Alignment.Center
                    ) {
                        monthLabelIndices[w]?.let { label ->
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                color = ReliveGray400,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.stats_heatmap_less),
                fontSize = 11.sp,
                color = ReliveGray500
            )
            Spacer(modifier = Modifier.width(4.dp))
            heatmapColors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = stringResource(R.string.stats_heatmap_more),
                fontSize = 11.sp,
                color = ReliveGray500
            )
        }
    }
}

private fun calculateYearDate(year: Int, weekIndex: Int, dayIndex: Int): Date {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, Calendar.JANUARY)
        set(Calendar.DAY_OF_MONTH, 1)
        val dayOfWeek = get(Calendar.DAY_OF_WEEK)
        add(Calendar.DATE, -(dayOfWeek - 1))
        add(Calendar.DATE, weekIndex * 7 + dayIndex)
    }
    return cal.time
}
