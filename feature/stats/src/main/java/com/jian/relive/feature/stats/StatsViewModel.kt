package com.jian.relive.feature.stats

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jian.relive.core.common.preferences.PreferencesManager
import com.jian.relive.core.media.MediaRepository
import com.jian.relive.core.model.MediaItem
import com.jian.relive.feature.stats.model.ShootingStatsData
import com.jian.relive.feature.stats.model.StatsPeriod
import com.jian.relive.feature.stats.model.StatsUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 拍摄多维度指标分析业务状态驱动器。
 *
 * 聚合设备图库的时间轴数据、时长、拍摄足迹与贡献热力图矩阵。
 */
class StatsViewModel(
    application: Application,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    private val mediaRepository = MediaRepository()
    private val prefs = PreferencesManager(context)

    private val _uiState = MutableStateFlow(
        StatsUiState(
            targetYear = Calendar.getInstance().get(Calendar.YEAR),
            targetMonth = Calendar.getInstance().get(Calendar.MONTH)
        )
    )
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private var cachedAllMedia: List<MediaItem> = emptyList()

    init {
        loadData()
    }

    /**
     * 加载全量媒体并执行指标聚合计算。
     */
    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            cachedAllMedia = mediaRepository.loadMedia(context)
            computeStats()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * 切换年度/月度统计周期。
     */
    fun selectPeriod(period: StatsPeriod) {
        val now = Calendar.getInstance()
        val maxYear = now.get(Calendar.YEAR)
        val maxMonth = now.get(Calendar.MONTH)

        var newYear = _uiState.value.targetYear
        var newMonth = _uiState.value.targetMonth

        if (newYear > maxYear) {
            newYear = maxYear
            newMonth = maxMonth
        }
        if (period == StatsPeriod.MONTH && newYear == maxYear && newMonth > maxMonth) {
            newMonth = maxMonth
        }

        _uiState.update {
            it.copy(
                period = period,
                targetYear = newYear,
                targetMonth = newMonth
            )
        }
        viewModelScope.launch {
            computeStats()
        }
    }

    /**
     * 切换到更早的统计周期（前一年或上个月）。
     */
    fun navigatePrevious() {
        val current = _uiState.value
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, current.targetYear)
            set(Calendar.MONTH, current.targetMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        if (current.period == StatsPeriod.YEAR) {
            cal.add(Calendar.YEAR, -1)
        } else {
            cal.add(Calendar.MONTH, -1)
        }

        _uiState.update {
            it.copy(
                targetYear = cal.get(Calendar.YEAR),
                targetMonth = cal.get(Calendar.MONTH)
            )
        }
        viewModelScope.launch {
            computeStats()
        }
    }

    /**
     * 切换到更新的统计周期（后一年或下个月）。
     */
    fun navigateNext() {
        if (!_uiState.value.canGoNext) return

        val current = _uiState.value
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, current.targetYear)
            set(Calendar.MONTH, current.targetMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        if (current.period == StatsPeriod.YEAR) {
            cal.add(Calendar.YEAR, 1)
        } else {
            cal.add(Calendar.MONTH, 1)
        }

        _uiState.update {
            it.copy(
                targetYear = cal.get(Calendar.YEAR),
                targetMonth = cal.get(Calendar.MONTH)
            )
        }
        viewModelScope.launch {
            computeStats()
        }
    }

    private suspend fun computeStats() = withContext(defaultDispatcher) {
        val currentState = _uiState.value
        val period = currentState.period
        val targetYear = currentState.targetYear
        val targetMonth = currentState.targetMonth
        val likedIds = prefs.likedIds

        val now = Calendar.getInstance()
        val maxYear = now.get(Calendar.YEAR)
        val maxMonth = now.get(Calendar.MONTH)

        val canGoNext = if (period == StatsPeriod.YEAR) {
            targetYear < maxYear
        } else {
            targetYear < maxYear || (targetYear == maxYear && targetMonth < maxMonth)
        }

        val filteredMedia = if (period == StatsPeriod.YEAR) {
            cachedAllMedia.filter { item ->
                val cal = Calendar.getInstance().apply { timeInMillis = item.dateAdded * 1000L }
                cal.get(Calendar.YEAR) == targetYear
            }
        } else {
            cachedAllMedia.filter { item ->
                val cal = Calendar.getInstance().apply { timeInMillis = item.dateAdded * 1000L }
                cal.get(Calendar.YEAR) == targetYear && cal.get(Calendar.MONTH) == targetMonth
            }
        }

        val photoCount = filteredMedia.count { it.type == "photo" }
        val videoCount = filteredMedia.count { it.type == "video" }
        val totalCount = photoCount + videoCount

        val totalDurationMs = filteredMedia.sumOf { it.duration }
        val totalDurationMins = (totalDurationMs / 60000L).toInt()

        val isZh = Locale.getDefault().language.startsWith("zh")
        val movieComparison = calculateMovieComparison(totalDurationMins, isZh)

        val screenshotCount = filteredMedia.count { item ->
            item.type == "photo" && (
                item.bucketName.contains("Screenshot", ignoreCase = true) ||
                    item.bucketName.contains("截屏", ignoreCase = true) ||
                    item.bucketName.contains("截图", ignoreCase = true)
                )
        }

        val likedCount = filteredMedia.count { likedIds.contains(it.id) }
        val cameraPhotoCount = (photoCount - screenshotCount).coerceAtLeast(0)

        val mostActiveDay = if (filteredMedia.isEmpty()) {
            null
        } else {
            val format = SimpleDateFormat("MM-dd", Locale.getDefault())
            filteredMedia.groupBy { format.format(Date(it.dateAdded * 1000L)) }
                .maxByOrNull { it.value.size }
        }

        val locationCount = filteredMedia.map { it.bucketName }.distinct().size

        val heatmapData = mutableMapOf<String, Int>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        filteredMedia.forEach { item ->
            val key = dateFormat.format(Date(item.dateAdded * 1000L))
            heatmapData[key] = (heatmapData[key] ?: 0) + 1
        }

        val stats = ShootingStatsData(
            photoCount = photoCount,
            videoCount = videoCount,
            totalCount = totalCount,
            totalDurationMins = totalDurationMins,
            screenshotCount = screenshotCount,
            likedCount = likedCount,
            cameraPhotoCount = cameraPhotoCount,
            mostActiveDate = mostActiveDay?.key,
            mostActiveCount = mostActiveDay?.value?.size ?: 0,
            locationCount = locationCount,
            heatmapData = heatmapData,
            movieComparisonText = movieComparison
        )

        _uiState.update {
            it.copy(
                canGoNext = canGoNext,
                canGoPrev = true,
                stats = stats
            )
        }
    }

    private fun calculateMovieComparison(totalDurationMins: Int, isZh: Boolean): String {
        val moviesEn = listOf(
            "Titanic" to 195,
            "Avatar" to 162,
            "The Matrix" to 136,
            "Inception" to 148,
            "The Godfather" to 175,
            "Forrest Gump" to 142
        )
        val moviesZh = listOf(
            "流浪地球" to 125,
            "战狼2" to 123,
            "你好，李焕英" to 128,
            "哪吒之魔童降世" to 110,
            "长津湖" to 176,
            "满江红" to 159
        )

        return when {
            totalDurationMins < 45 -> {
                val ratio = String.format(Locale.getDefault(), "%.1f", totalDurationMins.toFloat() / 3f)
                if (isZh) "相当于 $ratio 部短剧" else "Equiv. to $ratio short series"
            }
            totalDurationMins < 120 -> {
                val ratio = String.format(Locale.getDefault(), "%.1f", totalDurationMins.toFloat() / 45f)
                if (isZh) "相当于 $ratio 集电视剧" else "Equiv. to $ratio TV episodes"
            }
            else -> {
                val list = if (isZh) moviesZh else moviesEn
                val index = totalDurationMins % list.size
                val (name, duration) = list[index]
                val ratio = String.format(Locale.getDefault(), "%.1f", totalDurationMins.toFloat() / duration)
                if (isZh) "相当于 $ratio 部《$name》" else "Equiv. to $ratio x $name"
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
                StatsViewModel(app)
            }
        }
    }
}
