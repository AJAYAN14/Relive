package com.jian.relive.feature.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jian.relive.core.ui.animation.bounceClick
import com.jian.relive.core.ui.component.PredictiveBackWrapper
import com.jian.relive.core.ui.theme.ReliveBlack
import com.jian.relive.core.ui.theme.ReliveWhite
import com.jian.relive.feature.stats.ui.StatsContributionHeatmap
import com.jian.relive.feature.stats.ui.StatsMetricGrid
import com.jian.relive.feature.stats.ui.StatsPeriodSegmentedControl
import com.jian.relive.feature.stats.ui.StatsSummaryBanner
import kotlinx.serialization.Serializable

@Serializable
data object StatsRoute

/**
 * 在导航图中注册拍摄统计功能页面入口。
 */
fun NavGraphBuilder.statsScreen(
    onBack: () -> Unit = {}
) {
    composable<StatsRoute> {
        val viewModel: StatsViewModel = viewModel()
        StatsRoute(
            viewModel = viewModel,
            onBack = onBack
        )
    }
}

@Composable
internal fun StatsRoute(
    viewModel: StatsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PredictiveBackWrapper(
        isVisible = true,
        onBack = onBack
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(ReliveWhite)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.CenterStart)
                        .clip(CircleShape)
                        .background(ReliveBlack.copy(alpha = 0.05f))
                        .bounceClick(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.stats_back),
                        tint = ReliveBlack
                    )
                }

                Text(
                    text = stringResource(R.string.stats_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ReliveBlack,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatsPeriodSegmentedControl(
                        selectedPeriod = uiState.period,
                        onPeriodSelected = { viewModel.selectPeriod(it) }
                    )
                }

                StatsSummaryBanner(
                    period = uiState.period,
                    year = uiState.targetYear,
                    month = uiState.targetMonth,
                    canGoNext = uiState.canGoNext,
                    canGoPrev = uiState.canGoPrev,
                    stats = uiState.stats,
                    onPrevClick = { viewModel.navigatePrevious() },
                    onNextClick = { viewModel.navigateNext() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                StatsMetricGrid(stats = uiState.stats)

                Spacer(modifier = Modifier.height(24.dp))

                StatsContributionHeatmap(
                    period = uiState.period,
                    year = uiState.targetYear,
                    month = uiState.targetMonth,
                    heatmapData = uiState.stats.heatmapData
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
