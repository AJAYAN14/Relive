package com.jian.relive.feature.organize

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jian.relive.core.media.FileOperationManager
import com.jian.relive.core.model.MediaItem
import com.jian.relive.core.model.UndoAction
import com.jian.relive.core.ui.animation.bounceClick
import com.jian.relive.core.ui.component.FullScreenImageViewer
import com.jian.relive.core.ui.component.PredictiveBackWrapper
import com.jian.relive.core.ui.component.SlideText
import com.jian.relive.core.ui.theme.ReliveBlack
import com.jian.relive.core.ui.theme.ReliveGray100
import com.jian.relive.core.ui.theme.ReliveGray500
import com.jian.relive.core.ui.theme.ReliveGreen500
import com.jian.relive.core.ui.theme.RelivePrimary
import com.jian.relive.core.ui.theme.ReliveRed200
import com.jian.relive.core.ui.theme.ReliveRed50
import com.jian.relive.core.ui.theme.ReliveRed600
import com.jian.relive.core.ui.theme.ReliveWhite
import com.jian.relive.feature.organize.model.OrganizeUiState
import com.jian.relive.feature.organize.ui.DeepOrganizeView
import com.jian.relive.feature.organize.ui.DeleteReviewModal
import com.jian.relive.feature.organize.ui.EmptyState
import com.jian.relive.feature.organize.ui.FilterSheet
import com.jian.relive.feature.organize.ui.FolderDropRow
import com.jian.relive.feature.organize.ui.MonthlyListView
import com.jian.relive.feature.organize.ui.MonthlyReviewView
import com.jian.relive.feature.organize.ui.OrganizeCardStack
import com.jian.relive.feature.organize.ui.EdgeGlowEffects
import com.jian.relive.core.ui.sound.rememberSoundEffectHelper
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data object OrganizeRoute

/**
 * 整理主页面覆盖层状态枚举。
 */
private enum class OrganizeOverlay {
    NONE,
    FILTER,
    MONTHLY_LIST,
    MONTHLY_REVIEW,
    DELETE_REVIEW,
    DEEP_ORGANIZE
}

fun NavGraphBuilder.organizeScreen(
    onNavigateToStats: () -> Unit = {},
    onNavigateToClean: () -> Unit = {},
    onNavigateToCompress: () -> Unit = {},
    onNavigateToLikes: () -> Unit = {}
) {
    composable<OrganizeRoute> {
        OrganizeScreen(
            onNavigateToStats = onNavigateToStats,
            onNavigateToClean = onNavigateToClean,
            onNavigateToCompress = onNavigateToCompress,
            onNavigateToLikes = onNavigateToLikes
        )
    }
}

