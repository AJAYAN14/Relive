package com.jian.relive.feature.compress.model

import android.net.Uri
import androidx.annotation.StringRes

data class ImageQualityOption(
    val key: String,
    @param:StringRes val labelRes: Int,
    val maxSizePx: Int,
    val quality: Int
)

data class BitrateOption(
    val key: String,
    @param:StringRes val labelRes: Int,
    val ratio: Float,
    val minVideoBitrate: Int,
    val fallbackVideoBitrate: Int,
    val audioBitrate: Int
)


data class CompressionSummaryItem(
    val id: Long,
    val displayName: String,
    val originalUri: Uri,
    val originalSize: Long,
    val outputSize: Long
)

data class CompressionSummary(
    val items: List<CompressionSummaryItem>,
    val totalOriginal: Long,
    val totalOutput: Long
)

data class CompressionResult(
    val originalSize: Long,
    val outputSize: Long,
    val outputUri: Uri
)
