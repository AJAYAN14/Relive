package com.jian.relive.core.model

/**
 * 相册整理统计数据模型。
 *
 * @property processed 已整理处理的项目数量。
 * @property trashed 移入回收站的项目数量。
 * @property skipped 跳过保留的项目数量。
 * @property savedSize 释放或节省的存储空间大小（字节）。
 */
data class AppStats(
    val processed: Int = 0,
    val trashed: Int = 0,
    val skipped: Int = 0,
    val savedSize: Long = 0L
)
