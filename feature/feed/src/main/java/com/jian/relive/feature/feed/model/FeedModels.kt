package com.jian.relive.feature.feed.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.jian.relive.core.model.MediaItem
import com.jian.relive.feature.feed.R

/**
 * 实况与视频流媒体分类过滤枚举。
 */
enum class FeedFilterType(
    @get:StringRes val titleRes: Int
) {
    ALL(R.string.feed_filter_all),
    LIVE(R.string.feed_filter_live),
    VIDEO(R.string.feed_filter_video)
}

/**
 * 实况流模块整体响应式 UI 状态容器。
 */
@Immutable
data class FeedUiState(
    val filterType: FeedFilterType = FeedFilterType.ALL,
    val isMuted: Boolean = true,
    val feedItems: List<MediaItem> = emptyList(),
    val likedIds: Set<Long> = emptySet(),
    val isLoading: Boolean = false,
    val itemToDelete: MediaItem? = null,
    val showVolumeToast: Boolean = false
)
