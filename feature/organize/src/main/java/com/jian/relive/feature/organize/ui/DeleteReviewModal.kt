package com.jian.relive.feature.organize.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.video.VideoFrameDecoder
import com.jian.relive.core.model.MediaItem
import com.jian.relive.core.ui.theme.ReliveBlack
import com.jian.relive.core.ui.theme.ReliveGray100
import com.jian.relive.core.ui.theme.ReliveGray500
import com.jian.relive.core.ui.theme.ReliveRed500
import com.jian.relive.core.ui.theme.ReliveWhite
import com.jian.relive.feature.organize.R

/**
 * 废纸篓二次清单审核与批量删除确认弹窗。
 *
 * 展示待删除相片微缩网格、支持单张点选剔除保留、反选及二次确认调起安全删除。
 */
@Composable
fun DeleteReviewModal(
    pendingTrashIds: Set<Long>,
    allMedia: List<MediaItem>,
    onConfirm: (Set<Long>) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trashItems = remember(pendingTrashIds, allMedia) {
        allMedia.filter { pendingTrashIds.contains(it.id) }
    }

    var selectedIds by remember(pendingTrashIds) { mutableStateOf(pendingTrashIds) }
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ReliveWhite)
            .padding(horizontal = 16.dp)
            .padding(top = 48.dp, bottom = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(R.string.organize_delete_review_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = ReliveBlack,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Text(
                text = stringResource(R.string.organize_delete_review_subtitle),
                fontSize = 14.sp,
                color = ReliveGray500,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(items = trashItems, key = { it.id }) { item ->
                    val isSelected = selectedIds.contains(item.id)

                    val imageRequest = remember(item.uri) {
                        ImageRequest.Builder(context)
                            .data(item.uri)
                            .decoderFactory(VideoFrameDecoder.Factory())
                            .size(200, 200)
                            .build()
                    }

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                selectedIds = if (isSelected) {
                                    selectedIds - item.id
                                } else {
                                    selectedIds + item.id
                                }
                            }
                    ) {
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(24.dp)
                                .background(
                                    if (isSelected) ReliveRed500 else ReliveWhite.copy(alpha = 0.5f),
                                    CircleShape
                                )
                                .border(1.dp, ReliveWhite, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = stringResource(R.string.organize_selected),
                                    tint = ReliveWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = ReliveGray100),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.organize_cancel),
                        color = ReliveBlack
                    )
                }

                Button(
                    onClick = { onConfirm(selectedIds) },
                    enabled = selectedIds.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ReliveRed500,
                        disabledContainerColor = ReliveGray100
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = if (selectedIds.isNotEmpty()) ReliveWhite else ReliveGray500,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.organize_delete_count, selectedIds.size),
                        color = if (selectedIds.isNotEmpty()) ReliveWhite else ReliveGray500
                    )
                }
            }
        }
    }
}
