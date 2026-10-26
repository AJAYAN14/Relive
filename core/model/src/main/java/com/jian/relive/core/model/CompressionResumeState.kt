package com.jian.relive.core.model

/**
 * 媒体压缩任务断点续传状态模型。
 *
 * @property queueIds 待压缩队列中的媒体标识列表。
 * @property doneIds 已完成压缩处理的媒体标识列表。
 */
data class CompressionResumeState(
    val queueIds: List<Long> = emptyList(),
    val doneIds: List<Long> = emptyList()
)
