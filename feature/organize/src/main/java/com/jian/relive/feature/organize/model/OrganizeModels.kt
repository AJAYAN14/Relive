package com.jian.relive.feature.organize.model

import androidx.compose.runtime.Immutable
import com.jian.relive.core.model.AppStats
import com.jian.relive.core.model.MediaItem
import com.jian.relive.core.model.UndoAction
import com.jian.relive.core.model.UndoAnimationTarget

/**
 * 文件夹与相册过滤数据实体。
 *
 * @param name 文件夹/相册名称。
 * @param count 包含媒体数量。
 * @param previews 缩略图预览列表。
 */
@Immutable
data class FilterFolderData(
    val name: String,
    val count: Int,
    val previews: List<MediaItem> = emptyList()
)

/**
 * 媒体类型过滤选项枚举。
 */
enum class OrganizeMediaType(val key: String) {
    ALL("All"),
    IMAGE("Image"),
    VIDEO("Video"),
    LIVE("Live");

    companion object {
        fun fromKey(key: String): OrganizeMediaType {
            return entries.find { it.key.equals(key, ignoreCase = true) } ?: ALL
        }
    }
}

/**
 * 媒体队列排序方式枚举。
 */
enum class OrganizeQueueOrder(val key: String) {
    RANDOM("random"),
    TIME("time");

    companion object {
        fun fromKey(key: String): OrganizeQueueOrder {
            return entries.find { it.key.equals(key, ignoreCase = true) } ?: RANDOM
        }
    }
}

/**
 * 拖拽归类目标相册数据实体。
 *
 * @param id 目标标识或 "new_folder"。
 * @param name 相册展示名称。
 * @param path 相对存储目录路径。
 * @param count 包含媒体数。
 */
@Immutable
data class FolderData(
    val id: String,
    val name: String,
    val path: String?,
    val count: Int
)

/**
 * 主整理流程 UI 状态容器。
 */
@Immutable
data class OrganizeUiState(
    val displayMedia: List<MediaItem> = emptyList(),
    val allMedia: List<MediaItem> = emptyList(),
    val processedIds: Set<Long> = emptySet(),
    val pendingTrashIds: Set<Long> = emptySet(),
    val folderDropTargets: List<FolderData> = emptyList(),
    val currentBatchTotal: Int = 16,
    val isFullAlbumDone: Boolean = false,
    val isLoading: Boolean = true,
    val pendingTrashCount: Int = 0,
    val stats: AppStats = AppStats(),
    val undoStack: List<UndoAction> = emptyList(),
    val undoAnimationTarget: UndoAnimationTarget? = null,
    val isUndoAnimating: Boolean = false,
    val comboCount: Int = 0,
    val batchSize: Int = 16,
    val queueOrder: String = "random",
    val filterMonth: String = "All",
    val filterFolder: String = "All",
    val filterType: String = "All",
    val folderList: List<FilterFolderData> = emptyList(),
    val similarGroups: List<List<MediaItem>> = emptyList(),
    val topCardSimilarGroup: List<MediaItem>? = null,
    val folderUsage: Map<String, Int> = emptyMap(),
    val soundEffectsEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true
) {
    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val topCard: MediaItem?
        get() = displayMedia.firstOrNull()
}

