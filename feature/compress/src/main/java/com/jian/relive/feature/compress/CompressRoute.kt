package com.jian.relive.feature.compress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
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
import com.jian.relive.core.ui.theme.ReliveBlack
import com.jian.relive.core.ui.theme.ReliveBlue500
import com.jian.relive.core.ui.theme.ReliveGray100
import com.jian.relive.core.ui.theme.ReliveGray600
import com.jian.relive.core.ui.theme.ReliveWhite
import com.jian.relive.feature.compress.ui.ImageCompressionSection
import com.jian.relive.feature.compress.ui.VideoCompressionSection
import kotlinx.serialization.Serializable

@Serializable
data object CompressRoute

fun NavGraphBuilder.compressScreen(
    onBack: () -> Unit = {}
) {
    composable<CompressRoute> {
        val viewModel: CompressViewModel = viewModel(factory = CompressViewModel.Factory)
        CompressScreen(
            viewModel = viewModel,
            onBack = onBack
        )
    }
}

@Composable
internal fun CompressScreen(
    viewModel: CompressViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ReliveGray100)
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.CenterStart)
                    .clip(CircleShape)
                    .background(ReliveWhite)
                    .bounceClick(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.compress_back),
                    tint = ReliveBlack
                )
            }
            Text(
                text = stringResource(R.string.compress_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ReliveBlack,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        SecondaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = ReliveGray100,
            contentColor = ReliveBlack,
            divider = {}
        ) {


            Tab(
                selected = selectedTab == 0,
                onClick = { viewModel.selectTab(0) },
                text = {
                    Text(
                        text = stringResource(R.string.compress_tab_image),
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 0) ReliveBlack else ReliveGray600
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { viewModel.selectTab(1) },
                text = {
                    Text(
                        text = stringResource(R.string.compress_tab_video),
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 1) ReliveBlack else ReliveGray600
                    )
                }
            )
        }

        if (selectedTab == 0) {
            ImageCompressionSection(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            VideoCompressionSection(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
