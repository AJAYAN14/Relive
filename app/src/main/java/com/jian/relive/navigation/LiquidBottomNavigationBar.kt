package com.jian.relive.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MotionPhotosOn
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.relive.R
import com.jian.relive.core.ui.component.liquid.LiquidBottomTab
import com.jian.relive.core.ui.component.liquid.LiquidBottomTabs
import com.jian.relive.core.ui.theme.LocalDarkTheme
import com.jian.relive.core.ui.theme.ReliveBlack
import com.jian.relive.core.ui.theme.ReliveWhite
import com.kyant.backdrop.Backdrop

@Composable
fun LiquidBottomNavigationBar(
    currentTab: MainNavTab,
    onTabSelected: (MainNavTab) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val tabs = remember {
        listOf(
            MainNavTab.ORGANIZE,
            MainNavTab.LIVE_FEED,
            MainNavTab.STATS,
            MainNavTab.SETTINGS
        )
    }

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(tabs.indexOf(currentTab).coerceAtLeast(0)) }

    LaunchedEffect(currentTab) {
        val targetIndex = tabs.indexOf(currentTab)
        if (targetIndex >= 0 && selectedTabIndex != targetIndex) {
            selectedTabIndex = targetIndex
        }
    }

    val isDarkTheme = LocalDarkTheme.current
    val contentColor = if (isDarkTheme) ReliveWhite else ReliveBlack

    val labels = listOf(
        stringResource(R.string.nav_organize),
        stringResource(R.string.nav_live_feed),
        stringResource(R.string.nav_stats),
        stringResource(R.string.nav_settings)
    )

    val roundedIcons: List<ImageVector> = listOf(
        Icons.Rounded.Layers,
        Icons.Rounded.MotionPhotosOn,
        Icons.Rounded.BarChart,
        Icons.Rounded.Settings
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        LiquidBottomTabs(
            selectedTabIndex = { selectedTabIndex },
            onTabSelected = { index ->
                selectedTabIndex = index
                onTabSelected(tabs[index])
            },
            backdrop = backdrop,
            tabsCount = 4
        ) {
            repeat(4) { index ->
                LiquidBottomTab(
                    onClick = {
                        selectedTabIndex = index
                    }
                ) {
                    val icon = roundedIcons[index]
                    val painter = rememberVectorPainter(icon)

                    Box(
                        Modifier
                            .size(28.dp)
                            .paint(painter, colorFilter = ColorFilter.tint(contentColor))
                    )
                    BasicText(
                        text = labels[index],
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}