@Composable
internal fun OrganizeScreen(
    modifier: Modifier = Modifier,
    viewModel: OrganizeViewModel = viewModel(factory = OrganizeViewModel.Factory),
    onNavigateToStats: () -> Unit = {},
    onNavigateToClean: () -> Unit = {},
    onNavigateToCompress: () -> Unit = {},
    onNavigateToLikes: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentOverlay by remember { mutableStateOf(OrganizeOverlay.NONE) }
    var reviewMonthItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var pendingMoveItem by remember { mutableStateOf<MediaItem?>(null) }
    var pendingDeletedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var fullScreenViewerItem by remember { mutableStateOf<MediaItem?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val soundHelper = rememberSoundEffectHelper()

    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (pendingDeletedIds.isNotEmpty()) {
                viewModel.onTrashConfirmed(pendingDeletedIds)
                pendingDeletedIds = emptySet()
            }
        }
    }

    PredictiveBackWrapper(
        isVisible = currentOverlay != OrganizeOverlay.NONE,
        onBack = { currentOverlay = OrganizeOverlay.NONE }
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(ReliveWhite)
        ) {
            AnimatedContent(
                targetState = currentOverlay,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "OrganizeOverlayTransition"
            ) { overlay ->
                when (overlay) {
                    OrganizeOverlay.NONE -> {
                        MainOrganizeContent(
                            uiState = uiState,
                            onSwipeLeft = { item ->
                                soundHelper.playDelete(uiState.soundEffectsEnabled)
                                viewModel.handleSwipe(UndoAction.Trash(item))
                            },
                            onSwipeRight = { item ->
                                soundHelper.playLike(uiState.soundEffectsEnabled)
                                viewModel.handleSwipe(UndoAction.Like(item))
                            },
                            onSwipeUp = { item ->
                                soundHelper.playSwipe(uiState.soundEffectsEnabled)
                                viewModel.handleSwipe(UndoAction.Keep(item))
                            },
                            onClearUndoAnimation = { viewModel.clearUndoAnimation() },
                            onUndo = {
                                soundHelper.playSwipe(uiState.soundEffectsEnabled)
                                viewModel.handleUndo()
                            },
                            onLoadNextBatch = {
                                soundHelper.playStart(uiState.soundEffectsEnabled)
                                viewModel.loadNextBatch(playEntryAnimation = true)
                            },
                            onOpenFilter = { currentOverlay = OrganizeOverlay.FILTER },
                            onResetFilter = { viewModel.applyFilter("All", "All", "All") },
                            onOpenMonthly = { currentOverlay = OrganizeOverlay.MONTHLY_LIST },
                            onOpenTrash = { currentOverlay = OrganizeOverlay.DELETE_REVIEW },
                            onOpenDeepOrganize = { currentOverlay = OrganizeOverlay.DEEP_ORGANIZE },
                            onNavigateToStats = onNavigateToStats,
                            onNavigateToLikes = onNavigateToLikes,
                            onReleaseKept = { viewModel.releaseKeptMedia() },
                            onCardLongClick = { fullScreenViewerItem = it },
                            onDropTargetSelected = { item, targetId ->
                                if (targetId == "new_folder") {
                                    pendingMoveItem = item
                                    showNewFolderDialog = true
                                } else {
                                    scope.launch {
                                        val rawPath = uiState.folderDropTargets.find { it.id == targetId }?.path ?: targetId
                                        val targetFolder = rawPath.trim().trimEnd('/') + "/"
                                        val originalFile = File(item.path.orEmpty())
                                        val parent = originalFile.parentFile?.absolutePath.orEmpty()
                                        val intentSender = FileOperationManager.moveMedia(context, listOf(item), targetFolder)
                                        if (intentSender != null) {
                                            viewModel.handleSwipe(UndoAction.Move(item, parent, targetFolder))
                                            intentSenderLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                                        } else {
                                            viewModel.handleSwipe(UndoAction.Move(item, parent, targetFolder))
                                        }
                                    }
                                }
                            }
                        )
                    }

                    OrganizeOverlay.FILTER -> {
                        FilterSheet(
                            currentFolder = uiState.filterFolder,
                            currentType = uiState.filterType,
                            currentMonth = uiState.filterMonth,
                            folderList = uiState.folderList,
                            onApply = { folder, type, month ->
                                viewModel.applyFilter(folder, type, month)
                                currentOverlay = OrganizeOverlay.NONE
                            },
                            onBack = { currentOverlay = OrganizeOverlay.NONE },
                            onSelectMonth = { currentOverlay = OrganizeOverlay.MONTHLY_LIST },
                            onClearMonth = {
                                viewModel.applyFilter(uiState.filterFolder, uiState.filterType, "All")
                            }
                        )
                    }

                    OrganizeOverlay.MONTHLY_LIST -> {
                        MonthlyListView(
                            allMedia = uiState.allMedia,
                            processedIds = uiState.processedIds,
                            currentMonth = uiState.filterMonth,
                            onMonthClick = { monthKey, items ->
                                reviewMonthItems = items
                                currentOverlay = OrganizeOverlay.MONTHLY_REVIEW
                            },
                            onAllClick = {
                                viewModel.applyFilter(uiState.filterFolder, uiState.filterType, "All")
                                currentOverlay = OrganizeOverlay.NONE
                            },
                            onBack = { currentOverlay = OrganizeOverlay.NONE }
                        )
                    }

                    OrganizeOverlay.MONTHLY_REVIEW -> {
                        MonthlyReviewView(
                            monthItems = reviewMonthItems,
                            processedIds = uiState.processedIds,
                            pendingTrashCount = uiState.pendingTrashCount,
                            muteVideos = !uiState.soundEffectsEnabled,
                            onTrashClick = { currentOverlay = OrganizeOverlay.DELETE_REVIEW },
                            onSwipe = { item, isTrash, isLike ->
                                if (isTrash) {
                                    soundHelper.playDelete(uiState.soundEffectsEnabled)
                                } else if (isLike) {
                                    soundHelper.playLike(uiState.soundEffectsEnabled)
                                } else {
                                    soundHelper.playSwipe(uiState.soundEffectsEnabled)
                                }
                                val action = when {
                                    isTrash -> UndoAction.Trash(item)
                                    isLike -> UndoAction.Like(item)
                                    else -> UndoAction.Keep(item)
                                }
                                viewModel.handleSwipe(action)
                            },
                            onUndo = { _, onStackUpdated, onRestored ->
                                soundHelper.playSwipe(uiState.soundEffectsEnabled)
                                val lastAction = uiState.undoStack.lastOrNull()
                                if (lastAction != null) {
                                    onRestored(lastAction.item)
                                    onStackUpdated(uiState.undoStack.dropLast(1))
                                    viewModel.handleUndo()
                                }
                            },
                            onBack = { currentOverlay = OrganizeOverlay.MONTHLY_LIST }
                        )
                    }

                    OrganizeOverlay.DELETE_REVIEW -> {
                        DeleteReviewModal(
                            pendingTrashIds = uiState.pendingTrashIds,
                            allMedia = uiState.allMedia,
                            onConfirm = { idsToDelete ->
                                currentOverlay = OrganizeOverlay.NONE
                                scope.launch {
                                    pendingDeletedIds = idsToDelete
                                    val urisToDelete = uiState.allMedia
                                        .filter { idsToDelete.contains(it.id) }
                                        .map { it.uri }
                                    val intentSender = FileOperationManager.deleteMedia(
                                        context = context,
                                        uris = urisToDelete,
                                        moveToTrash = true
                                    )
                                    if (intentSender != null) {
                                        intentSenderLauncher.launch(
                                            IntentSenderRequest.Builder(intentSender).build()
                                        )
                                    } else {
                                        viewModel.onTrashConfirmed(idsToDelete)
                                    }
                                }
                            },
                            onCancel = { currentOverlay = OrganizeOverlay.NONE }
                        )
                    }

                    OrganizeOverlay.DEEP_ORGANIZE -> {
                        DeepOrganizeView(
                            allMedia = uiState.allMedia,
                            onBack = { currentOverlay = OrganizeOverlay.NONE },
                            onSimilarClick = {
                                currentOverlay = OrganizeOverlay.NONE
                                onNavigateToClean()
                            },
                            onLowQualityClick = {
                                currentOverlay = OrganizeOverlay.NONE
                                onNavigateToClean()
                            },
                            onVideoCompressClick = {
                                currentOverlay = OrganizeOverlay.NONE
                                onNavigateToCompress()
                            },
                            onImageCompressClick = {
                                currentOverlay = OrganizeOverlay.NONE
                                onNavigateToCompress()
                            }
                        )
                    }
                }
            }

            if (showNewFolderDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showNewFolderDialog = false
                        newFolderName = ""
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.organize_new_folder),
                            fontWeight = FontWeight.Bold,
                            color = ReliveBlack
                        )
                    },
                    text = {
                        OutlinedTextField(
                            value = newFolderName,
                            onValueChange = { newFolderName = it },
                            placeholder = { Text(stringResource(R.string.organize_folder_name_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val name = newFolderName.trim()
                                val item = pendingMoveItem
                                if (name.isNotEmpty() && item != null) {
                                    scope.launch {
                                        val targetFolder = "Pictures/$name/"
                                        val originalFile = File(item.path.orEmpty())
                                        val parent = originalFile.parentFile?.absolutePath.orEmpty()
                                        val intentSender = FileOperationManager.moveMedia(context, listOf(item), targetFolder)
                                        if (intentSender != null) {
                                            viewModel.handleSwipe(UndoAction.Move(item, parent, targetFolder, isNewFolder = true))
                                            intentSenderLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                                        } else {
                                            viewModel.handleSwipe(UndoAction.Move(item, parent, targetFolder, isNewFolder = true))
                                        }
                                    }
                                }
                                showNewFolderDialog = false
                                newFolderName = ""
                                pendingMoveItem = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RelivePrimary)
                        ) {
                            Text(stringResource(R.string.organize_create), color = ReliveWhite)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showNewFolderDialog = false
                                newFolderName = ""
                                pendingMoveItem = null
                            }
                        ) {
                            Text(stringResource(R.string.organize_cancel), color = ReliveGray500)
                        }
                    }
                )
            }

            if (fullScreenViewerItem != null) {
                FullScreenImageViewer(
                    item = fullScreenViewerItem!!,
                    onDismiss = { fullScreenViewerItem = null }
                )
            }
        }
    }
}

