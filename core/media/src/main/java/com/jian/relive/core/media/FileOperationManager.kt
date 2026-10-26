package com.jian.relive.core.media

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.jian.relive.core.model.MediaItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 原生 Android 15 分区存储文件物理操作管理器。
 *
 * 统一收编跨模块媒体移动、批量删除及废纸篓操作，提供纯净的现代媒体存储操作入口。
 */
object FileOperationManager {

    /**
     * 将媒体列表移动至目标相册目录。
     *
     * @param context 应用程序上下文。
     * @param items 待移动的媒体实体列表。
     * @param destinationFolder 目标目录相对路径（例如 "Pictures/Vacation"）。
     * @param coroutineDispatcher 协程调度器，默认 Dispatchers.IO。
     * @return 若需要系统授权弹窗则返回 [IntentSender]，若静默执行成功则返回 null。
     */
    suspend fun moveMedia(
        context: Context,
        items: List<MediaItem>,
        destinationFolder: String,
        coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
    ): IntentSender? {
        return withContext(coroutineDispatcher) {
            val hasManageStorage = Environment.isExternalStorageManager()

            if (hasManageStorage) {
                val targetDir = File(Environment.getExternalStorageDirectory(), destinationFolder)
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }

                items.forEach { item ->
                    val sourcePath = getPathFromUri(context, item.uri)
                    if (sourcePath != null) {
                        val sourceFile = File(sourcePath)
                        val destFile = File(targetDir, sourceFile.name)
                        if (sourceFile.exists()) {
                            if (sourceFile.renameTo(destFile)) {
                                scanFile(context, destFile.absolutePath)
                                scanFile(context, sourceFile.absolutePath)
                            } else {
                                try {
                                    sourceFile.inputStream().use { input ->
                                        destFile.outputStream().use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    if (destFile.exists() && destFile.length() == sourceFile.length()) {
                                        sourceFile.delete()
                                        scanFile(context, destFile.absolutePath)
                                        scanFile(context, sourceFile.absolutePath)
                                    }
                                } catch (e: Exception) {
                                    if (e is CancellationException) throw e
                                }
                            }
                        }
                    }
                }
                return@withContext null
            }

            val failedUris = mutableListOf<Uri>()

            items.forEach { item ->
                try {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, destinationFolder)
                    }
                    context.contentResolver.update(item.uri, values, null, null)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    failedUris.add(item.uri)
                }
            }

            if (failedUris.isNotEmpty()) {
                return@withContext MediaStore.createWriteRequest(context.contentResolver, failedUris).intentSender
            }

            null
        }
    }

    /**
     * 批量删除或移入回收站。
     *
     * @param context 应用程序上下文。
     * @param uris 待删除的媒体 URI 列表。
     * @param moveToTrash 是否移入系统废纸篓（默认为 true）。
     * @param coroutineDispatcher 协程调度器，默认 Dispatchers.IO。
     * @return 若需要系统授权弹窗则返回 [IntentSender]，若静默直接完成则返回 null。
     */
    suspend fun deleteMedia(
        context: Context,
        uris: List<Uri>,
        moveToTrash: Boolean = true,
        coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
    ): IntentSender? {
        if (uris.isEmpty()) return null

        return withContext(coroutineDispatcher) {
            val hasManageStorage = Environment.isExternalStorageManager()

            if (hasManageStorage) {
                uris.forEach { uri ->
                    try {
                        context.contentResolver.delete(uri, null, null)
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                    }
                }
                return@withContext null
            }

            if (moveToTrash) {
                MediaStore.createTrashRequest(context.contentResolver, uris, true).intentSender
            } else {
                MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
            }
        }
    }

    /**
     * 通过 ContentResolver 解析出媒体的绝对路径。
     */
    private fun getPathFromUri(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        return try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    cursor.getString(idx)
                } else null
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }
    }

    /**
     * 触发媒体库重新扫描指定物理文件。
     */
    fun scanFile(context: Context, path: String) {
        MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
    }
}
