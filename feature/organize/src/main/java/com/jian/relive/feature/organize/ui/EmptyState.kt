package com.jian.relive.feature.organize.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.relive.core.ui.component.CelebrationConfetti
import com.jian.relive.core.ui.sound.rememberSoundEffectHelper
import com.jian.relive.core.ui.theme.ReliveBlack
import com.jian.relive.core.ui.theme.ReliveGray100
import com.jian.relive.core.ui.theme.ReliveGray500
import com.jian.relive.core.ui.theme.ReliveGreen500
import com.jian.relive.core.ui.theme.RelivePrimary
import com.jian.relive.core.ui.theme.ReliveWhite
import com.jian.relive.feature.organize.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 整理流程批次空状态与完成状态交互卡片视图。
 */
@Composable
fun EmptyState(
    onRestart: () -> Unit,
    filterFolder: String = "All",
    filterType: String = "All",
    filterMonth: String = "All",
    onReleaseKept: () -> Unit = {},
    onViewLikes: () -> Unit = {},
    onChangeFilter: () -> Unit = {},
    remainingCount: Int = 0,
    isFullAlbumDone: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isFilterActive = filterFolder != "All" || filterType != "All" || filterMonth != "All"
    val isTrulyDone = isFullAlbumDone || (isFilterActive && remainingCount == 0)

    var showReleaseDialog by remember { mutableStateOf(false) }
    var releasedMessageVisible by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val soundHelper = rememberSoundEffectHelper()
    val density = LocalDensity.current
    val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }

    val cardOffsetY = remember { Animatable(0f) }
    val cardAlpha = remember { Animatable(1f) }
    var isLeaving by remember { mutableStateOf(false) }

    LaunchedEffect(isTrulyDone) {
        if (isTrulyDone) {
            soundHelper.playLike()
        }
    }

    LaunchedEffect(isLeaving, remainingCount) {
        if (isLeaving) {
            delay(400)
            isLeaving = false
            cardAlpha.snapTo(1f)
            cardOffsetY.snapTo(0f)
        }
    }

    if (showReleaseDialog) {
        AlertDialog(
            onDismissRequest = { showReleaseDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.organize_release_dialog_title),
                    color = ReliveBlack
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.organize_release_dialog_content),
                    color = ReliveBlack.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReleaseDialog = false
                        onReleaseKept()
                        scope.launch {
                            releasedMessageVisible = true
                            delay(2000)
                            releasedMessageVisible = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.organize_confirm), color = ReliveBlack)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReleaseDialog = false }) {
                    Text(stringResource(R.string.organize_cancel), color = ReliveGray500)
                }
            },
            containerColor = ReliveWhite
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.68f)
                .padding(bottom = 64.dp)
                .graphicsLayer {
                    translationY = cardOffsetY.value
                    alpha = cardAlpha.value
                },
            colors = CardDefaults.cardColors(containerColor = ReliveWhite),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(ReliveGray100, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isTrulyDone) Icons.Outlined.CheckCircle else Icons.Outlined.Refresh,
                        contentDescription = null,
                        tint = if (isTrulyDone) ReliveGreen500 else ReliveBlack.copy(alpha = 0.4f),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (isTrulyDone) {
                        stringResource(R.string.organize_filter_completed_title)
                    } else {
                        stringResource(R.string.organize_empty_state_title)
                    },
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Serif,
                    color = ReliveBlack.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isTrulyDone) {
                        val folderLabel = if (filterFolder == "All") stringResource(R.string.organize_filter_all) else filterFolder
                        val monthLabel = if (filterMonth != "All") " • $filterMonth" else ""
                        stringResource(R.string.organize_filter_completed_subtitle, "$folderLabel$monthLabel")
                    } else {
                        if (remainingCount > 0) {
                            stringResource(R.string.organize_ready_next_batch, remainingCount)
                        } else {
                            stringResource(R.string.organize_empty_state_subtitle)
                        }
                    },
                    fontSize = 14.sp,
                    color = ReliveBlack.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (isTrulyDone) {
                    Button(
                        onClick = { showReleaseDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RelivePrimary),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (releasedMessageVisible) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = ReliveWhite,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.organize_released_toast), color = ReliveWhite)
                        } else {
                            Text(
                                text = stringResource(R.string.organize_release_kept_button),
                                color = ReliveWhite,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onViewLikes,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, ReliveBlack.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = stringResource(R.string.organize_view_likes_button),
                            color = ReliveBlack,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = onChangeFilter,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.organize_change_filter),
                            color = ReliveGray500
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            if (isLeaving) return@Button
                            isLeaving = true
                            scope.launch {
                                launch {
                                    cardAlpha.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
                                }
                                cardOffsetY.animateTo(-screenHeightPx, tween(260, easing = FastOutSlowInEasing))
                                onRestart()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RelivePrimary),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.organize_continue),
                            color = ReliveWhite,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }

        if (isTrulyDone) {
            CelebrationConfetti(
                intensity = if (isFullAlbumDone) 400 else 250,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
