package com.jian.relive.feature.compress.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.relive.core.ui.animation.bounceClick
import com.jian.relive.core.ui.theme.ReliveBlack
import com.jian.relive.core.ui.theme.ReliveBlue500
import com.jian.relive.core.ui.theme.ReliveGray100
import com.jian.relive.core.ui.theme.ReliveGray200
import com.jian.relive.core.ui.theme.ReliveGray600
import com.jian.relive.core.ui.theme.ReliveGray900
import com.jian.relive.core.ui.theme.ReliveRed500
import com.jian.relive.core.ui.theme.ReliveWarningRedBg
import com.jian.relive.core.ui.theme.ReliveWarningRedBody
import com.jian.relive.core.ui.theme.ReliveWarningRedText
import com.jian.relive.core.ui.theme.ReliveWhite

@Composable
fun CompressionWarningCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ReliveWarningRedBg),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ReliveWarningRedText
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                color = ReliveWarningRedBody,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun CompressionHeader(
    title: String,
    onBack: () -> Unit,
    backLabel: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .background(ReliveWhite)
                .bounceClick(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = backLabel,
                tint = ReliveBlack
            )
        }
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ReliveBlack,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun CompressionFooter(
    isCompressing: Boolean,
    progress: Float,
    activeName: String?,
    errorMessage: String?,
    actionLabel: String,
    compressingLabel: String,
    enabled: Boolean,
    onCompress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ReliveWhite),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            AnimatedVisibility(
                visible = isCompressing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeName.orEmpty().ifBlank { compressingLabel },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = ReliveGray600,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ReliveBlue500
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = ReliveBlue500,
                        trackColor = ReliveGray200
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    fontSize = 12.sp,
                    color = ReliveRed500,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = onCompress,
                enabled = enabled && !isCompressing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ReliveGray900,
                    disabledContainerColor = ReliveGray200
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .bounceClick(onClick = onCompress)
            ) {
                Text(
                    text = if (isCompressing) compressingLabel else actionLabel,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled && !isCompressing) ReliveWhite else ReliveGray600
                )
            }
        }
    }
}

fun findGridItemIdAtOffset(state: LazyGridState, position: Offset): Long? {
    val info = state.layoutInfo.visibleItemsInfo.firstOrNull { item ->
        val left = item.offset.x
        val top = item.offset.y
        val right = left + item.size.width
        val bottom = top + item.size.height
        position.x >= left && position.x <= right && position.y >= top && position.y <= bottom
    } ?: return null
    val key = info.key
    return if (key is Long) key else null
}
