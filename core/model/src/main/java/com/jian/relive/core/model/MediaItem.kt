package com.jian.relive.core.model

import android.net.Uri

/**
 * 核心媒体实体模型。
 *
 * @property id 媒体唯一标识符（MediaStore _ID）。
 * @property uri 媒体内容 URI。
 * @property type 媒体类别（"photo" 或 "video"）。
 * @property dateAdded 添加时间戳。
 * @property size 文件大小（字节）。
 * @property width 媒体宽度像素。
 * @property height 媒体高度像素。
 * @property mimeType 媒体 MIME 类型。
 * @property displayName 文件显示名称。
 * @property resolution 分辨率描述文本。
 * @property camera 拍摄设备信息。
 * @property iso ISO 感光度。
 * @property shutter 快门时间。
 * @property focalLength 焦距。
 * @property aperture 光圈值。
 * @property location 拍摄地理位置描述。
 * @property isLivePhoto 是否为实况照片。
 * @property livePhotoVideoUri 实况照片关联视频 URI。
 * @property livePhotoVideoId 实况照片关联视频 MediaStore ID。
 * @property path 文件物理绝对路径。
 * @property bucketName 所属相册/目录名称。
 * @property duration 视频时长（毫秒）。
 */
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val type: String,
    val dateAdded: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val displayName: String = "",
    val resolution: String = "",
    val camera: String = "--",
    val iso: String = "--",
    val shutter: String = "--",
    val focalLength: String = "--",
    val aperture: String = "--",
    val location: String = "Unknown Location",
    val isLivePhoto: Boolean = false,
    val livePhotoVideoUri: Uri? = null,
    val livePhotoVideoId: Long? = null,
    val path: String?,
    val bucketName: String = "",
    val duration: Long = 0L
) {
    val isVideo: Boolean get() = type == "video"
    val isPhoto: Boolean get() = type == "photo"
}

