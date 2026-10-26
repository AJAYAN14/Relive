package com.jian.relive.feature.feed

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jian.relive.core.model.MediaItem
import com.jian.relive.core.ui.component.PredictiveBackWrapper
import com.jian.relive.core.ui.theme.ReliveWhite
import com.jian.relive.feature.feed.ui.FeedActionsColumn
import com.jian.relive.feature.feed.ui.FeedDeleteConfirmDialog
import com.jian.relive.feature.feed.ui.FeedEmptyView
import com.jian.relive.feature.feed.ui.FeedFilterSegmentedRow
import com.jian.relive.feature.feed.ui.FeedHeartBurst
import com.jian.relive.feature.feed.ui.FeedItemInfoOverlay
import com.jian.relive.feature.feed.ui.FeedPauseIndicator
import com.jian.relive.feature.feed.ui.FeedSoundControl
import com.jian.relive.feature.feed.ui.FeedVolumeToastBadge
import com.jian.relive.feature.feed.ui.LiveFeedPlayer
import com.jian.relive.feature.feed.ui.LikesView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object FeedRoute

@Serializable
data object LikesRoute

/**
 * 在导航图中注册实况与视频流媒体页面入口。
 */
fun NavGraphBuilder.feedScreen(
    onBack: () -> Unit = {},
    enterTransition: (androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> androidx.compose.animation.EnterTransition?)? = null,
    exitTransition: (androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> androidx.compose.animation.ExitTransition?)? = null,
    popEnterTransition: (androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> androidx.compose.animation.EnterTransition?)? = null,
    popExitTransition: (androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> androidx.compose.animation.ExitTransition?)? = null
) {
    composable<FeedRoute>(
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        popEnterTransition = popEnterTransition,
        popExitTransition = popExitTransition
    ) {
        val viewModel: FeedViewModel = viewModel(factory = FeedViewModel.Factory)
        FeedRoute(
            viewModel = viewModel,
            onBack = onBack
        )
    }
}

/**
 * 在导航图中注册喜欢相片沉浸式全屏浏览入口。
 */
fun NavGraphBuilder.likesScreen(
    onBack: () -> Unit = {}
) {
    composable<LikesRoute> {
        val viewModel: FeedViewModel = viewModel(factory = FeedViewModel.Factory)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val likedItems = remember(uiState.likedIds, viewModel.allMedia) {
            viewModel.getLikedMediaItems()
        }
        LikesView(
            likedItems = likedItems,
            allMedia = viewModel.allMedia,
            onBack = onBack,
            onUnlike = { viewModel.unlikeItem(it) }
        )
    }
}

@Composable
internal fun FeedRoute(
    viewModel: FeedViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 80.dp
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val isScreenActive = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)

    var isUserPaused by remember { mutableStateOf(false) }
    var isPlaybackReady by remember { mutableStateOf(false) }

    var pendingTrashItem by remember { mutableStateOf<MediaItem?>(null) }
    var heartBurstTargetId by remember { mutableStateOf<Long?>(null) }

    val trashSuccessMsg = stringResource(R.string.feed_delete_success)
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val item = pendingTrashItem
            if (item != null) {
                viewModel.onDeleteConfirmed(item)
                Toast.makeText(context, trashSuccessMsg, Toast.LENGTH_SHORT).show()
            }
        }
        pendingTrashItem = null
    }

    val pagerState = rememberPagerState(
        pageCount = { kotlin.math.max(1, uiState.feedItems.size) }
    )

    LaunchedEffect(pagerState.currentPage) {
        isUserPaused = false
    }

    LaunchedEffect(isScreenActive, pagerState.isScrollInProgress) {
        if (isScreenActive && !pagerState.isScrollInProgress) {
            delay(300)
            isPlaybackReady = true
        } else {
            isPlaybackReady = false
        }
    }

    LaunchedEffect(uiState.filterType) {
        if (uiState.feedItems.isNotEmpty()) {
            pagerState.scrollToPage(0)
        }
    }

    PredictiveBackWrapper(
        isVisible = true,
        onBack = onBack
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(ReliveWhite)
        ) {
            if (uiState.feedItems.isEmpty()) {
                FeedEmptyView(
                    filterType = uiState.filterType,
                    bottomPadding = bottomPadding,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { page ->
                        uiState.feedItems.getOrNull(page)?.id ?: page
                    }
                ) { page ->
                    val item = uiState.feedItems.getOrNull(page) ?: return@VerticalPager
                    val isCurrentPage = page == pagerState.currentPage
                    val isLiked = uiState.likedIds.contains(item.id)
                    val shouldPlay = isCurrentPage && isPlaybackReady && !isUserPaused

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(isCurrentPage) {
                                if (isCurrentPage) {
                                    detectTapGestures(
                                        onTap = {
                                            isUserPaused = !isUserPaused
                                        }
                                    )
                                }
                            }
                    ) {
                        LiveFeedPlayer(
                            item = item,
                            isMuted = uiState.isMuted,
                            shouldPlay = shouldPlay,
                            modifier = Modifier.fillMaxSize()
                        )

                        FeedPauseIndicator(
                            visible = isCurrentPage && isUserPaused,
                            modifier = Modifier.align(Alignment.Center)
                        )

                        FeedItemInfoOverlay(
                            item = item,
                            bottomPadding = bottomPadding,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                        )

                        FeedActionsColumn(
                            item = item,
                            isLiked = isLiked,
                            bottomPadding = bottomPadding,
                            onLikeClick = {
                                viewModel.toggleLike(item)
                                if (!isLiked) {
                                    heartBurstTargetId = item.id
                                    scope.launch {
                                        delay(650)
                                        if (heartBurstTargetId == item.id) {
                                            heartBurstTargetId = null
                                        }
                                    }
                                }
                            },
                            onDeleteClick = {
                                viewModel.setItemToDelete(item)
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .navigationBarsPadding()
                        )

                        FeedHeartBurst(
                            visible = heartBurstTargetId == item.id,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                    .zIndex(10f)
            ) {
                FeedFilterSegmentedRow(
                    selectedFilter = uiState.filterType,
                    onFilterSelected = { viewModel.setFilterType(it) },
                    modifier = Modifier.align(Alignment.Center)
                )

                FeedSoundControl(
                    isMuted = uiState.isMuted,
                    showToast = uiState.showVolumeToast,
                    onToggleMute = { viewModel.toggleMute() },
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            FeedVolumeToastBadge(
                isMuted = uiState.isMuted,
                visible = uiState.showVolumeToast,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 64.dp)
                    .zIndex(15f)
            )

            if (uiState.itemToDelete != null) {
                val target = uiState.itemToDelete!!
                FeedDeleteConfirmDialog(
                    onConfirm = {
                        viewModel.setItemToDelete(null)
                        viewModel.executeDelete(
                            target = target,
                            onIntentSender = { intentSender ->
                                pendingTrashItem = target
                                val request = IntentSenderRequest.Builder(intentSender).build()
                                deleteLauncher.launch(request)
                            },
                            onSuccess = {
                                Toast.makeText(context, trashSuccessMsg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    onDismiss = {
                        viewModel.setItemToDelete(null)
                    }
                )
            }
        }
    }
}
