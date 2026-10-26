package com.jian.relive.feature.compress

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.jian.relive.core.common.preferences.PreferencesManager
import com.jian.relive.core.media.FileOperationManager
import com.jian.relive.core.media.MediaAnalysisManager
import com.jian.relive.core.media.MediaRepository
import com.jian.relive.core.model.CompressionResumeState
import com.jian.relive.core.model.MediaItem
import com.jian.relive.feature.compress.model.BitrateOption
import com.jian.relive.feature.compress.model.CompressionResult
import com.jian.relive.feature.compress.model.CompressionSummary
import com.jian.relive.feature.compress.model.CompressionSummaryItem
import com.jian.relive.feature.compress.model.ImageQualityOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.roundToInt

class CompressViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    val prefs = PreferencesManager(context)
    val mediaRepository = MediaRepository()

    val allMediaItems: StateFlow<List<MediaItem>> = mediaRepository.mediaItems

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // ----------------- 图片压缩状态 -----------------
    private val _imageSelectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val imageSelectedIds: StateFlow<Set<Long>> = _imageSelectedIds.asStateFlow()

    private val _imageThresholdMb = MutableStateFlow(prefs.imageCompressThresholdMb.coerceIn(3, 10))
    val imageThresholdMb: StateFlow<Int> = _imageThresholdMb.asStateFlow()

    private val _imageQualityKey = MutableStateFlow(prefs.imageCompressQualityKey)
    val imageQualityKey: StateFlow<String> = _imageQualityKey.asStateFlow()

    private val _isImageCompressing = MutableStateFlow(false)
    val isImageCompressing: StateFlow<Boolean> = _isImageCompressing.asStateFlow()

    private val _imageProgress = MutableStateFlow(0f)
    val imageProgress: StateFlow<Float> = _imageProgress.asStateFlow()

    private val _activeImageItem = MutableStateFlow<MediaItem?>(null)
    val activeImageItem: StateFlow<MediaItem?> = _activeImageItem.asStateFlow()

    private val _imageErrorMessage = MutableStateFlow<String?>(null)
    val imageErrorMessage: StateFlow<String?> = _imageErrorMessage.asStateFlow()

    private val _imageResultSummary = MutableStateFlow<CompressionSummary?>(null)
    val imageResultSummary: StateFlow<CompressionSummary?> = _imageResultSummary.asStateFlow()

    private val _imageRemovedIds = MutableStateFlow<Set<Long>>(emptySet())
    val imageRemovedIds: StateFlow<Set<Long>> = _imageRemovedIds.asStateFlow()

    // ----------------- 视频压缩状态 -----------------
    private val _videoSelectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val videoSelectedIds: StateFlow<Set<Long>> = _videoSelectedIds.asStateFlow()

    private val _videoThresholdMb = MutableStateFlow(50)
    val videoThresholdMb: StateFlow<Int> = _videoThresholdMb.asStateFlow()

    private val _videoBitrateKey = MutableStateFlow(prefs.videoCompressBitrateKey)
    val videoBitrateKey: StateFlow<String> = _videoBitrateKey.asStateFlow()

    private val _isVideoCompressing = MutableStateFlow(false)
    val isVideoCompressing: StateFlow<Boolean> = _isVideoCompressing.asStateFlow()

    private val _videoProgress = MutableStateFlow(0f)
    val videoProgress: StateFlow<Float> = _videoProgress.asStateFlow()

    private val _activeVideoItem = MutableStateFlow<MediaItem?>(null)
    val activeVideoItem: StateFlow<MediaItem?> = _activeVideoItem.asStateFlow()

    private val _videoErrorMessage = MutableStateFlow<String?>(null)
    val videoErrorMessage: StateFlow<String?> = _videoErrorMessage.asStateFlow()

    private val _videoResultSummary = MutableStateFlow<CompressionSummary?>(null)
    val videoResultSummary: StateFlow<CompressionSummary?> = _videoResultSummary.asStateFlow()

    private val _videoRemovedIds = MutableStateFlow<Set<Long>>(emptySet())
    val videoRemovedIds: StateFlow<Set<Long>> = _videoRemovedIds.asStateFlow()

    private val _videoDurationMap = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val videoDurationMap: StateFlow<Map<Long, Long>> = _videoDurationMap.asStateFlow()

    val qualityOptions = listOf(
        ImageQualityOption("small", R.string.compress_image_original_size, 1280, 72),
        ImageQualityOption("balanced", R.string.compress_image_original_size, 1920, 82),
        ImageQualityOption("sharp", R.string.compress_image_original_size, 2560, 90),
        ImageQualityOption("original", R.string.compress_image_original_label, Int.MAX_VALUE, 85)
    )

    val bitrateOptions = listOf(
        BitrateOption("low", R.string.compress_video_bitrate_low, 0.33f, 4_200_000, 5_000_000, 160_000),
        BitrateOption("balanced", R.string.compress_video_bitrate_balanced, 0.42f, 5_200_000, 6_200_000, 160_000),
        BitrateOption("high", R.string.compress_video_bitrate_high, 0.55f, 7_000_000, 8_500_000, 192_000)
    )

    init {
        viewModelScope.launch {
            mediaRepository.loadMedia(context)
        }
        val initialResume = prefs.imageCompressResume
        if (initialResume.queueIds.isNotEmpty()) {
            _imageSelectedIds.value = initialResume.queueIds.toSet()
        }
        val videoResume = prefs.videoCompressResume
        if (videoResume.queueIds.isNotEmpty()) {
            _videoSelectedIds.value = videoResume.queueIds.toSet()
        }
    }


    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    // ----------------- 图片压缩交互 -----------------
    fun setImageThreshold(mb: Int) {
        _imageThresholdMb.value = mb
        prefs.imageCompressThresholdMb = mb
    }

    fun setImageQualityKey(key: String) {
        _imageQualityKey.value = key
        prefs.imageCompressQualityKey = key
    }

    fun toggleImageSelection(id: Long) {
        val current = _imageSelectedIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _imageSelectedIds.value = current
    }

    fun toggleMonthImagesSelection(monthIds: List<Long>, selectAll: Boolean) {
        val current = _imageSelectedIds.value.toMutableSet()
        if (selectAll) {
            current.addAll(monthIds)
        } else {
            current.removeAll(monthIds.toSet())
        }
        _imageSelectedIds.value = current
    }

    fun setImageSelection(ids: Set<Long>) {
        _imageSelectedIds.value = ids
    }

    fun dismissImageResultSummary() {
        _imageResultSummary.value = null
    }

    fun startImageCompression(visiblePhotos: List<MediaItem>) {
        if (_isImageCompressing.value || _imageSelectedIds.value.isEmpty()) return

        viewModelScope.launch {
            _isImageCompressing.value = true
            _imageErrorMessage.value = null

            val visibleIdSet = visiblePhotos.map { it.id }.toSet()
            val resumeState = prefs.imageCompressResume
            val pendingQueue = resumeState.queueIds.filter { it in visibleIdSet }
            val pendingSet = pendingQueue.toSet()
            val selectionQueue = visiblePhotos.filter { _imageSelectedIds.value.contains(it.id) }.map { it.id }
            val selectionSet = selectionQueue.toSet()

            val usePending = pendingQueue.isNotEmpty() && (selectionSet.isEmpty() || selectionSet.intersect(pendingSet).isNotEmpty())
            val queueIds = if (usePending) pendingQueue else selectionQueue
            if (queueIds.isEmpty()) {
                _isImageCompressing.value = false
                return@launch
            }

            val doneSet = if (usePending) resumeState.doneIds.toMutableSet() else mutableSetOf()
            val cleanedDoneSet = doneSet.filter { it in queueIds }.toMutableSet()
            val totalCount = queueIds.size
            val itemMap = visiblePhotos.associateBy { it.id }

            val currentResume = CompressionResumeState(queueIds = queueIds, doneIds = cleanedDoneSet.toList())
            prefs.imageCompressResume = currentResume

            val option = qualityOptions.firstOrNull { it.key == _imageQualityKey.value } ?: qualityOptions[1]
            val successResults = mutableListOf<CompressionSummaryItem>()

            queueIds.forEach { id ->
                if (cleanedDoneSet.contains(id)) {
                    if (totalCount > 0) {
                        _imageProgress.value = cleanedDoneSet.size.toFloat() / totalCount.toFloat()
                    }
                    return@forEach
                }

                val item = itemMap[id] ?: return@forEach
                _activeImageItem.value = item
                if (totalCount > 0) {
                    _imageProgress.value = cleanedDoneSet.size.toFloat() / totalCount.toFloat()
                }

                val baseIndex = cleanedDoneSet.size
                val tempFile = compressImageToTempFile(
                    item = item,
                    maxSizePx = option.maxSizePx,
                    quality = option.quality,
                    onProgress = { p ->
                        if (totalCount > 0) {
                            val current = (baseIndex.toFloat() + p.coerceIn(0f, 1f)) / totalCount.toFloat()
                            _imageProgress.value = current.coerceIn(0f, 1f)
                        }
                    }
                )

                if (tempFile == null) {
                    _imageErrorMessage.value = context.getString(R.string.compress_image_error)
                    return@forEach
                }

                val outputUri = withContext(Dispatchers.IO) {
                    saveCompressedImage(tempFile, item.displayName, item.dateAdded, item.uri)
                }
                val outputSize = tempFile.length()
                tempFile.delete()

                if (outputUri == null) {
                    _imageErrorMessage.value = context.getString(R.string.compress_image_error)
                } else {
                    cleanedDoneSet.add(item.id)
                    val updatedResume = CompressionResumeState(queueIds = queueIds, doneIds = cleanedDoneSet.toList())
                    prefs.imageCompressResume = updatedResume

                    successResults.add(
                        CompressionSummaryItem(
                            id = item.id,
                            displayName = item.displayName,
                            originalUri = item.uri,
                            originalSize = item.size,
                            outputSize = outputSize
                        )
                    )
                }
            }

            _activeImageItem.value = null
            _isImageCompressing.value = false

            if (cleanedDoneSet.size == queueIds.size) {
                prefs.imageCompressResume = CompressionResumeState()
            }

            if (successResults.isNotEmpty()) {
                val totalOriginal = successResults.sumOf { it.originalSize }
                val totalOutput = successResults.sumOf { it.outputSize }
                val savedBytes = (totalOriginal - totalOutput).coerceAtLeast(0)
                if (savedBytes > 0) {
                    prefs.stats = prefs.stats.copy(savedSize = prefs.stats.savedSize + savedBytes)
                }
                _imageResultSummary.value = CompressionSummary(
                    items = successResults,
                    totalOriginal = totalOriginal,
                    totalOutput = totalOutput
                )
                _imageSelectedIds.value = _imageSelectedIds.value - successResults.map { it.id }.toSet()
            }
        }
    }

    suspend fun deleteOriginalImages(items: List<CompressionSummaryItem>): IntentSender? {
        val uris = items.map { it.originalUri }
        val ids = items.map { it.id }.toSet()
        val intentSender = FileOperationManager.deleteMedia(context, uris, moveToTrash = true)
        if (intentSender == null) {
            _imageRemovedIds.value = _imageRemovedIds.value + ids
            _imageSelectedIds.value = _imageSelectedIds.value - ids
            _imageResultSummary.value = null
        }
        return intentSender
    }

    fun onImageDeleteConfirmed(ids: List<Long>) {
        val idSet = ids.toSet()
        _imageRemovedIds.value = _imageRemovedIds.value + idSet
        _imageSelectedIds.value = _imageSelectedIds.value - idSet
        _imageResultSummary.value = null
    }

    // ----------------- 视频压缩交互 -----------------
    fun setVideoThreshold(mb: Int) {
        _videoThresholdMb.value = mb
    }

    fun setVideoBitrateKey(key: String) {
        _videoBitrateKey.value = key
        prefs.videoCompressBitrateKey = key
    }

    fun toggleVideoSelection(id: Long) {
        val current = _videoSelectedIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _videoSelectedIds.value = current
    }

    fun dismissVideoResultSummary() {
        _videoResultSummary.value = null
    }

    fun warmVideos(visibleVideos: List<MediaItem>) {
        val targets = visibleVideos.take(40)
        viewModelScope.launch(Dispatchers.IO) {
            MediaAnalysisManager.warmVideoThumbs(context, targets)
            val map = _videoDurationMap.value.toMutableMap()

            var changed = false
            targets.forEach { item ->
                if (!map.containsKey(item.id)) {
                    val duration = loadVideoDurationMs(item.uri)
                    if (duration != null) {
                        map[item.id] = duration
                        changed = true
                    }
                }
            }
            if (changed) {
                _videoDurationMap.value = map
            }
        }
    }

    fun startVideoCompression(visibleVideos: List<MediaItem>) {
        if (_isVideoCompressing.value || _videoSelectedIds.value.isEmpty()) return

        viewModelScope.launch {
            _isVideoCompressing.value = true
            _videoErrorMessage.value = null

            val visibleIdSet = visibleVideos.map { it.id }.toSet()
            val resumeState = prefs.videoCompressResume
            val pendingQueue = resumeState.queueIds.filter { it in visibleIdSet }
            val pendingSet = pendingQueue.toSet()
            val selectionQueue = visibleVideos.filter { _videoSelectedIds.value.contains(it.id) }.map { it.id }
            val selectionSet = selectionQueue.toSet()

            val usePending = pendingQueue.isNotEmpty() && (selectionSet.isEmpty() || selectionSet.intersect(pendingSet).isNotEmpty())
            val queueIds = if (usePending) pendingQueue else selectionQueue
            if (queueIds.isEmpty()) {
                _isVideoCompressing.value = false
                return@launch
            }

            val doneSet = if (usePending) resumeState.doneIds.toMutableSet() else mutableSetOf()
            val cleanedDoneSet = doneSet.filter { it in queueIds }.toMutableSet()
            val totalCount = queueIds.size
            val itemMap = visibleVideos.associateBy { it.id }

            val currentResume = CompressionResumeState(queueIds = queueIds, doneIds = cleanedDoneSet.toList())
            prefs.videoCompressResume = currentResume

            val option = bitrateOptions.firstOrNull { it.key == _videoBitrateKey.value } ?: bitrateOptions[1]
            val successResults = mutableListOf<CompressionSummaryItem>()

            queueIds.forEach { id ->
                if (cleanedDoneSet.contains(id)) {
                    if (totalCount > 0) {
                        _videoProgress.value = cleanedDoneSet.size.toFloat() / totalCount.toFloat()
                    }
                    return@forEach
                }

                val item = itemMap[id] ?: return@forEach
                _activeVideoItem.value = item
                if (totalCount > 0) {
                    _videoProgress.value = cleanedDoneSet.size.toFloat() / totalCount.toFloat()
                }

                val baseIndex = cleanedDoneSet.size
                val durationMs = _videoDurationMap.value[item.id] ?: loadVideoDurationMs(item.uri)
                val targetVideoBitrate = deriveTargetVideoBitrate(item.size, durationMs, option)

                val tempFile = compressVideoToTempFile(
                    item = item,
                    targetVideoBitrate = targetVideoBitrate,
                    onProgress = { p ->
                        if (totalCount > 0) {
                            val current = (baseIndex.toFloat() + p.coerceIn(0f, 1f)) / totalCount.toFloat()
                            _videoProgress.value = current.coerceIn(0f, 1f)
                        }
                    }
                )

                if (tempFile == null) {
                    _videoErrorMessage.value = context.getString(R.string.compress_video_error)
                    return@forEach
                }

                val outputUri = withContext(Dispatchers.IO) {
                    saveCompressedVideo(tempFile, item.displayName, item.dateAdded)
                }
                val outputSize = tempFile.length()
                tempFile.delete()

                if (outputUri == null) {
                    _videoErrorMessage.value = context.getString(R.string.compress_video_error)
                } else {
                    cleanedDoneSet.add(item.id)
                    val updatedResume = CompressionResumeState(queueIds = queueIds, doneIds = cleanedDoneSet.toList())
                    prefs.videoCompressResume = updatedResume

                    successResults.add(
                        CompressionSummaryItem(
                            id = item.id,
                            displayName = item.displayName,
                            originalUri = item.uri,
                            originalSize = item.size,
                            outputSize = outputSize
                        )
                    )
                }
            }

            _activeVideoItem.value = null
            _isVideoCompressing.value = false

            if (cleanedDoneSet.size == queueIds.size) {
                prefs.videoCompressResume = CompressionResumeState()
            }

            if (successResults.isNotEmpty()) {
                val totalOriginal = successResults.sumOf { it.originalSize }
                val totalOutput = successResults.sumOf { it.outputSize }
                val savedBytes = (totalOriginal - totalOutput).coerceAtLeast(0)
                if (savedBytes > 0) {
                    prefs.stats = prefs.stats.copy(savedSize = prefs.stats.savedSize + savedBytes)
                }
                _videoResultSummary.value = CompressionSummary(
                    items = successResults,
                    totalOriginal = totalOriginal,
                    totalOutput = totalOutput
                )
                _videoSelectedIds.value = _videoSelectedIds.value - successResults.map { it.id }.toSet()
            }
        }
    }

    suspend fun deleteOriginalVideos(items: List<CompressionSummaryItem>): IntentSender? {
        val uris = items.map { it.originalUri }
        val ids = items.map { it.id }.toSet()
        val intentSender = FileOperationManager.deleteMedia(context, uris, moveToTrash = true)
        if (intentSender == null) {
            _videoRemovedIds.value = _videoRemovedIds.value + ids
            _videoSelectedIds.value = _videoSelectedIds.value - ids
            _videoResultSummary.value = null
        }
        return intentSender
    }

    fun onVideoDeleteConfirmed(ids: List<Long>) {
        val idSet = ids.toSet()
        _videoRemovedIds.value = _videoRemovedIds.value + idSet
        _videoSelectedIds.value = _videoSelectedIds.value - idSet
        _videoResultSummary.value = null
    }

    // ----------------- 图片压缩底层引擎 -----------------
    private suspend fun compressImageToTempFile(
        item: MediaItem,
        maxSizePx: Int,
        quality: Int,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(item.uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }
        val width = boundsOptions.outWidth
        val height = boundsOptions.outHeight
        if (width <= 0 || height <= 0) return@withContext null

        val sampleSize = calculateInSampleSize(width, height, maxSizePx)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = resolver.openInputStream(item.uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return@withContext null

        onProgress(0.3f)
        val rotatedBitmap = rotateBitmapIfNeeded(item.uri, bitmap)
        val scaledBitmap = if (rotatedBitmap.width > maxSizePx || rotatedBitmap.height > maxSizePx) {
            val scale = maxSizePx.toFloat() / max(rotatedBitmap.width, rotatedBitmap.height).toFloat()
            Bitmap.createScaledBitmap(
                rotatedBitmap,
                (rotatedBitmap.width * scale).roundToInt(),
                (rotatedBitmap.height * scale).roundToInt(),
                true
            )
        } else {
            rotatedBitmap
        }
        onProgress(0.7f)

        val outputFile = File(context.cacheDir, "image_compress_${item.id}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outputFile).use { out ->
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(50, 95), out)
        }
        if (scaledBitmap != bitmap) scaledBitmap.recycle()
        if (rotatedBitmap != bitmap && rotatedBitmap != scaledBitmap) rotatedBitmap.recycle()
        bitmap.recycle()

        onProgress(1f)
        outputFile
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxSizePx: Int): Int {
        var inSampleSize = 1
        if (height > maxSizePx || width > maxSizePx) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= maxSizePx && halfWidth / inSampleSize >= maxSizePx) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun rotateBitmapIfNeeded(uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private suspend fun saveCompressedImage(
        tempFile: File,
        originalName: String,
        originalDateAdded: Long,
        originalUri: Uri
    ): Uri? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val baseName = if (originalName.isBlank()) "image_${System.currentTimeMillis()}" else originalName.substringBeforeLast(".")
        val fileName = "${baseName}_compressed.jpg"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (originalDateAdded > 0) {
                put(MediaStore.Images.Media.DATE_ADDED, originalDateAdded)
                put(MediaStore.Images.Media.DATE_MODIFIED, originalDateAdded)
                put(MediaStore.Images.Media.DATE_TAKEN, originalDateAdded * 1000)
            }
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Relive")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
        val outputStream = resolver.openOutputStream(uri)
        if (outputStream == null) {
            resolver.delete(uri, null, null)
            return@withContext null
        }

        outputStream.use { out ->
            tempFile.inputStream().use { input -> input.copyTo(out) }
        }

        try {
            val originalExif = resolver.openInputStream(originalUri)?.use { ExifInterface(it) }
            val pfd = resolver.openFileDescriptor(uri, "rw")
            if (pfd != null) {
                val targetExif = ExifInterface(pfd.fileDescriptor)
                val tags = listOf(
                    ExifInterface.TAG_MAKE, ExifInterface.TAG_MODEL, ExifInterface.TAG_SOFTWARE,
                    ExifInterface.TAG_F_NUMBER, ExifInterface.TAG_APERTURE_VALUE, ExifInterface.TAG_EXPOSURE_TIME,
                    ExifInterface.TAG_SHUTTER_SPEED_VALUE, ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
                    ExifInterface.TAG_FOCAL_LENGTH, ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
                    ExifInterface.TAG_WHITE_BALANCE, ExifInterface.TAG_FLASH,
                    ExifInterface.TAG_GPS_LATITUDE, ExifInterface.TAG_GPS_LONGITUDE,
                    ExifInterface.TAG_GPS_LATITUDE_REF, ExifInterface.TAG_GPS_LONGITUDE_REF,
                    ExifInterface.TAG_GPS_ALTITUDE, ExifInterface.TAG_GPS_ALTITUDE_REF,
                    ExifInterface.TAG_DATETIME_ORIGINAL, ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
                    ExifInterface.TAG_IMAGE_UNIQUE_ID
                )

                tags.forEach { tag ->
                    val v = originalExif?.getAttribute(tag)
                    if (v != null) targetExif.setAttribute(tag, v)
                }
                targetExif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                targetExif.saveAttributes()
                pfd.close()
            }
        } catch (_: Exception) {
        }

        val pendingValues = ContentValues().apply {
            put(MediaStore.Images.Media.IS_PENDING, 0)
        }
        resolver.update(uri, pendingValues, null, null)
        uri
    }

    // ----------------- 视频压缩底层引擎 -----------------
    @OptIn(UnstableApi::class)
    private suspend fun compressVideoToTempFile(
        item: MediaItem,
        targetVideoBitrate: Int,
        onProgress: (Float) -> Unit
    ): File? = suspendCancellableCoroutine { continuation ->
        val outputFile = File(context.cacheDir, "video_compress_${item.id}_${System.currentTimeMillis()}.mp4")

        val encoderFactory = DefaultEncoderFactory.Builder(context)
            .setRequestedVideoEncoderSettings(
                VideoEncoderSettings.Builder()
                    .setBitrate(targetVideoBitrate)
                    .build()
            )
            .build()

        val transformer = Transformer.Builder(context)
            .setEncoderFactory(encoderFactory)
            .build()

        var progressJob: Job? = null
        progressJob = viewModelScope.launch(Dispatchers.Main) {
            val progressHolder = androidx.media3.transformer.ProgressHolder()
            while (isActive) {
                val state = transformer.getProgress(progressHolder)
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    onProgress(progressHolder.progress / 100f)
                }
                delay(300)
            }
        }

        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                transformer.removeListener(this)
                progressJob.cancel()
                onProgress(1f)
                continuation.resume(outputFile)
            }

            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException
            ) {
                transformer.removeListener(this)
                progressJob.cancel()
                outputFile.delete()
                onProgress(0f)
                continuation.resume(null)
            }
        }

        try {
            transformer.addListener(listener)
            transformer.start(androidx.media3.common.MediaItem.fromUri(item.uri), outputFile.absolutePath)
        } catch (e: Exception) {
            transformer.removeListener(listener)
            progressJob.cancel()
            outputFile.delete()
            onProgress(0f)
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        continuation.invokeOnCancellation {
            transformer.removeListener(listener)
            transformer.cancel()
            progressJob.cancel()
            outputFile.delete()
        }
    }

    private suspend fun saveCompressedVideo(
        tempFile: File,
        originalName: String,
        originalDateAdded: Long
    ): Uri? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val baseName = if (originalName.isBlank()) "video_${System.currentTimeMillis()}" else originalName.substringBeforeLast(".")
        val fileName = "${baseName}_compressed.mp4"

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (originalDateAdded > 0) {
                put(MediaStore.Video.Media.DATE_ADDED, originalDateAdded)
                put(MediaStore.Video.Media.DATE_MODIFIED, originalDateAdded)
                put(MediaStore.Video.Media.DATE_TAKEN, originalDateAdded * 1000)
            }
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Relive")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
        val outputStream = resolver.openOutputStream(uri)
        if (outputStream == null) {
            resolver.delete(uri, null, null)
            return@withContext null
        }

        outputStream.use { out ->
            tempFile.inputStream().use { input -> input.copyTo(out) }
        }

        val pendingValues = ContentValues().apply {
            put(MediaStore.Video.Media.IS_PENDING, 0)
        }
        resolver.update(uri, pendingValues, null, null)
        uri
    }

    private suspend fun loadVideoDurationMs(uri: Uri): Long? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    fun estimateOriginalVideoBitrate(sizeBytes: Long, durationMs: Long?): Int? {
        if (sizeBytes <= 0 || durationMs == null || durationMs <= 0) return null
        val seconds = durationMs / 1000.0
        if (seconds <= 0.0) return null
        val bitrate = ((sizeBytes * 8.0) / seconds).toInt()
        return bitrate.coerceAtLeast(100_000)
    }

    fun deriveTargetVideoBitrate(sizeBytes: Long, durationMs: Long?, option: BitrateOption): Int {
        val originalBitrate = estimateOriginalVideoBitrate(sizeBytes, durationMs)
        val target = if (originalBitrate != null) {
            (originalBitrate * option.ratio).toInt()
        } else {
            option.fallbackVideoBitrate
        }
        return target.coerceAtLeast(option.minVideoBitrate)
    }
}
