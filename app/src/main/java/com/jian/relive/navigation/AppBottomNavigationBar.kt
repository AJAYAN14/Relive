package com.jian.relive.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.MotionPhotosOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.relive.R
import com.jian.relive.core.ui.animation.bounceClick
import com.jian.relive.core.ui.theme.ReliveBlack
import com.jian.relive.core.ui.theme.RelivePrimary
import com.jian.relive.core.ui.theme.ReliveWhite

enum class MainNavTab {
    ORGANIZE,
    LIVE_FEED,
    STATS,
    SETTINGS
}

data class NavTabItem(
    val tab: MainNavTab,
    val icon: ImageVector,
    val labelRes: Int
)

@Composable
fun AppBottomNavigationBar(
    currentTab: MainNavTab,
    onTabSelected: (MainNavTab) -> Unit,
    modifier: Modifier = Modifier,
    hapticsEnabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val tabs = listOf(
        NavTabItem(MainNavTab.ORGANIZE, Icons.Outlined.Layers, R.string.nav_organize),
        NavTabItem(MainNavTab.LIVE_FEED, Icons.Outlined.MotionPhotosOn, R.string.nav_live_feed),
        NavTabItem(MainNavTab.STATS, Icons.Outlined.BarChart, R.string.nav_stats),
        NavTabItem(MainNavTab.SETTINGS, Icons.Outlined.Settings, R.string.nav_settings)
    )

    val containerColor = ReliveWhite.copy(alpha = 0.94f)
    val borderColor = ReliveBlack.copy(alpha = 0.06f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(68.dp),
        shape = RoundedCornerShape(34.dp),
        color = containerColor,
        shadowElevation = 8.dp,
        border = BorderStroke(width = 1.dp, color = borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { item ->
                val isSelected = currentTab == item.tab

                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.08f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "icon_scale"
                )

                val activeBgColor = RelivePrimary.copy(alpha = 0.12f)
                val contentColor = if (isSelected) RelivePrimary else ReliveBlack.copy(alpha = 0.45f)

                val label = stringResource(item.labelRes)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .bounceClick(onClick = {
                            if (hapticsEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            onTabSelected(item.tab)
                        })
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) activeBgColor else Color.Transparent)
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = label,
                                tint = contentColor,
                                modifier = Modifier
                                    .size(22.dp)
                                    .scale(iconScale)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}
