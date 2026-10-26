package com.jian.relive.core.media

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.util.LinkedList

/**
 * 视频播放器实例池缓存管理器。
 *
 * 维护常驻 ExoPlayer 实例池，避免在滑动信息流或视频卡片切换时频繁重建播放器引发的抖动与耗时。
 */
object VideoPlayerCache {
    private val pool = LinkedList<ExoPlayer>()
    private const val MAX_POOL_SIZE = 3

    /**
     * 获取可复用的播放器实例，若池中无空闲实例则构建新实例。
     */
    fun get(context: Context): ExoPlayer {
        return synchronized(pool) {
            if (pool.isNotEmpty()) {
                pool.removeFirst().apply {
                    playWhenReady = false
                    repeatMode = Player.REPEAT_MODE_ONE
                    seekTo(0)
                }
            } else {
                ExoPlayer.Builder(context).build().apply {
                    repeatMode = Player.REPEAT_MODE_ONE
                }
            }
        }
    }

    /**
     * 将播放器释放回池中以便复用；若池已满则直接销毁。
     */
    fun release(player: ExoPlayer) {
        player.stop()
        player.clearMediaItems()

        synchronized(pool) {
            if (pool.size < MAX_POOL_SIZE) {
                pool.add(player)
            } else {
                player.release()
            }
        }
    }

    /**
     * 清空并销毁池中所有播放器实例。
     */
    fun clear() {
        synchronized(pool) {
            pool.forEach { it.release() }
            pool.clear()
        }
    }
}
