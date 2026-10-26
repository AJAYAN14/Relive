package com.jian.relive.core.common.util

import java.util.Locale
import kotlin.math.log
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * 格式化快门速度显示文本。
 *
 * @param shutter 原始快门数值字符串。
 * @return 格式化后的快门时间描述（如 "1/100s" 或 "2s"）。
 */
fun formatShutterSpeed(shutter: String): String {
    if (shutter == "--") return "--"
    return try {
        val value = shutter.toDouble()
        if (value < 1.0 && value > 0) {
            val reciprocal = (1.0 / value).roundToInt()
            "1/${reciprocal}s"
        } else {
            val text = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
            "${text}s"
        }
    } catch (e: Exception) {
        shutter
    }
}

/**
 * 格式化字节大小为易读的文件体积单位（B、KB、MB、GB 等）。
 */
fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    if (bytes < 1024) return "$bytes B"
    val unit = 1024
    val exp = (log(bytes.toDouble(), unit.toDouble())).toInt()
    val pre = "KMGTPE"[exp - 1]
    val value = bytes / (unit.toDouble().pow(exp.toDouble()))
    return String.format(Locale.getDefault(), "%.1f %sB", value, pre)
}


