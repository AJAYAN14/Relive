package com.jian.relive.core.model

/**
 * 撤销操作密封类层级。
 */
sealed class UndoAction {
    abstract val item: MediaItem
    abstract val direction: SwipeDirection

    /**
     * 回收站删除动作撤销。
     */
    data class Trash(
        override val item: MediaItem,
        override val direction: SwipeDirection = SwipeDirection.Left
    ) : UndoAction()

    /**
     * 收藏点赞动作撤销。
     */
    data class Like(
        override val item: MediaItem,
        override val direction: SwipeDirection = SwipeDirection.Right
    ) : UndoAction()

    /**
     * 保留跳过动作撤销。
     */
    data class Keep(
        override val item: MediaItem,
        override val direction: SwipeDirection = SwipeDirection.Up
    ) : UndoAction()

    /**
     * 移动归档至相册动作撤销。
     *
     * @property sourceParentPath 原始父目录路径。
     * @property targetFolder 目标相册目录。
     * @property isNewFolder 是否为新建的相册目录。
     */
    data class Move(
        override val item: MediaItem,
        val sourceParentPath: String,
        val targetFolder: String,
        val isNewFolder: Boolean = false,
        override val direction: SwipeDirection = SwipeDirection.Down
    ) : UndoAction()
}
