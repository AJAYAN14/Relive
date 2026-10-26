package com.jian.relive.core.media

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import com.jian.relive.core.model.MediaItem

/**
 * 系统媒体分享辅助工具。
 *
 * 封装原生 Intent.ACTION_SEND 分享协议，使用应用包名动态绑定 URI 权限与 ClipData。
 */
object MediaShareHelper {

    /**
     * 调起系统分享选择器分享指定媒体实体。
     *
     * @param context 应用程序上下文。
     * @param item 目标媒体实体。
     */
    fun shareMedia(context: Context, item: MediaItem) {
        val displayName = getShareDisplayName(context, item)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (item.mimeType.isNotEmpty()) {
                item.mimeType
            } else if (item.type == "video") {
                "video/*"
            } else {
                "image/*"
            }
            putExtra(Intent.EXTRA_STREAM, item.uri)
            putExtra(Intent.EXTRA_TITLE, displayName)
            putExtra(Intent.EXTRA_SUBJECT, displayName)
            clipData = ClipData.newUri(context.contentResolver, displayName, item.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, null)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(chooser)
        }
    }

    private fun getShareDisplayName(context: Context, item: MediaItem): String {
        val fromItem = item.displayName.trim()
        if (fromItem.isNotEmpty()) return fromItem

        val fromResolver = runCatching {
            context.contentResolver.query(
                item.uri,
                arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
        }.getOrNull()?.trim().orEmpty()

        if (fromResolver.isNotEmpty()) return fromResolver
        return if (item.type == "video") "video_${item.id}.mp4" else "image_${item.id}.jpg"
    }
}
