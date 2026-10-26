package com.jian.relive.feature.clean.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.relive.core.common.util.formatFileSize
import com.jian.relive.core.model.MediaItem
import com.jian.relive.core.ui.animation.bounceClick
import com.jian.relive.core.ui.theme.ReliveBlack
import com.jian.relive.core.ui.theme.ReliveBlue500
import com.jian.relive.core.ui.theme.ReliveGray100
import com.jian.relive.core.ui.theme.ReliveGray200
import com.jian.relive.core.ui.theme.ReliveGray400
import com.jian.relive.core.ui.theme.ReliveGray600
import com.jian.relive.core.ui.theme.ReliveRed600
import com.jian.relive.core.ui.theme.ReliveWarningRedBg
import com.jian.relive.core.ui.theme.ReliveWhite
import com.jian.relive.feature.clean.CleanViewModel
import com.jian.relive.feature.clean.R
import com.jian.relive.feature.clean.model.SimilarPhotoGroup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 相似照片智能对比与排重操作页面。
 */
@Composable
fun SimilarPhotosSection(
    viewModel: CleanViewModel,
    similarGroups: List<SimilarPhotoGroup>,
    isScanning: Boolean,
    scanProgress: Float,
    modifier: Modifier = Modifier
) {
    val currentGroup = similarGroups.firstOrNull()

    if (currentGroup == null) {
        if (isScanning) {
            CleanScanningWaitView(
                progress = scanProgress,
                onStopClick = { viewModel.stopScan() },
                modifier = modifier
            )
        } else {
            CleanEmptyStateView(
                title = stringResource(R.string.clean_no_similar_found),
                description = stringResource(R.string.clean_similar_done_description),
                onDoneClick = { viewModel.loadAndStartScan() },
                modifier = modifier
            )
        }
        return
    }

    var keptId by remember(currentGroup.id) { mutableStateOf<Long?>(null) }
    val pair = currentGroup.photos.take(2)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ReliveGray100)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.clean_similar_count, similarGroups.size),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ReliveBlack
                )

                if (isScanning) {
                    Text(
                        text = stringResource(R.string.clean_scanning),
                        fontSize = 12.sp,
                        color = ReliveGray600
                    )
                }
            }

            if (isScanning) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { scanProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape),
                    color = ReliveBlue500,
                    trackColor = ReliveGray200
                )
            }
        }

        Text(
            text = stringResource(R.string.clean_select_best),
            fontSize = 13.sp,
            color = ReliveGray600,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { item ->
                    val isSelected = keptId == item.id

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(9f / 16f)
                            .bounceClick(onClick = {
                                keptId = if (keptId == item.id) null else item.id
                            })
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(20.dp),
                            shadowElevation = 8.dp,
                            color = ReliveWhite,
                            border = if (isSelected) BorderStroke(3.dp, ReliveBlue500) else null
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    CleanMediaThumbnail(
                                        item = item,
                                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(10.dp)
                                                .background(ReliveBlue500, CircleShape)
                                                .size(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = ReliveWhite,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(ReliveWhite)
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val date = Date(item.dateAdded * 1000)
                                    val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                    Text(
                                        text = format.format(date),
                                        fontSize = 10.sp,
                                        color = ReliveGray600,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = formatFileSize(item.size),
                                        fontSize = 10.sp,
                                        color = ReliveGray400,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    if (keptId != null) {
                        val unkept = pair.filter { it.id != keptId }.map { it.id }.toSet()
                        viewModel.onSimilarDecision(unkept, currentGroup)
                    }
                },
                enabled = keptId != null,
                colors = ButtonDefaults.buttonColors(containerColor = ReliveBlack),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp)
            ) {
                Text(
                    text = stringResource(R.string.clean_keep_selected),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.onSimilarDecision(emptySet(), currentGroup)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ReliveWhite,
                        contentColor = ReliveBlack
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.clean_keep_all),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = {
                        val allInPair = pair.map { it.id }.toSet()
                        viewModel.onSimilarDecision(allInPair, currentGroup)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ReliveWarningRedBg,
                        contentColor = ReliveRed600
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.clean_delete_all),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
