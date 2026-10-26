package com.jian.relive.core.media

import android.content.Context
import android.net.Uri
import android.util.Log
import com.jian.relive.core.model.MediaItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

/**
 * 原生实况照片与动态照片（Motion Photo）解析及流式提取工具。
 *
 * 支持 Google Camera、小米、三星及 OPPO 等厂商规范的 XMP APP1 头部扫描与 MP4 视频流按需提取。
 */
object MotionPhotoHelper {
    private const val TAG = "MotionPhotoHelper"
    private const val HEADER_SCAN_SIZE = 65536
    private const val CHUNK_SIZE = 65536

    private const val PREFS_NAME = "motion_photo_cache"
    private const val KEY_LIVE_IDS = "live_photo_ids"
    private const val KEY_SCANNED_IDS = "scanned_photo_ids"
    private const val KEY_OFFSETS = "video_offsets"

    private val motionPhotoStatusCache = ConcurrentHashMap<Long, Boolean>()
    private val videoOffsetCache = ConcurrentHashMap<Long, Long>()

    @Volatile
    private var isCacheLoaded = false

    /**
     * 极速预热已扫描的动态照片持久化索引缓存。
     */
    fun initCache(context: Context) {
        if (isCacheLoaded) return
        synchronized(this) {
            if (isCacheLoaded) return
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val liveSet = prefs.getStringSet(KEY_LIVE_IDS, null)
                val scannedSet = prefs.getStringSet(KEY_SCANNED_IDS, null)

                scannedSet?.forEach { idStr ->
                    idStr.toLongOrNull()?.let { id ->
                        motionPhotoStatusCache[id] = false
                    }
                }
                liveSet?.forEach { idStr ->
                    idStr.toLongOrNull()?.let { id ->
                        motionPhotoStatusCache[id] = true
                    }
                }

                val offsetsString = prefs.getString(KEY_OFFSETS, null)
                if (!offsetsString.isNullOrEmpty()) {
                    offsetsString.split(";").forEach { pair ->
                        val parts = pair.split(":")
                        if (parts.size == 2) {
                            val id = parts[0].toLongOrNull()
                            val offset = parts[1].toLongOrNull()
                            if (id != null && offset != null) {
                                videoOffsetCache[id] = offset
                            }
                        }
                    }
                }
                isCacheLoaded = true
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Failed to load cache: ${e.message}")
            }
        }
    }

    /**
     * 检查目标照片是否已存在于内存或历史扫描缓存中。
     *
     * @return true 为实况照片，false 为静态照片，null 为尚未扫描。
     */
    fun isKnownMotionPhoto(id: Long): Boolean? {
        return motionPhotoStatusCache[id]
    }

    /**
     * 持久化已识别的动态照片索引至磁盘。
     */
    fun persistCache(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val liveSet = HashSet<String>()
            val scannedSet = HashSet<String>()
            val offsetBuilder = StringBuilder()

            motionPhotoStatusCache.forEach { (id, isLive) ->
                val idStr = id.toString()
                if (isLive) {
                    liveSet.add(idStr)
                    videoOffsetCache[id]?.let { offset ->
                        if (offsetBuilder.isNotEmpty()) offsetBuilder.append(";")
                        offsetBuilder.append(id).append(":").append(offset)
                    }
                } else {
                    scannedSet.add(idStr)
                }
            }

            prefs.edit()
                .putStringSet(KEY_LIVE_IDS, liveSet)
                .putStringSet(KEY_SCANNED_IDS, scannedSet)
                .putString(KEY_OFFSETS, offsetBuilder.toString())
                .apply()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Failed to persist cache: ${e.message}")
        }
    }

    /**
     * 对单张媒体执行动态照片判断（快速尺寸过滤 + XMP 头部扫描）。
     */
    fun isMotionPhoto(context: Context, id: Long, uri: Uri, path: String?, fileSize: Long): Boolean {
        motionPhotoStatusCache[id]?.let { return it }

        // 嵌入视频轨的动态照片通常大于 1.5MB，小于该尺寸直接判定为静态照片
        if (fileSize < 1500 * 1024L) {
            motionPhotoStatusCache[id] = false
            return false
        }

        if (path != null) {
            val lower = path.lowercase()
            if (!lower.endsWith(".jpg") && !lower.endsWith(".jpeg") &&
                !lower.endsWith(".heic") && !lower.endsWith(".heif")) {
                motionPhotoStatusCache[id] = false
                return false
            }
        }

        try {
            val headerBytes = readHeaderBytes(context, uri, path, HEADER_SCAN_SIZE)
            if (headerBytes == null || headerBytes.isEmpty()) {
                motionPhotoStatusCache[id] = false
                return false
            }

            val xmpString = String(headerBytes, StandardCharsets.ISO_8859_1)

            if (!xmpString.contains("MotionPhoto", ignoreCase = true) &&
                !xmpString.contains("MicroVideo", ignoreCase = true)) {
                motionPhotoStatusCache[id] = false
                return false
            }

            val offsetFromXmp = parseOffsetFromXmp(xmpString, fileSize)
            val isMotion: Boolean
            if (offsetFromXmp > 0 && offsetFromXmp < fileSize) {
                videoOffsetCache[id] = offsetFromXmp
                isMotion = true
            } else {
                val fallbackOffset = searchFtypOffsetChunked(context, uri, path, fileSize)
                if (fallbackOffset > 0) {
                    videoOffsetCache[id] = fallbackOffset
                    isMotion = true
                } else {
                    isMotion = false
                }
            }

            motionPhotoStatusCache[id] = isMotion
            return isMotion
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            motionPhotoStatusCache[id] = false
            return false
        }
    }

    private fun parseOffsetFromXmp(xmpString: String, fileSize: Long): Long {
        val microVideoRegex = Regex("""MicroVideoOffset(?:=['"]|>)\s*(\d+)""", RegexOption.IGNORE_CASE)
        val matchMicro = microVideoRegex.findAll(xmpString).lastOrNull()
        if (matchMicro != null) {
            val length = matchMicro.groupValues[1].toLongOrNull()
            if (length != null && length in 10000 until fileSize) {
                return fileSize - length
            }
        }

        val itemLengthRegex = Regex("""Item:Length(?:=['"]|>)\s*(\d+)""", RegexOption.IGNORE_CASE)
        for (match in itemLengthRegex.findAll(xmpString)) {
            val length = match.groupValues[1].toLongOrNull()
            if (length != null && length in 10000 until fileSize) {
                return fileSize - length
            }
        }

        return -1L
    }

    private fun searchFtypOffsetChunked(context: Context, uri: Uri, path: String?, fileSize: Long): Long {
        try {
            val scanStart = (fileSize - 35 * 1024 * 1024L).coerceAtLeast(64 * 1024L)
            val buffer = ByteArray(CHUNK_SIZE + 4)

            if (path != null && File(path).canRead()) {
                RandomAccessFile(path, "r").use { raf ->
                    var currentPos = scanStart
                    while (currentPos < fileSize) {
                        raf.seek(currentPos)
                        val readBytes = raf.read(buffer, 0, CHUNK_SIZE + 4)
                        if (readBytes < 8) break

                        val foundIndex = findFtypInBuffer(buffer, readBytes)
                        if (foundIndex != -1) {
                            val absoluteVideoStart = currentPos + foundIndex - 4
                            if (absoluteVideoStart in 0 until fileSize) {
                                return absoluteVideoStart
                            }
                        }
                        currentPos += CHUNK_SIZE
                    }
                }
            } else {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { pfd ->
                    FileInputStream(pfd.fileDescriptor).use { fis ->
                        var currentPos = scanStart
                        while (currentPos < fileSize) {
                            fis.channel.position(currentPos)
                            val readBytes = fis.read(buffer, 0, CHUNK_SIZE + 4)
                            if (readBytes < 8) break

                            val foundIndex = findFtypInBuffer(buffer, readBytes)
                            if (foundIndex != -1) {
                                val absoluteVideoStart = currentPos + foundIndex - 4
                                if (absoluteVideoStart in 0 until fileSize) {
                                    return absoluteVideoStart
                                }
                            }
                            currentPos += CHUNK_SIZE
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "searchFtypOffsetChunked failed: ${e.message}")
        }
        return -1L
    }

    private fun findFtypInBuffer(buffer: ByteArray, length: Int): Int {
        for (i in 4 until length - 3) {
            if (buffer[i] == 'f'.code.toByte() &&
                buffer[i + 1] == 't'.code.toByte() &&
                buffer[i + 2] == 'y'.code.toByte() &&
                buffer[i + 3] == 'p'.code.toByte()) {
                return i
            }
        }
        return -1
    }

    private fun readHeaderBytes(context: Context, uri: Uri, path: String?, size: Int): ByteArray? {
        try {
            val buffer = ByteArray(size)
            var read = 0
            if (path != null && File(path).canRead()) {
                RandomAccessFile(path, "r").use { raf ->
                    read = raf.read(buffer)
                }
            } else {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    read = stream.read(buffer)
                }
            }
            if (read > 0) {
                return if (read == size) buffer else buffer.copyOf(read)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
        return null
    }

    /**
     * 获取已被提取并缓存至应用内部沙盒的 MP4 视频 URI。
     */
    fun getCachedVideoUri(context: Context, item: MediaItem): Uri? {
        if (item.livePhotoVideoUri != null) return item.livePhotoVideoUri
        if (!item.isLivePhoto) return null

        val cacheDir = File(context.cacheDir, "motion_videos")
        val targetFile = File(cacheDir, "live_${item.id}.mp4")
        if (targetFile.exists() && targetFile.length() > 1024) {
            return Uri.fromFile(targetFile)
        }
        return null
    }

    /**
     * 按需提取：从动态照片内部截取嵌入的 MP4 视频流并写入应用缓存。
     */
    suspend fun getOrExtractVideoUri(
        context: Context,
        item: MediaItem,
        coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
    ): Uri? = withContext(coroutineDispatcher) {
        if (item.livePhotoVideoUri != null) {
            return@withContext item.livePhotoVideoUri
        }
        if (!item.isLivePhoto) {
            return@withContext null
        }

        val cacheDir = File(context.cacheDir, "motion_videos").apply { if (!exists()) mkdirs() }
        val targetFile = File(cacheDir, "live_${item.id}.mp4")

        if (targetFile.exists() && targetFile.length() > 1024) {
            return@withContext Uri.fromFile(targetFile)
        }

        var offset = videoOffsetCache[item.id]
        if (offset == null || offset <= 0) {
            val headerBytes = readHeaderBytes(context, item.uri, item.path, HEADER_SCAN_SIZE)
            if (headerBytes != null) {
                val xmpString = String(headerBytes, StandardCharsets.ISO_8859_1)
                offset = parseOffsetFromXmp(xmpString, item.size)
            }
            if (offset == null || offset <= 0) {
                offset = searchFtypOffsetChunked(context, item.uri, item.path, item.size)
            }
            if (offset > 0) {
                videoOffsetCache[item.id] = offset
            }
        }

        val currentOffset = offset
        if (currentOffset <= 0 || currentOffset >= item.size) {
            Log.e(TAG, "Cannot extract video, invalid offset: $currentOffset for item ${item.id}")
            return@withContext null
        }

        try {
            val tempFile = File(cacheDir, "live_${item.id}.tmp")
            val buffer = ByteArray(CHUNK_SIZE)
            val filePath = item.path

            if (filePath != null && File(filePath).canRead()) {
                RandomAccessFile(filePath, "r").use { raf ->
                    raf.seek(currentOffset)
                    FileOutputStream(tempFile).use { fos ->
                        var r: Int
                        while (raf.read(buffer).also { r = it } != -1) {
                            fos.write(buffer, 0, r)
                        }
                    }
                }
            } else {
                context.contentResolver.openAssetFileDescriptor(item.uri, "r")?.use { pfd ->
                    FileInputStream(pfd.fileDescriptor).use { fis ->
                        fis.channel.position(offset)
                        FileOutputStream(tempFile).use { fos ->
                            var r: Int
                            while (fis.read(buffer).also { r = it } != -1) {
                                fos.write(buffer, 0, r)
                            }
                        }
                    }
                }
            }

            if (tempFile.exists() && tempFile.length() > 1024) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
                return@withContext Uri.fromFile(targetFile)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Failed to extract motion video for item ${item.id}: ${e.message}", e)
        }

        return@withContext null
    }

    /**
     * 清除指定媒体的本地抽取视频缓存和内存索引。
     */
    fun cleanCache(context: Context, itemId: Long) {
        try {
            val cacheDir = File(context.cacheDir, "motion_videos")
            val targetFile = File(cacheDir, "live_${itemId}.mp4")
            if (targetFile.exists()) {
                targetFile.delete()
            }
            motionPhotoStatusCache.remove(itemId)
            videoOffsetCache.remove(itemId)
            persistCache(context)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error cleaning cache for item $itemId: ${e.message}")
        }
    }
}
