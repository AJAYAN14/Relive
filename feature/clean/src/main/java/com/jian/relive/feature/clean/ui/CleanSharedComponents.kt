package com.jian.relive.feature.clean.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.jian.relive.core.model.MediaItem
import com.jian.relive.core.ui.animation.bounceClick
import com.jian.relive.core.ui.theme.ReliveBlack
import com.jian.relive.core.ui.theme.ReliveGray100
import com.jian.relive.core.ui.theme.ReliveGray200
import com.jian.relive.core.ui.theme.ReliveGray400
import com.jian.relive.core.ui.theme.ReliveGray600
import com.jian.relive.core.ui.theme.ReliveGreenLight
import com.jian.relive.core.ui.theme.ReliveRed600
import com.jian.relive.core.ui.theme.ReliveWhite
import com.jian.relive.feature.clean.R

/**
 * 清理模块通用空状态展示视图。
 */
@Composable
fun CleanEmptyStateView(
    title: String,
    description: String,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ReliveGray100),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(ReliveWhite, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = ReliveGreenLight,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ReliveBlack,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                fontSize = 14.sp,
                color = ReliveGray600,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onDoneClick,
                colors = ButtonDefaults.buttonColors(containerColor = ReliveBlack),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .height(48.dp)
                    .bounceClick(onClick = onDoneClick)
            ) {
                Text(
                    text = stringResource(R.string.clean_done),
                    color = ReliveWhite,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 初次分析过程中的全屏扫描等待指示视图。
 */
@Composable
fun CleanScanningWaitView(
    progress: Float,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ReliveGray100),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            CircularProgressIndicator(
                color = ReliveBlack,
                strokeWidth = 3.dp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.clean_scanning_wait),
                color = ReliveBlack,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.clean_scanning_description),
                color = ReliveGray600,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .padding(top = 24.dp)
                    .width(220.dp)
                    .height(6.dp)
                    .clip(CircleShape),
                color = ReliveBlack,
                trackColor = ReliveGray200
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onStopClick,
                colors = ButtonDefaults.buttonColors(containerColor = ReliveWhite),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.bounceClick(onClick = onStopClick)
            ) {
                Text(
                    text = stringResource(R.string.clean_stop),
                    color = ReliveBlack,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 带有内存与磁盘缓存优化的纯净图片缩略展示。
 */
@Composable
fun CleanMediaThumbnail(
    item: MediaItem,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val imageRequest = ImageRequest.Builder(context)
        .data(item.uri)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .crossfade(true)
        .build()

    AsyncImage(
        model = imageRequest,
        contentDescription = item.displayName,
        contentScale = contentScale,
        modifier = modifier
            .clip(shape)
            .background(ReliveGray200)
    )
}

/**
 * 确认将选中的媒体移入废纸篓二次确认对话框。
 */
@Composable
fun CleanConfirmTrashDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.clean_confirm_delete_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Text(
                text = stringResource(R.string.clean_confirm_delete_msg, count),
                fontSize = 14.sp,
                color = ReliveGray600
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ReliveRed600)
            ) {
                Text(
                    text = stringResource(R.string.clean_confirm),
                    color = ReliveWhite
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.clean_cancel),
                    color = ReliveBlack
                )
            }
        },
        containerColor = ReliveWhite,
        shape = RoundedCornerShape(24.dp)
    )
}
