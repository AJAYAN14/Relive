package com.jian.relive.feature.clean

import android.app.Application
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jian.relive.core.media.FileOperationManager
import com.jian.relive.core.media.MediaAnalysisManager
import com.jian.relive.core.media.MediaRepository
import com.jian.relive.core.model.MediaItem
import com.jian.relive.feature.clean.model.CleanTab
import com.jian.relive.feature.clean.model.CleanUiState
import com.jian.relive.feature.clean.model.LowQualityItem
import com.jian.relive.feature.clean.model.LowQualityReason
import com.jian.relive.feature.clean.model.SimilarPhotoGroup
import com.jian.relive.feature.clean.model.UndoCleanAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import kotlin.math.abs

/**
 * 清理功能业务状态管理驱动器。
 *
 * 响应式编排端侧 dHash 相似照片排重聚类与低质照片多维度检测，管理批量清理与回滚撤销事务。
 */
class CleanViewModel(
    application: Application,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    private val mediaRepository = MediaRepository()

    private val _uiState = MutableStateFlow(CleanUiState())
    val uiState: StateFlow<CleanUiState> = _uiState.asStateFlow()

    private val ignoredIds = Collections.synchronizedSet(HashSet<Long>())
    private var scanJob: Job? = null
    private var lastUndoAction: UndoCleanAction? = null

    init {
        MediaAnalysisManager.init(context)
        loadAndStartScan()
    }

    /**
     * 切换当前激活的清理类型分页。
     */
    fun selectTab(tab: CleanTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    /**
     * 加载本地媒体并触发分析扫描。
     */
    fun loadAndStartScan() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanProgress = 0f) }

            val allMedia = mediaRepository.loadMedia(context)
            val photos = allMedia.filter { it.type == "photo" }

            if (photos.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanProgress = 1f,
                        similarGroups = emptyList(),
                        lowQualityItems = emptyList()
                    )
                }
                return@launch
            }

            MediaAnalysisManager.startScan(context, photos)

            launch {
                MediaAnalysisManager.isScanning.collect { scanning ->
                    _uiState.update { it.copy(isScanning = scanning) }
                    if (!scanning) {
                        refreshGroups(photos, force = true)
                    }
                }
            }

            launch {
                MediaAnalysisManager.progress.collect { progress ->
                    _uiState.update { it.copy(scanProgress = progress) }
                    if (progress in 0.05f..0.99f) {
                        refreshGroups(photos, force = false)
                    }
                }
            }

            refreshGroups(photos, force = true)
        }
    }

    /**
     * 停止正在进行的分析扫描。
     */
    fun stopScan() {
        scanJob?.cancel()
        _uiState.update { it.copy(isScanning = false) }
    }

    private suspend fun refreshGroups(photos: List<MediaItem>, force: Boolean) = withContext(defaultDispatcher) {
        val currentIgnored = ignoredIds.toSet()

        val groups = findSimilarPhotoGroups(photos, currentIgnored)
        val lowQuality = findLowQualityItems(photos, currentIgnored)

        _uiState.update {
            it.copy(
                similarGroups = groups,
                lowQualityItems = lowQuality
            )
        }
    }

    private fun findSimilarPhotoGroups(
        sortedPhotos: List<MediaItem>,
        ignored: Set<Long>
    ): List<SimilarPhotoGroup> {
        val groups = mutableListOf<SimilarPhotoGroup>()
        val visited = HashSet<Long>(ignored)
        val photos = sortedPhotos.sortedByDescending { it.dateAdded }

        for (i in photos.indices) {
            val current = photos[i]
            if (visited.contains(current.id)) continue

            val currentHash = MediaAnalysisManager.getAnalysis(current.id)?.dHash ?: continue
            if (currentHash == 0L) continue

            for (j in i + 1 until photos.size) {
                val next = photos[j]
                if (visited.contains(next.id)) continue

                if (abs(current.dateAdded - next.dateAdded) > TIME_WINDOW_SEC) {
                    break
                }

                val nextHash = MediaAnalysisManager.getAnalysis(next.id)?.dHash ?: continue
                if (hammingDistance(currentHash, nextHash) <= SIMILARITY_THRESHOLD) {
                    val isLiveVsStatic = current.isLivePhoto != next.isLivePhoto
                    val isSameMoment = abs(current.dateAdded - next.dateAdded) <= 2
                    if (isLiveVsStatic && isSameMoment) {
                        continue
                    }

                    groups.add(SimilarPhotoGroup(id = current.id, photos = listOf(current, next)))
                    visited.add(current.id)
                    visited.add(next.id)
                    break
                }
            }
        }
        return groups
    }

    private fun findLowQualityItems(
        photos: List<MediaItem>,
        ignored: Set<Long>
    ): List<LowQualityItem> {
        val results = mutableListOf<LowQualityItem>()

        for (photo in photos) {
            if (ignored.contains(photo.id)) continue

            val staticReason = checkStaticQuality(photo)
            if (staticReason != null) {
                results.add(LowQualityItem(item = photo, reason = staticReason))
                continue
            }

            val analysis = MediaAnalysisManager.getAnalysis(photo.id) ?: continue
            val dynamicReason = checkDynamicQuality(photo, analysis)
            if (dynamicReason != null) {
                results.add(LowQualityItem(item = photo, reason = dynamicReason))
            }
        }
        return results
    }

    private fun checkStaticQuality(item: MediaItem): LowQualityReason? {
        if (item.size < MIN_FILE_SIZE_BYTES) {
            return LowQualityReason.SMALL_FILE
        }
        if (item.width > 0 && item.height > 0 && item.width * item.height < MIN_PIXELS_THRESHOLD) {
            return LowQualityReason.LOW_RESOLUTION
        }
        return null
    }

    private fun checkDynamicQuality(
        item: MediaItem,
        analysis: MediaAnalysisManager.AnalysisResult
    ): LowQualityReason? {
        if (analysis.solidColorStdDev < SOLID_COLOR_STD_DEV_THRESHOLD) {
            return LowQualityReason.SOLID_COLOR
        }

        val isScreenshot = item.displayName.contains("Screenshot", ignoreCase = true) ||
            item.displayName.contains("截屏", ignoreCase = true) ||
            item.bucketName.contains("Screenshot", ignoreCase = true) ||
            item.bucketName.contains("截屏", ignoreCase = true)

        if (!isScreenshot) {
            if (analysis.exposureAvg < UNDEREXPOSED_THRESHOLD) {
                return LowQualityReason.UNDEREXPOSED
            }
            if (analysis.exposureAvg > OVEREXPOSED_THRESHOLD) {
                return LowQualityReason.OVEREXPOSED
            }
            if (analysis.blurVariance < BLUR_VARIANCE_THRESHOLD) {
                return LowQualityReason.BLUR
            }
        }

        if (analysis.hasClosedEyes) {
            return LowQualityReason.CLOSED_EYES
        }

        return null
    }

    private fun hammingDistance(hash1: Long, hash2: Long): Int {
        return java.lang.Long.bitCount(hash1 xor hash2)
    }

    /**
     * 记录用户在相似照片组中的决策结果。
     */
    fun onSimilarDecision(deletedIds: Set<Long>, group: SimilarPhotoGroup) {
        val allIds = group.photos.map { it.id }.toSet()
        ignoredIds.addAll(allIds)

        lastUndoAction = UndoCleanAction.SimilarDecision(
            deletedIds = deletedIds,
            processedGroup = group
        )

        _uiState.update { state ->
            val updatedGroups = state.similarGroups.filter { it.id != group.id }
            val newTrash = state.pendingTrashIds + deletedIds
            state.copy(
                similarGroups = updatedGroups,
                pendingTrashIds = newTrash,
                canUndo = true
            )
        }
    }

    /**
     * 响应低质卡片滑动处理。
     */
    fun onLowQualitySwipe(item: LowQualityItem, wasDeleted: Boolean) {
        ignoredIds.add(item.item.id)

        lastUndoAction = UndoCleanAction.LowQualitySwipe(
            item = item,
            wasDeleted = wasDeleted
        )

        _uiState.update { state ->
            val updatedItems = state.lowQualityItems.filter { it.item.id != item.item.id }
            val newTrash = if (wasDeleted) state.pendingTrashIds + item.item.id else state.pendingTrashIds
            state.copy(
                lowQualityItems = updatedItems,
                pendingTrashIds = newTrash,
                canUndo = true
            )
        }
    }

    /**
     * 撤销上一步操作。
     */
    fun undoLastAction() {
        val action = lastUndoAction ?: return
        lastUndoAction = null

        when (action) {
            is UndoCleanAction.SimilarDecision -> {
                ignoredIds.removeAll(action.processedGroup.photos.map { it.id }.toSet())
                _uiState.update { state ->
                    val restoredGroups = listOf(action.processedGroup) + state.similarGroups
                    val restoredTrash = state.pendingTrashIds - action.deletedIds
                    state.copy(
                        similarGroups = restoredGroups,
                        pendingTrashIds = restoredTrash,
                        canUndo = false
                    )
                }
            }
            is UndoCleanAction.LowQualitySwipe -> {
                ignoredIds.remove(action.item.item.id)
                _uiState.update { state ->
                    val restoredItems = listOf(action.item) + state.lowQualityItems
                    val restoredTrash = if (action.wasDeleted) {
                        state.pendingTrashIds - action.item.item.id
                    } else {
                        state.pendingTrashIds
                    }
                    state.copy(
                        lowQualityItems = restoredItems,
                        pendingTrashIds = restoredTrash,
                        canUndo = false
                    )
                }
            }
        }
    }

    /**
     * 开启或关闭批量删除二次确认弹窗。
     */
    fun setConfirmingDelete(show: Boolean) {
        _uiState.update { it.copy(isConfirmingDelete = show) }
    }

    /**
     * 执行将待清理照片移入系统废纸篓。
     */
    fun executeTrashPending(onIntentSender: (IntentSender) -> Unit) {
        val trashIds = _uiState.value.pendingTrashIds
        if (trashIds.isEmpty()) return

        viewModelScope.launch {
            val allMedia = mediaRepository.loadMedia(context)
            val urisToTrash = allMedia.filter { trashIds.contains(it.id) }.map { it.uri }

            val intentSender = FileOperationManager.deleteMedia(
                context = context,
                uris = urisToTrash,
                moveToTrash = true
            )

            if (intentSender != null) {
                onIntentSender(intentSender)
            } else {
                onTrashConfirmed()
            }
        }
    }

    /**
     * 确认删除完成后的状态清空。
     */
    fun onTrashConfirmed() {
        _uiState.update {
            it.copy(
                pendingTrashIds = emptySet(),
                canUndo = false,
                isConfirmingDelete = false
            )
        }
        viewModelScope.launch {
            mediaRepository.loadMedia(context)
        }
    }

    companion object {
        /**
         * 显式构造工厂：经 CreationExtras 取 Application 直接实例化，
         * 规避默认工厂对多参数构造签名的反射匹配失败。
         */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                CleanViewModel(app)
            }
        }

        private const val SIMILARITY_THRESHOLD = 7
        private const val TIME_WINDOW_SEC = 120
        private const val MIN_FILE_SIZE_BYTES = 50 * 1024L
        private const val MIN_PIXELS_THRESHOLD = 500_000
        private const val SOLID_COLOR_STD_DEV_THRESHOLD = 5.0
        private const val UNDEREXPOSED_THRESHOLD = 40
        private const val OVEREXPOSED_THRESHOLD = 220
        private const val BLUR_VARIANCE_THRESHOLD = 100.0
    }
}
