package com.jian.relive.feature.organize

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jian.relive.core.common.preferences.PreferencesManager
import com.jian.relive.core.media.MediaAnalysisManager
import com.jian.relive.core.media.MediaRepository
import com.jian.relive.core.model.AppStats
import com.jian.relive.core.model.MediaItem
import com.jian.relive.core.model.UndoAction
import com.jian.relive.core.model.UndoAnimationTarget
import com.jian.relive.feature.organize.model.FilterFolderData
import com.jian.relive.feature.organize.model.FolderData
import com.jian.relive.feature.organize.model.OrganizeUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

/**
 * 相册整理主流程状态管理驱动器。
 *
 * 负责批次照片过滤编排、手势动作处理、撤销回滚事务栈管理与统计偏好持久化。
 */
class OrganizeViewModel(
    application: Application,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    private val mediaRepository = MediaRepository()
    private val prefs = PreferencesManager(context)

    private val _uiState = MutableStateFlow(OrganizeUiState())
    val uiState: StateFlow<OrganizeUiState> = _uiState.asStateFlow()

    private var allMediaList: List<MediaItem> = emptyList()
    private var lastActionTime: Long = 0L
    private var lastUndoTime: Long = 0L
    private var similarCheckJob: Job? = null
    private var lastCheckedIds: Set<Long> = emptySet()

    init {
        MediaAnalysisManager.init(context)
        loadInitialPreferences()
        observeMediaRepository()
        loadMedia()
    }

    /**
     * 加载设备图库媒体数据。
     */
    fun loadMedia() {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoading = true) }
            mediaRepository.loadMedia(context)
        }
    }

    private fun loadInitialPreferences() {
        val initialStats = prefs.stats
        val initialBatchSize = prefs.batchSize
        val initialQueueOrder = prefs.queueOrder
        val initialFilterFolder = prefs.filterFolder
        val initialFilterType = prefs.filterType
        val initialFilterMonth = prefs.filterMonth
        val initialPendingTrashCount = prefs.pendingTrashIds.size
        val initialFolderUsage = prefs.folderUsage.toMap()
        val initialSound = prefs.soundEffectsEnabled
        val initialHaptics = prefs.hapticsEnabled
        val initialProcessedIds = prefs.processedIds.toSet()
        val initialPendingTrashIds = prefs.pendingTrashIds.toSet()

        _uiState.update {
            it.copy(
                stats = initialStats,
                batchSize = initialBatchSize,
                queueOrder = initialQueueOrder,
                filterFolder = initialFilterFolder,
                filterType = initialFilterType,
                filterMonth = initialFilterMonth,
                pendingTrashCount = initialPendingTrashCount,
                folderUsage = initialFolderUsage,
                soundEffectsEnabled = initialSound,
                hapticsEnabled = initialHaptics,
                processedIds = initialProcessedIds,
                pendingTrashIds = initialPendingTrashIds
            )
        }
    }

    private fun observeMediaRepository() {
        viewModelScope.launch {
            mediaRepository.mediaItems.collect { items ->
                allMediaList = sortMediaList(items, _uiState.value.queueOrder)
                val folders = calculateFolderList(items)
                val dropTargets = calculateFolderDropTargets(items, _uiState.value.folderUsage)
                _uiState.update {
                    it.copy(
                        allMedia = allMediaList,
                        folderList = folders,
                        folderDropTargets = dropTargets,
                        processedIds = prefs.processedIds.toSet(),
                        pendingTrashIds = prefs.pendingTrashIds.toSet()
                    )
                }

                if (_uiState.value.isLoading || _uiState.value.displayMedia.isEmpty()) {
                    loadNextBatch(initial = true)
                }
            }
        }
    }

    private fun sortMediaList(items: List<MediaItem>, order: String): List<MediaItem> {
        return if (order == "time") {
            items.sortedByDescending { it.dateAdded }
        } else {
            items.shuffled()
        }
    }

    private fun calculateFolderList(items: List<MediaItem>): List<FilterFolderData> {
        val groups = items.groupBy { it.bucketName }
        return groups.map { (name, list) ->
            FilterFolderData(
                name = name,
                count = list.size,
                previews = list.take(4)
            )
        }.sortedByDescending { it.count }
    }

    private fun calculateFolderDropTargets(
        items: List<MediaItem>,
        usage: Map<String, Int>
    ): List<FolderData> {
        val groups = items.groupBy { it.bucketName }
        return groups.map { (name, list) ->
            val relFolder = list.firstOrNull()?.path?.let { File(it).parentFile?.absolutePath } ?: "Pictures/$name/"
            FolderData(
                id = name,
                name = name,
                path = relFolder,
                count = list.size
            )
        }.sortedWith(
            compareByDescending<FolderData> { usage[it.id] ?: 0 }
                .thenByDescending { it.count }
        )
    }

    /**
     * 响应卡片滑动动作并执行对应事务。
     *
     * @param action 撤销动作实体（包含媒体条目与滑动方向）。
     */
    fun handleSwipe(action: UndoAction) {
        val item = action.item
        val currentState = _uiState.value
        val now = System.currentTimeMillis()

        val newCombo = if (now - lastActionTime < 3000L) {
            currentState.comboCount + 1
        } else {
            1
        }
        lastActionTime = now

        val updatedDisplayMedia = currentState.displayMedia.toMutableList()
        if (updatedDisplayMedia.isNotEmpty()) {
            updatedDisplayMedia.removeAt(0)
        }

        val processed = prefs.processedIds
        processed.add(item.id)
        prefs.processedIds = processed

        var currentStats = currentState.stats
        var pendingTrashCount = currentState.pendingTrashCount
        var updatedFolderUsage = currentState.folderUsage

        when (action) {
            is UndoAction.Trash -> {
                val pending = prefs.pendingTrashIds
                pending.add(item.id)
                val liveVideoId = item.livePhotoVideoId
                if (liveVideoId != null) {
                    pending.add(liveVideoId)
                }
                prefs.pendingTrashIds = pending
                pendingTrashCount = pending.size

                currentStats = currentStats.copy(
                    processed = currentStats.processed + 1,
                    trashed = currentStats.trashed + 1,
                    savedSize = currentStats.savedSize + item.size
                )
                prefs.stats = currentStats
            }

            is UndoAction.Like -> {
                val liked = prefs.likedIds
                liked.add(item.id)
                prefs.likedIds = liked

                currentStats = currentStats.copy(
                    processed = currentStats.processed + 1,
                    skipped = currentStats.skipped + 1
                )
                prefs.stats = currentStats
            }

            is UndoAction.Keep -> {
                currentStats = currentStats.copy(
                    processed = currentStats.processed + 1,
                    skipped = currentStats.skipped + 1
                )
                prefs.stats = currentStats
            }

            is UndoAction.Move -> {
                val moved = prefs.movedIds
                moved.add(item.id)
                prefs.movedIds = moved

                val mutableUsage = prefs.folderUsage.toMutableMap()
                val targetBucketId = File(action.targetFolder).name
                mutableUsage[targetBucketId] = (mutableUsage[targetBucketId] ?: 0) + 1
                prefs.folderUsage = mutableUsage
                updatedFolderUsage = mutableUsage

                currentStats = currentStats.copy(
                    processed = currentStats.processed + 1,
                    skipped = currentStats.skipped + 1
                )
                prefs.stats = currentStats
            }
        }

        val updatedUndoStack = currentState.undoStack + action
        val nextTopCard = updatedDisplayMedia.firstOrNull()
        val nextTopGroup = if (nextTopCard != null) {
            currentState.similarGroups.find { group -> group.any { it.id == nextTopCard.id } }
        } else {
            null
        }

        _uiState.update {
            it.copy(
                displayMedia = updatedDisplayMedia,
                undoStack = updatedUndoStack,
                comboCount = newCombo,
                stats = currentStats,
                pendingTrashCount = pendingTrashCount,
                processedIds = processed.toSet(),
                pendingTrashIds = prefs.pendingTrashIds.toSet(),
                folderUsage = updatedFolderUsage,
                topCardSimilarGroup = nextTopGroup
            )
        }

        if (updatedDisplayMedia.isEmpty()) {
            loadNextBatch()
        }
    }

    /**
     * 撤销上一次卡片处理动作。
     */
    fun handleUndo() {
        val now = System.currentTimeMillis()
        if (now - lastUndoTime < 300L) return
        lastUndoTime = now

        val currentState = _uiState.value
        if (currentState.undoStack.isEmpty()) return

        val action = currentState.undoStack.last()
        val remainingUndoStack = currentState.undoStack.dropLast(1)
        val item = action.item

        val restoredDisplayMedia = listOf(item) + currentState.displayMedia
        val animTarget = UndoAnimationTarget(item.id, action.direction, System.currentTimeMillis())

        val processed = prefs.processedIds
        processed.remove(item.id)
        prefs.processedIds = processed

        var currentStats = currentState.stats
        var pendingTrashCount = currentState.pendingTrashCount
        var updatedFolderUsage = currentState.folderUsage

        when (action) {
            is UndoAction.Trash -> {
                val pending = prefs.pendingTrashIds
                pending.remove(item.id)
                val liveVideoId = item.livePhotoVideoId
                if (liveVideoId != null) {
                    pending.remove(liveVideoId)
                }
                prefs.pendingTrashIds = pending
                pendingTrashCount = pending.size

                currentStats = currentStats.copy(
                    processed = maxOf(0, currentStats.processed - 1),
                    trashed = maxOf(0, currentStats.trashed - 1),
                    savedSize = maxOf(0L, currentStats.savedSize - item.size)
                )
                prefs.stats = currentStats
            }

            is UndoAction.Like -> {
                val liked = prefs.likedIds
                liked.remove(item.id)
                prefs.likedIds = liked

                currentStats = currentStats.copy(
                    processed = maxOf(0, currentStats.processed - 1),
                    skipped = maxOf(0, currentStats.skipped - 1)
                )
                prefs.stats = currentStats
            }

            is UndoAction.Keep -> {
                currentStats = currentStats.copy(
                    processed = maxOf(0, currentStats.processed - 1),
                    skipped = maxOf(0, currentStats.skipped - 1)
                )
                prefs.stats = currentStats
            }

            is UndoAction.Move -> {
                val moved = prefs.movedIds
                moved.remove(item.id)
                prefs.movedIds = moved

                val mutableUsage = prefs.folderUsage.toMutableMap()
                val targetBucketId = File(action.targetFolder).name
                if (action.isNewFolder) {
                    mutableUsage.remove(targetBucketId)
                } else {
                    val count = mutableUsage[targetBucketId] ?: 0
                    if (count > 0) {
                        mutableUsage[targetBucketId] = count - 1
                    }
                }
                prefs.folderUsage = mutableUsage
                updatedFolderUsage = mutableUsage

                currentStats = currentStats.copy(
                    processed = maxOf(0, currentStats.processed - 1),
                    skipped = maxOf(0, currentStats.skipped - 1)
                )
                prefs.stats = currentStats
            }
        }

        val topGroup = currentState.similarGroups.find { group -> group.any { it.id == item.id } }

        _uiState.update {
            it.copy(
                displayMedia = restoredDisplayMedia,
                undoStack = remainingUndoStack,
                undoAnimationTarget = animTarget,
                isUndoAnimating = true,
                stats = currentStats,
                pendingTrashCount = pendingTrashCount,
                processedIds = prefs.processedIds.toSet(),
                pendingTrashIds = prefs.pendingTrashIds.toSet(),
                folderUsage = updatedFolderUsage,
                topCardSimilarGroup = topGroup
            )
        }
    }

    /**
     * 重置撤销动效目标。
     */
    fun clearUndoAnimation() {
        _uiState.update {
            it.copy(
                undoAnimationTarget = null,
                isUndoAnimating = false
            )
        }
    }

    /**
     * 加载下一个卡片批次。
     *
     * @param playEntryAnimation 是否需要标记执行入场动画。
     * @param initial 是否为首次初始化加载。
     */
    fun loadNextBatch(playEntryAnimation: Boolean = false, initial: Boolean = false) {
        viewModelScope.launch(defaultDispatcher) {
            val currentState = _uiState.value
            val processedSnapshot = prefs.processedIds.toSet()
            val monthFilter = currentState.filterMonth
            val folderFilter = currentState.filterFolder
            val typeFilter = currentState.filterType
            val batchSize = currentState.batchSize

            var filtered = allMediaList

            if (monthFilter != "All") {
                val calendar = Calendar.getInstance()
                filtered = filtered.filter { item ->
                    calendar.timeInMillis = item.dateAdded * 1000L
                    val year = calendar.get(Calendar.YEAR)
                    val month = calendar.get(Calendar.MONTH) + 1
                    val key = "$year-${month.toString().padStart(2, '0')}"
                    key == monthFilter
                }
            }

            if (folderFilter != "All") {
                filtered = filtered.filter { it.bucketName == folderFilter }
            }

            if (typeFilter != "All") {
                filtered = filtered.filter { item ->
                    when (typeFilter) {
                        "Image" -> item.type == "photo" && !item.isLivePhoto
                        "Video" -> item.type == "video"
                        "Live" -> item.isLivePhoto
                        else -> true
                    }
                }
            }

            val remaining = filtered.filter { !processedSnapshot.contains(it.id) }
            val nextBatch = remaining.take(batchSize)
            val remainingAfterBatch = (remaining.size - nextBatch.size).coerceAtLeast(0)

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        displayMedia = nextBatch,
                        currentBatchTotal = nextBatch.size,
                        isFullAlbumDone = allMediaList.isNotEmpty() && remaining.isEmpty(),
                        remainingCount = remainingAfterBatch,
                        isLoading = false,
                        undoStack = emptyList()
                    )
                }
                triggerLookaheadSimilarityCheck(nextBatch)
            }
        }
    }

    /**
     * 释放当前筛选条件下的保留照片，将其移出已处理集合以便重新整理。
     */
    fun releaseKeptMedia() {
        viewModelScope.launch(defaultDispatcher) {
            val currentState = _uiState.value
            val processed = prefs.processedIds.toMutableSet()
            val liked = prefs.likedIds.toSet()
            val pendingTrash = prefs.pendingTrashIds.toSet()
            val moved = prefs.movedIds.toSet()

            var targetMedia = allMediaList
            val folderFilter = currentState.filterFolder
            val typeFilter = currentState.filterType
            val monthFilter = currentState.filterMonth

            if (folderFilter != "All") {
                targetMedia = targetMedia.filter { it.bucketName == folderFilter }
            }

            if (typeFilter != "All") {
                targetMedia = targetMedia.filter { item ->
                    when (typeFilter) {
                        "Image" -> item.type == "photo" && !item.isLivePhoto
                        "Video" -> item.type == "video"
                        "Live" -> item.isLivePhoto
                        else -> true
                    }
                }
            }

            if (monthFilter != "All") {
                val calendar = Calendar.getInstance()
                targetMedia = targetMedia.filter { item ->
                    calendar.timeInMillis = item.dateAdded * 1000L
                    val year = calendar.get(Calendar.YEAR)
                    val month = calendar.get(Calendar.MONTH) + 1
                    val key = "$year-${month.toString().padStart(2, '0')}"
                    key == monthFilter
                }
            }

            val toRelease = targetMedia.filter {
                processed.contains(it.id) &&
                    !liked.contains(it.id) &&
                    !pendingTrash.contains(it.id) &&
                    !moved.contains(it.id)
            }

            if (toRelease.isNotEmpty()) {
                val toReleaseIds = toRelease.map { it.id }.toSet()
                processed.removeAll(toReleaseIds)
                prefs.processedIds = processed

                val stats = prefs.stats
                val updatedStats = stats.copy(
                    processed = (stats.processed - toRelease.size).coerceAtLeast(0),
                    skipped = (stats.skipped - toRelease.size).coerceAtLeast(0)
                )
                prefs.stats = updatedStats

                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            processedIds = processed,
                            stats = updatedStats
                        )
                    }
                }
            }

            loadNextBatch()
        }
    }

    /**
     * 应用新的筛选条件。
     */
    fun applyFilter(folder: String, type: String, month: String) {
        prefs.filterFolder = folder
        prefs.filterType = type
        prefs.filterMonth = month

        _uiState.update {
            it.copy(
                filterFolder = folder,
                filterType = type,
                filterMonth = month
            )
        }
        loadNextBatch()
    }

    /**
     * 切换媒体队列排序方式。
     */
    fun setQueueOrder(order: String) {
        prefs.queueOrder = order
        _uiState.update { it.copy(queueOrder = order) }
        allMediaList = sortMediaList(allMediaList, order)
        loadNextBatch()
    }

    /**
     * 重置已处理记录，重新开始整理相册。
     */
    fun resetProcessedProgress() {
        prefs.processedIds.clear()
        prefs.saveProcessedIdsToDisk(emptySet())
        loadNextBatch()
    }

    /**
     * 响应废纸篓批量安全删除完成。
     */
    fun onTrashConfirmed(deletedIds: Set<Long>) {
        val pending = prefs.pendingTrashIds
        pending.removeAll(deletedIds)
        prefs.pendingTrashIds = pending

        allMediaList = allMediaList.filter { !deletedIds.contains(it.id) }
        val updatedFolders = calculateFolderList(allMediaList)
        val updatedDropTargets = calculateFolderDropTargets(allMediaList, _uiState.value.folderUsage)

        _uiState.update {
            it.copy(
                allMedia = allMediaList,
                folderList = updatedFolders,
                folderDropTargets = updatedDropTargets,
                pendingTrashCount = pending.size,
                pendingTrashIds = pending.toSet(),
                displayMedia = it.displayMedia.filter { item -> !deletedIds.contains(item.id) }
            )
        }
        if (_uiState.value.displayMedia.isEmpty()) {
            loadNextBatch()
        }
    }

    private fun triggerLookaheadSimilarityCheck(batch: List<MediaItem>) {
        if (batch.isEmpty()) return

        val itemsToCheck = batch.take(16)
        val currentIds = itemsToCheck.map { it.id }.toSet()
        if (lastCheckedIds.isNotEmpty() && lastCheckedIds.containsAll(currentIds)) {
            return
        }
        lastCheckedIds = currentIds

        similarCheckJob?.cancel()
        similarCheckJob = viewModelScope.launch(defaultDispatcher) {
            val photosSorted = allMediaList.filter { it.type == "photo" }.sortedBy { it.dateAdded }
            val discoveredGroups = mutableListOf<List<MediaItem>>()

            for (target in itemsToCheck) {
                if (target.type != "photo") continue

                val targetAnalysis = MediaAnalysisManager.getAnalysis(target.id) ?: continue
                val targetHash = targetAnalysis.dHash
                if (targetHash == 0L) continue

                val minTime = target.dateAdded - 120
                val maxTime = target.dateAdded + 120
                val candidates = photosSorted.filter { it.dateAdded in minTime..maxTime && it.id != target.id }

                val matches = mutableListOf<MediaItem>()
                for (candidate in candidates) {
                    val candidateHash = MediaAnalysisManager.getAnalysis(candidate.id)?.dHash ?: continue
                    if (java.lang.Long.bitCount(targetHash xor candidateHash) <= 7) {
                        matches.add(candidate)
                    }
                }

                if (matches.isNotEmpty()) {
                    discoveredGroups.add(listOf(target) + matches)
                }
            }

            val distinctGroups = discoveredGroups.distinctBy { group ->
                group.map { it.id }.sorted().joinToString(",")
            }

            withContext(Dispatchers.Main) {
                val topCard = _uiState.value.displayMedia.firstOrNull()
                val topGroup = if (topCard != null) {
                    distinctGroups.find { group -> group.any { it.id == topCard.id } }
                } else {
                    null
                }
                _uiState.update {
                    it.copy(
                        similarGroups = distinctGroups,
                        topCardSimilarGroup = topGroup
                    )
                }
            }
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
                OrganizeViewModel(app)
            }
        }
    }
}