/**
 * 主卡片整理界面内容。
 */
@Composable
private fun MainOrganizeContent(
    uiState: OrganizeUiState,
    onSwipeLeft: (MediaItem) -> Unit,
    onSwipeRight: (MediaItem) -> Unit,
    onSwipeUp: (MediaItem) -> Unit,
    onClearUndoAnimation: () -> Unit,
    onUndo: () -> Unit,
    onLoadNextBatch: () -> Unit,
    onOpenFilter: () -> Unit,
    onResetFilter: () -> Unit = {},
    onOpenMonthly: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenDeepOrganize: () -> Unit = {},
    onNavigateToStats: () -> Unit,
    onNavigateToLikes: () -> Unit,
    onReleaseKept: () -> Unit,
    onCardLongClick: (MediaItem) -> Unit,
    onDropTargetSelected: (MediaItem, String) -> Unit
) {
    var isFolderDropMode by remember { mutableStateOf(false) }
    var activeDropTargetId by remember { mutableStateOf<String?>(null) }
    var folderPositions by remember { mutableStateOf<Map<String, Rect>>(emptyMap()) }
    var topCardOffset by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = Modifier.fillMaxSize()) {
        EdgeGlowEffects(
            offset = topCardOffset,
            isUndoAnimating = uiState.isUndoAnimating
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 12.dp, start = 20.dp, end = 20.dp, bottom = 12.dp)
            ) {
                if (uiState.displayMedia.isNotEmpty()) {
                    val denominator = uiState.currentBatchTotal
                    val remaining = uiState.displayMedia.size
                    val done = (denominator - remaining).coerceAtLeast(0)

                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SlideText(
                                targetValue = done,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = ReliveBlack.copy(alpha = 0.6f)
                            )
                            Text(
                                text = " / $denominator",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ReliveBlack.copy(alpha = 0.6f),
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (uiState.filterFolder != "All" || uiState.filterType != "All" || uiState.filterMonth != "All") {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(ReliveGray100)
                                    .bounceClick(onClick = onResetFilter)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                val textList = mutableListOf<String>()
                                if (uiState.filterFolder != "All") textList.add(uiState.filterFolder)
                                if (uiState.filterType != "All") {
                                    textList.add(
                                        when (uiState.filterType) {
                                            "Image" -> stringResource(R.string.organize_filter_image)
                                            "Video" -> stringResource(R.string.organize_filter_video)
                                            "Live" -> stringResource(R.string.organize_filter_live)
                                            else -> uiState.filterType
                                        }
                                    )
                                }
                                if (uiState.filterMonth != "All") textList.add(uiState.filterMonth)

                                Text(
                                    text = textList.joinToString(" • "),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ReliveBlack.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = null,
                                    tint = ReliveBlack.copy(alpha = 0.6f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    androidx.compose.animation.AnimatedVisibility(visible = uiState.pendingTrashCount > 0) {
                        Button(
                            onClick = onOpenTrash,
                            colors = ButtonDefaults.buttonColors(containerColor = ReliveRed50),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ReliveRed200),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.organize_trash_clean),
                                tint = ReliveRed600,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            SlideText(
                                targetValue = uiState.pendingTrashCount,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = ReliveRed600
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.displayMedia.isEmpty() && !uiState.isLoading) {
                    EmptyState(
                        onRestart = onLoadNextBatch,
                        filterFolder = uiState.filterFolder,
                        filterType = uiState.filterType,
                        filterMonth = uiState.filterMonth,
                        onReleaseKept = onReleaseKept,
                        onViewLikes = onNavigateToLikes,
                        onChangeFilter = onOpenFilter,
                        remainingCount = uiState.remainingCount,
                        isFullAlbumDone = uiState.isFullAlbumDone,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    OrganizeCardStack(
                        items = uiState.displayMedia,
                        undoAnimationTarget = uiState.undoAnimationTarget,
                        modifier = Modifier.fillMaxSize(),
                        onSwipeLeft = onSwipeLeft,
                        onSwipeRight = onSwipeRight,
                        onSwipeUp = onSwipeUp,
                        onLongClick = onCardLongClick,
                        onClearUndoAnimation = onClearUndoAnimation,
                        onDragOffsetChange = { topCardOffset = it }
                    )
                }
            }
        }

        FolderDropRow(
            visible = isFolderDropMode,
            folders = uiState.folderDropTargets,
            activeDropTargetId = activeDropTargetId,
            onFolderPositionsChanged = { positions ->
                folderPositions = positions
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 24.dp, start = 20.dp)
        ) {
            if (uiState.comboCount > 1) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(ReliveBlack.copy(alpha = 0.8f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.organize_combo_count, uiState.comboCount),
                        color = ReliveWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            AnimatedVisibility(
                visible = uiState.canUndo,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                Button(
                    onClick = onUndo,
                    colors = ButtonDefaults.buttonColors(containerColor = ReliveWhite.copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(24.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ReliveBlack.copy(alpha = 0.1f)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Replay,
                        contentDescription = stringResource(R.string.organize_undo),
                        tint = ReliveBlack.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.organize_undo),
                        color = ReliveBlack.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 20.dp)
        ) {
            AnimatedVisibility(
                visible = !isFolderDropMode,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                Button(
                    onClick = onOpenFilter,
                    colors = ButtonDefaults.buttonColors(containerColor = ReliveWhite.copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(24.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ReliveBlack.copy(alpha = 0.1f)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FilterList,
                        contentDescription = stringResource(R.string.organize_filter_title),
                        tint = ReliveBlack.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
