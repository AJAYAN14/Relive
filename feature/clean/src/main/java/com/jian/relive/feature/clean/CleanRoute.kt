package com.jian.relive.feature.clean

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import com.jian.relive.core.ui.component.PredictiveBackWrapper
import com.jian.relive.core.ui.theme.ReliveBlack
import com.jian.relive.core.ui.theme.ReliveGray100
import com.jian.relive.core.ui.theme.ReliveGray600
import com.jian.relive.core.ui.theme.ReliveWhite
import com.jian.relive.feature.clean.model.CleanTab
import com.jian.relive.feature.clean.ui.CleanConfirmTrashDialog
import com.jian.relive.feature.clean.ui.LowQualitySection
import com.jian.relive.feature.clean.ui.SimilarPhotosSection
import kotlinx.serialization.Serializable

@Serializable
data object CleanRoute

/**
 * 在导航图中注册清理功能页面入口。
 */
fun NavGraphBuilder.cleanScreen(
    onBack: () -> Unit = {}
) {
    composable<CleanRoute> {
        val viewModel: CleanViewModel = viewModel(factory = CleanViewModel.Factory)
        CleanRoute(
            viewModel = viewModel,
            onBack = onBack
        )
    }
}

@Composable
internal fun CleanRoute(
    viewModel: CleanViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val trashLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onTrashConfirmed()
        }
    }

    PredictiveBackWrapper(
        isVisible = true,
        onBack = onBack
    ) {
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
                        contentDescription = stringResource(R.string.clean_stop),
                        tint = ReliveBlack
                    )
                }

                Text(
                    text = stringResource(R.string.clean_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ReliveBlack,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            SecondaryTabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = ReliveGray100,
                contentColor = ReliveBlack,
                divider = {}
            ) {
                CleanTab.entries.forEach { tab ->
                    val isSelected = uiState.selectedTab == tab
                    Tab(
                        selected = isSelected,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Text(
                                text = stringResource(tab.titleRes),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) ReliveBlack else ReliveGray600
                            )
                        }
                    )
                }
            }

            when (uiState.selectedTab) {
                CleanTab.SIMILAR -> {
                    SimilarPhotosSection(
                        viewModel = viewModel,
                        similarGroups = uiState.similarGroups,
                        isScanning = uiState.isScanning,
                        scanProgress = uiState.scanProgress,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                CleanTab.LOW_QUALITY -> {
                    LowQualitySection(
                        viewModel = viewModel,
                        lowQualityItems = uiState.lowQualityItems,
                        pendingTrashCount = uiState.pendingTrashIds.size,
                        canUndo = uiState.canUndo,
                        isScanning = uiState.isScanning,
                        scanProgress = uiState.scanProgress,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (uiState.isConfirmingDelete) {
            CleanConfirmTrashDialog(
                count = uiState.pendingTrashIds.size,
                onConfirm = {
                    viewModel.setConfirmingDelete(false)
                    viewModel.executeTrashPending { intentSender ->
                        val request = IntentSenderRequest.Builder(intentSender).build()
                        trashLauncher.launch(request)
                    }
                },
                onDismiss = {
                    viewModel.setConfirmingDelete(false)
                }
            )
        }
    }
}
