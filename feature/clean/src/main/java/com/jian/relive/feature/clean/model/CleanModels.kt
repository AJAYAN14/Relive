package com.jian.relive.feature.clean.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.jian.relive.core.model.MediaItem
import com.jian.relive.feature.clean.R

/**
 * 清理页面主功能 Tab 枚举。
 */
enum class CleanTab(
    @get:StringRes val titleRes: Int
) {
    SIMILAR(R.string.clean_tab_similar),
    LOW_QUALITY(R.string.clean_tab_low_quality)
}

/**
 * 低质照片的判定原因枚举。
 */
enum class LowQualityReason(
    @get:StringRes val labelRes: Int
) {
    BLUR(R.string.clean_reason_blur),
    UNDEREXPOSED(R.string.clean_reason_underexposed),
    OVEREXPOSED(R.string.clean_reason_overexposed),
    SMALL_FILE(R.string.clean_reason_small_file),
    LOW_RESOLUTION(R.string.clean_reason_low_resolution),
    CLOSED_EYES(R.string.clean_reason_closed_eyes),
    SOLID_COLOR(R.string.clean_reason_solid_color)
}

/**
 * 相似照片分组实体。
 *
 * @param id 组唯一标识，默认以首张照片的 ID 标识。
 * @param photos 构成相似关系的媒体项目列表。
 */
@Immutable
data class SimilarPhotoGroup(
    val id: Long,
    val photos: List<MediaItem>
)

/**
 * 低质照片分析记录。
 *
 * @param item 对应的媒体实体。
 * @param reason 导致归类的主要低质原因。
 */
@Immutable
data class LowQualityItem(
    val item: MediaItem,
    val reason: LowQualityReason
)

/**
 * 用于回滚上一步滑动的撤销动作描述。
 */
@Immutable
sealed interface UndoCleanAction {
    data class SimilarDecision(
        val deletedIds: Set<Long>,
        val processedGroup: SimilarPhotoGroup
    ) : UndoCleanAction

    data class LowQualitySwipe(
        val item: LowQualityItem,
        val wasDeleted: Boolean
    ) : UndoCleanAction
}

/**
 * 清理模块的整体响应式 UI 状态容器。
 */
@Immutable
data class CleanUiState(
    val selectedTab: CleanTab = CleanTab.SIMILAR,
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val similarGroups: List<SimilarPhotoGroup> = emptyList(),
    val lowQualityItems: List<LowQualityItem> = emptyList(),
    val pendingTrashIds: Set<Long> = emptySet(),
    val canUndo: Boolean = false,
    val isConfirmingDelete: Boolean = false
)
