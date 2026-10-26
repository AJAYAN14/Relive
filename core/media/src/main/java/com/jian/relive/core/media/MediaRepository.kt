package com.jian.relive.core.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.jian.relive.core.common.util.formatShutterSpeed
import com.jian.relive.core.model.MediaItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 媒体数据仓库。
 *
 * 负责从系统 MediaStore 异步查询图片与视频资产，执行三方干扰目录剔除、实况照片成对配对合并、动态照片关联解析与 EXIF 读取。
 */
class MediaRepository(
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + defaultDispatcher)
    private var scanJob: Job? = null

    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems: StateFlow<List<MediaItem>> = _mediaItems.asStateFlow()

    private data class RawMediaItem(
        val id: Long,
        val date: Long,
        val type: String,
        val size: Long,
        val width: Int,
        val height: Int,
        val mime: String,
        val name: String,
        val uri: Uri,
        val bucketId: String,
        val duration: Long,
        val path: String
    )

    /**
     * 异步加载设备图库媒体列表。
     *
     * @param context 应用程序上下文。
     * @param coroutineDispatcher 协程调度器，默认使用仓库注入的 Dispatcher。
     * @return 经过实况照片配对与随机打乱后的媒体实体列表。
     */
    suspend fun loadMedia(
        context: Context,
        coroutineDispatcher: CoroutineDispatcher = defaultDispatcher
    ): List<MediaItem> = withContext(coroutineDispatcher) {
        MotionPhotoHelper.initCache(context)
        val items = mutableListOf<MediaItem>()

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.WIDTH,
            MediaStore.Files.FileColumns.HEIGHT,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.DURATION,
            MediaStore.Files.FileColumns.DATA
        )

        val selection = "(${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?) AND ${MediaStore.MediaColumns.IS_TRASHED}=0"
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )

        try {
            context.contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                val typeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val wCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
                val hCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

                val rawItems = mutableListOf<RawMediaItem>()

                while (cursor.moveToNext()) {
                    val bucketName = cursor.getString(bucketNameCol) ?: ""
                    if (bucketName.contains("WeChat", true) ||
                        bucketName.contains("WeiXin", true) ||
                        bucketName.contains("QQ", true) ||
                        bucketName.contains("Tencent", true)
                    ) {
                        continue
                    }

                    val id = cursor.getLong(idCol)
                    val date = cursor.getLong(dateCol)
                    val typeInt = cursor.getInt(typeCol)
                    val size = cursor.getLong(sizeCol)
                    val w = cursor.getInt(wCol)
                    val h = cursor.getInt(hCol)
                    val mime = cursor.getString(mimeCol) ?: ""
                    val name = cursor.getString(nameCol) ?: ""
                    val bucketId = cursor.getString(bucketCol) ?: "unknown"
                    val bucketNameStr = cursor.getString(bucketNameCol) ?: "Unknown"
                    val duration = cursor.getLong(durationCol)
                    val path = cursor.getString(pathCol) ?: ""

                    val type = if (typeInt == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) "video" else "photo"
                    val contentUri = if (type == "video") {
                        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    } else {
                        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    }

                    rawItems.add(
                        RawMediaItem(
                            id = id,
                            date = date,
                            type = type,
                            size = size,
                            width = w,
                            height = h,
                            mime = mime,
                            name = name,
                            uri = contentUri,
                            bucketId = bucketNameStr,
                            duration = duration,
                            path = path
                        )
                    )
                }

                val grouped = rawItems.groupBy { "${it.bucketId}_${it.name.substringBeforeLast(".")}" }

                grouped.forEach { (_, group) ->
                    val photos = group.filter { it.type == "photo" }
                    val videos = group.filter { it.type == "video" }

                    if (photos.isNotEmpty() && videos.isNotEmpty()) {
                        val photo = photos.first()
                        val video = videos.first()

                        items.add(
                            MediaItem(
                                id = photo.id,
                                uri = photo.uri,
                                type = "photo",
                                dateAdded = photo.date,
                                size = photo.size + video.size,
                                width = photo.width,
                                height = photo.height,
                                mimeType = photo.mime,
                                displayName = photo.name,
                                resolution = "${photo.width}x${photo.height}",
                                isLivePhoto = true,
                                livePhotoVideoUri = video.uri,
                                livePhotoVideoId = video.id,
                                bucketName = photo.bucketId,
                                duration = video.duration,
                                path = photo.path
                            )
                        )

                        val remainingPhotos = photos.drop(1)
                        val remainingVideos = videos.drop(1)
                        (remainingPhotos + remainingVideos).forEach { item ->
                            val isMotion = if (item.type == "photo") {
                                MotionPhotoHelper.isKnownMotionPhoto(item.id) ?: false
                            } else {
                                false
                            }
                            items.add(
                                MediaItem(
                                    id = item.id,
                                    uri = item.uri,
                                    type = item.type,
                                    dateAdded = item.date,
                                    size = item.size,
                                    width = item.width,
                                    height = item.height,
                                    mimeType = item.mime,
                                    displayName = item.name,
                                    resolution = "${item.width}x${item.height}",
                                    isLivePhoto = isMotion,
                                    bucketName = item.bucketId,
                                    duration = item.duration,
                                    path = item.path
                                )
                            )
                        }
                    } else {
                        group.forEach { item ->
                            val isMotion = if (item.type == "photo") {
                                MotionPhotoHelper.isKnownMotionPhoto(item.id) ?: false
                            } else {
                                false
                            }
                            items.add(
                                MediaItem(
                                    id = item.id,
                                    uri = item.uri,
                                    type = item.type,
                                    dateAdded = item.date,
                                    size = item.size,
                                    width = item.width,
                                    height = item.height,
                                    mimeType = item.mime,
                                    displayName = item.name,
                                    resolution = "${item.width}x${item.height}",
                                    isLivePhoto = isMotion,
                                    bucketName = item.bucketId,
                                    duration = item.duration,
                                    path = item.path
                                )
                            )
                        }
                    }
                }

                items.shuffle()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("MediaRepository", "Error loading media: ${e.message}", e)
        }

        _mediaItems.value = items
        startMotionPhotoScan(context, items)
        items
    }

    private fun startMotionPhotoScan(context: Context, items: List<MediaItem>) {
        scanJob?.cancel()
        val unscannedPhotos = items.filter {
            it.type == "photo" && it.size >= 1500 * 1024L && MotionPhotoHelper.isKnownMotionPhoto(it.id) == null
        }
        if (unscannedPhotos.isEmpty()) return

        scanJob = repositoryScope.launch {
            var scannedCount = 0
            for (photo in unscannedPhotos) {
                if (!isActive) break
                val isMotion = MotionPhotoHelper.isMotionPhoto(context, photo.id, photo.uri, photo.path, photo.size)
                if (isMotion) {
                    _mediaItems.update { currentList ->
                        currentList.map {
                            if (it.id == photo.id) it.copy(isLivePhoto = true) else it
                        }
                    }
                }
                scannedCount++
                if (scannedCount % 50 == 0) {
                    MotionPhotoHelper.persistCache(context)
                }
            }
            if (scannedCount > 0) {
                MotionPhotoHelper.persistCache(context)
            }
        }
    }

    /**
     * 按需读取指定媒体的相机 EXIF 拍摄参数。
     *
     * @param context 应用程序上下文。
     * @param item 目标媒体实体。
     * @param coroutineDispatcher 协程调度器，默认使用仓库注入的 Dispatcher。
     * @return 填充了相机型号、ISO、快门、焦距与光圈信息的全新 [MediaItem] 实体。
     */
    suspend fun loadExif(
        context: Context,
        item: MediaItem,
        coroutineDispatcher: CoroutineDispatcher = defaultDispatcher
    ): MediaItem = withContext(coroutineDispatcher) {
        if (item.type != "photo") return@withContext item

        try {
            context.contentResolver.openInputStream(item.uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val cameraMake = exif.getAttribute(ExifInterface.TAG_MAKE).orEmpty()
                val cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL).orEmpty()
                val cameraDesc = when {
                    cameraMake.isNotEmpty() && cameraModel.isNotEmpty() -> {
                        if (cameraModel.startsWith(cameraMake, ignoreCase = true)) cameraModel else "$cameraMake $cameraModel"
                    }
                    cameraModel.isNotEmpty() -> cameraModel
                    cameraMake.isNotEmpty() -> cameraMake
                    else -> "--"
                }

                val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY) ?: "--"
                val rawShutter = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME) ?: "--"
                val shutter = formatShutterSpeed(rawShutter)
                val rawFocalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH) ?: "--"
                val focalLength = if (rawFocalLength.contains("/")) {
                    val parts = rawFocalLength.split("/")
                    if (parts.size == 2) {
                        val num = parts[0].toDoubleOrNull() ?: 0.0
                        val den = parts[1].toDoubleOrNull() ?: 1.0
                        if (den != 0.0) "${(num / den).toInt()}mm" else rawFocalLength
                    } else rawFocalLength
                } else if (rawFocalLength != "--") {
                    "${rawFocalLength.toDoubleOrNull()?.toInt() ?: rawFocalLength}mm"
                } else "--"

                val aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { "f/$it" } ?: "--"

                return@withContext item.copy(
                    camera = cameraDesc,
                    iso = iso,
                    shutter = shutter,
                    focalLength = focalLength,
                    aperture = aperture
                )
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w("MediaRepository", "Failed to parse EXIF for media ${item.id}: ${e.message}")
        }

        item
    }

    /**
     * 全量更新内存媒体列表状态。
     */
    fun updateMediaList(newList: List<MediaItem>) {
        _mediaItems.value = newList
    }

    /**
     * 从内存列表中移除指定 ID 的媒体实体。
     */
    fun removeMediaItem(id: Long) {
        _mediaItems.value = _mediaItems.value.filter { it.id != id }
    }

    /**
     * 根据媒体 ID 检索内存实体。
     */
    fun findMediaItem(id: Long): MediaItem? {
        return _mediaItems.value.find { it.id == id }
    }
}
