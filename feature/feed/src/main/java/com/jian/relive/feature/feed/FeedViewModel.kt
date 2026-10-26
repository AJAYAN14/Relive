package com.jian.relive.feature.feed

import android.app.Application
import android.content.Context
import android.content.IntentSender
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jian.relive.core.common.preferences.PreferencesManager
import com.jian.relive.core.media.FileOperationManager
import com.jian.relive.core.media.MediaRepository
import com.jian.relive.core.media.MotionPhotoHelper
import com.jian.relive.core.model.MediaItem
import com.jian.relive.feature.feed.model.FeedFilterType
import com.jian.relive.feature.feed.model.FeedUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 实况与视频流媒体业务状态驱动器。
 *
 * 管理动态照片与视频列表过滤筛选、常驻静音策略同步、喜欢状态与安全废纸篓删除。
 */
class FeedViewModel(
    application: Application,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    private val mediaRepository = MediaRepository()
    private val prefs = PreferencesManager(context)

    private val _uiState = MutableStateFlow(
        FeedUiState(
            isMuted = prefs.muteVideos,
            likedIds = prefs.likedIds.toSet()
        )
    )
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private var cachedAllMedia: List<MediaItem> = emptyList()
    private var volumeToastJob: Job? = null

    init {
        loadMedia()
    }

    val allMedia: List<MediaItem>
        get() = cachedAllMedia

    /**
     * 获取当前已被收藏喜欢的相片集合。
     */
    fun getLikedMediaItems(): List<MediaItem> {
        val liked = prefs.likedIds.toSet()
        return cachedAllMedia.filter { liked.contains(it.id) }
    }

    /**
     * 取消指定媒体项目的收藏喜欢状态。
     */
    fun unlikeItem(id: Long) {
        val currentLiked = prefs.likedIds.toMutableSet()
        currentLiked.remove(id)
        prefs.likedIds = currentLiked
        _uiState.update { it.copy(likedIds = currentLiked.toSet()) }
    }

    /**
     * 重新加载媒体并响应式更新流列表。
     */
    fun loadMedia() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            cachedAllMedia = mediaRepository.loadMedia(context)
            applyFilter()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * 切换流媒体过滤类型（全部 / 实况 / 视频）。
     */
    fun setFilterType(type: FeedFilterType) {
        _uiState.update { it.copy(filterType = type) }
        viewModelScope.launch {
            applyFilter()
        }
    }

    /**
     * 切换全局静音状态并触发气泡指示。
     */
    fun toggleMute() {
        val nextMute = !_uiState.value.isMuted
        prefs.muteVideos = nextMute

        volumeToastJob?.cancel()
        _uiState.update {
            it.copy(
                isMuted = nextMute,
                showVolumeToast = true
            )
        }

        volumeToastJob = viewModelScope.launch {
            delay(1500)
            _uiState.update { it.copy(showVolumeToast = false) }
        }
    }

    /**
     * 切换指定项的喜欢状态。
     */
    fun toggleLike(item: MediaItem) {
        val currentLiked = prefs.likedIds.toMutableSet()
        val willBeLiked = !currentLiked.contains(item.id)

        if (willBeLiked) {
            currentLiked.add(item.id)
        } else {
            currentLiked.remove(item.id)
        }

        prefs.likedIds = currentLiked
        _uiState.update { it.copy(likedIds = currentLiked.toSet()) }
    }

    /**
     * 设定当前弹窗待确认删除的项目。
     */
    fun setItemToDelete(item: MediaItem?) {
        _uiState.update { it.copy(itemToDelete = item) }
    }

    /**
     * 执行移入废纸篓删除操作。
     */
    fun executeDelete(
        target: MediaItem,
        onIntentSender: (IntentSender) -> Unit,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val uris = listOfNotNull(target.uri, target.livePhotoVideoUri)
            val intentSender = FileOperationManager.deleteMedia(
                context = context,
                uris = uris,
                moveToTrash = true
            )

            if (intentSender != null) {
                onIntentSender(intentSender)
            } else {
                onDeleteConfirmed(target)
                onSuccess()
            }
        }
    }

    /**
     * 确认删除完成后的状态清空与缓存注销。
     */
    fun onDeleteConfirmed(item: MediaItem) {
        MotionPhotoHelper.cleanCache(context, item.id)

        cachedAllMedia = cachedAllMedia.filter { it.id != item.id }
        _uiState.update { state ->
            val updatedFeed = state.feedItems.filter { it.id != item.id }
            state.copy(
                feedItems = updatedFeed,
                itemToDelete = null
            )
        }
    }

    private suspend fun applyFilter() = withContext(defaultDispatcher) {
        val filter = _uiState.value.filterType
        val filtered = when (filter) {
            FeedFilterType.ALL -> cachedAllMedia.filter { it.type == "video" || it.isLivePhoto }
            FeedFilterType.LIVE -> cachedAllMedia.filter { it.isLivePhoto }
            FeedFilterType.VIDEO -> cachedAllMedia.filter { it.type == "video" }
        }.sortedByDescending { it.dateAdded }

        _uiState.update { it.copy(feedItems = filtered) }
    }

    companion object {
        /**
         * 显式构造工厂：经 CreationExtras 取 Application 直接实例化，
         * 规避默认工厂对多参数构造签名的反射匹配失败。
         */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                FeedViewModel(app)
            }
        }
    }
}
