package com.jian.relive.feature.feed.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AutoAwesomeMotion
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MotionPhotosOn
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.relive.core.media.MediaShareHelper
import com.jian.relive.core.model.MediaItem
import com.jian.relive.core.ui.animation.bounceClick
import com.jian.relive.core.ui.composition.LocalHapticsEnabled
import com.jian.relive.core.ui.theme.ReliveBlack
import com.jian.relive.core.ui.theme.ReliveGray100
import com.jian.relive.core.ui.theme.ReliveGray400
import com.jian.relive.core.ui.theme.ReliveGray500
import com.jian.relive.core.ui.theme.ReliveGray600
import com.jian.relive.core.ui.theme.ReliveGray700
import com.jian.relive.core.ui.theme.ReliveRed500
import com.jian.relive.core.ui.theme.ReliveWhite
import com.jian.relive.feature.feed.R
import com.jian.relive.feature.feed.model.FeedFilterType
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 顶部居中流媒体分类筛选胶囊栏。
 */
@Composable
fun FeedFilterSegmentedRow(
    selectedFilter: FeedFilterType,
    onFilterSelected: (FeedFilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(ReliveGray100)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FeedFilterType.entries.forEach { filter ->
            val isSelected = selectedFilter == filter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) ReliveBlack else Color.Transparent)
                    .clickable {
                        if (!isSelected) {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onFilterSelected(filter)
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(filter.titleRes),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) ReliveWhite else ReliveGray600
                )
            }
        }
    }
}

/**
 * 顶部声音开关按钮与悬浮提示徽章。
 */
@Composable
fun FeedSoundControl(
    isMuted: Boolean,
    showToast: Boolean,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current

    Box(modifier = modifier) {
        IconButton(
            onClick = {
                if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggleMute()
            },
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(ReliveGray100)
        ) {
            Icon(
                imageVector = if (isMuted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp,
                contentDescription = stringResource(if (isMuted) R.string.feed_sound_muted else R.string.feed_sound_unmuted),
                tint = ReliveBlack,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 屏幕中央悬浮的音量状态提示卡片。
 */
@Composable
fun FeedVolumeToastBadge(
    isMuted: Boolean,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ReliveWhite,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = ReliveBlack
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(if (isMuted) R.string.feed_sound_muted else R.string.feed_sound_unmuted),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = ReliveBlack
                )
            }
        }
    }
}

/**
 * 底部阴影渐变信息遮罩（展示相册、时间、地理位置与标签）。
 */
@Composable
fun FeedItemInfoOverlay(
    item: MediaItem,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, ReliveWhite.copy(alpha = 0.9f))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = bottomPadding + 16.dp, end = 90.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                if (item.isLivePhoto) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ReliveBlack)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.MotionPhotosOn,
                                contentDescription = null,
                                tint = ReliveWhite,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.feed_tag_live),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ReliveWhite
                            )
                        }
                    }
                } else if (item.type == "video") {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ReliveGray100)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Videocam,
                                contentDescription = null,
                                tint = ReliveBlack,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.feed_tag_video),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ReliveBlack
                            )
                        }
                    }
                }

                if (item.bucketName.isNotBlank()) {
                    Text(
                        text = item.bucketName,
                        fontSize = 12.sp,
                        color = ReliveGray600,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            val dateFormatted = remember(item.dateAdded) {
                try {
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        .format(Date(item.dateAdded * 1000L))
                } catch (e: Exception) {
                    ""
                }
            }

            if (dateFormatted.isNotEmpty()) {
                Text(
                    text = dateFormatted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ReliveBlack
                )
            }

            if (item.location != "Unknown Location" && item.location.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = ReliveGray500,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.location,
                        fontSize = 12.sp,
                        color = ReliveGray600,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * 右下侧悬浮交互栏（点赞、系统分享、移入废纸篓）。
 */
@Composable
fun FeedActionsColumn(
    item: MediaItem,
    isLiked: Boolean,
    bottomPadding: Dp,
    onLikeClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current

    Column(
        modifier = modifier
            .padding(end = 16.dp, bottom = bottomPadding + 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        IconButton(
            onClick = {
                if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLikeClick()
            },
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(ReliveGray100)
                .bounceClick(onClick = onLikeClick)
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = stringResource(R.string.feed_like),
                tint = if (isLiked) ReliveRed500 else ReliveBlack,
                modifier = Modifier.size(26.dp)
            )
        }

        IconButton(
            onClick = {
                if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                MediaShareHelper.shareMedia(context, item)
            },
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(ReliveGray100)
                .bounceClick(onClick = { MediaShareHelper.shareMedia(context, item) })
        ) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = stringResource(R.string.feed_share),
                tint = ReliveBlack,
                modifier = Modifier.size(24.dp)
            )
        }

        IconButton(
            onClick = {
                if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDeleteClick()
            },
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(ReliveGray100)
                .bounceClick(onClick = onDeleteClick)
        ) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = stringResource(R.string.feed_delete_button),
                tint = ReliveBlack,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * 点赞红心瞬时放大迸发动效。
 */
@Composable
fun FeedHeartBurst(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = ReliveRed500.copy(alpha = 0.9f),
            modifier = Modifier.size(100.dp)
        )
    }
}

/**
 * 流媒体为空时的优雅空状态展示。
 */
@Composable
fun FeedEmptyView(
    filterType: FeedFilterType,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(bottom = bottomPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(ReliveGray100),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (filterType) {
                        FeedFilterType.LIVE -> Icons.Outlined.MotionPhotosOn
                        FeedFilterType.VIDEO -> Icons.Outlined.Videocam
                        FeedFilterType.ALL -> Icons.Outlined.AutoAwesomeMotion
                    },
                    contentDescription = null,
                    tint = ReliveGray400,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.feed_empty_title),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = ReliveBlack
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.feed_empty_subtitle),
                fontSize = 13.sp,
                color = ReliveGray500,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 单条媒体移入废纸篓二次确认对话框。
 */
@Composable
fun FeedDeleteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ReliveWhite,
        titleContentColor = ReliveBlack,
        textContentColor = ReliveGray700,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = stringResource(R.string.feed_delete_confirm_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Text(
                text = stringResource(R.string.feed_delete_confirm_msg),
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.feed_delete_button),
                    color = ReliveRed500,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.feed_cancel_button),
                    color = ReliveGray600
                )
            }
        }
    )
}
