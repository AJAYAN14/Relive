package com.jian.relive.feature.organize.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jian.relive.core.common.preferences.PreferencesManager
import com.jian.relive.core.media.MediaAnalysisManager
import com.jian.relive.core.model.MediaItem
import com.jian.relive.core.ui.animation.bounceClick
import com.jian.relive.core.ui.animation.staggeredRevealModifier
import com.jian.relive.core.ui.theme.ReliveBlack
import com.jian.relive.core.ui.theme.ReliveGray100
import com.jian.relive.core.ui.theme.ReliveGray800
import com.jian.relive.core.ui.theme.ReliveGreen500
import com.jian.relive.core.ui.theme.ReliveParticleBlue
import com.jian.relive.core.ui.theme.ReliveParticleRed
import com.jian.relive.core.ui.theme.RelivePurple500
import com.jian.relive.core.ui.theme.ReliveWhite
import com.jian.relive.feature.organize.R
import kotlin.math.ceil
import kotlin.math.max

/**
 * 深度相册分析与整理聚合控制台。
 *
 * 集中展示媒体库离线扫描状态、耗时预估，并提供相似照片、低质照片、视频与照片压缩的专属分类处理入口。
 */
@Composable
fun DeepOrganizeView(
    allMedia: List<MediaItem>,
    onBack: () -> Unit,
    onSimilarClick: () -> Unit,
    onLowQualityClick: () -> Unit,
    onVideoCompressClick: () -> Unit,
    onImageCompressClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isScanning by MediaAnalysisManager.isScanning.collectAsStateWithLifecycle()
    val progress by MediaAnalysisManager.progress.collectAsStateWithLifecycle()
    val scanRatePerMinute by MediaAnalysisManager.scanRatePerMinute.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }

    var lowQualityCount by remember { mutableIntStateOf(0) }
    var videoCount by remember { mutableIntStateOf(0) }
    var imageCompressCount by remember { mutableIntStateOf(0) }
    var similarCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(isScanning, progress, allMedia) {
        lowQualityCount = MediaAnalysisManager.getLowQualityCount()
        similarCount = MediaAnalysisManager.countSimilarPhotos(allMedia)
    }

    LaunchedEffect(allMedia) {
        videoCount = allMedia.count { it.isVideo }
        val thresholdMb = prefs.imageCompressThresholdMb.coerceIn(3, 10)
        val minBytes = thresholdMb * 1024L * 1024L
        imageCompressCount = allMedia.count { it.isPhoto && it.size >= minBytes }
    }

    val percent = (progress * 100).toInt()
    val totalPhotos = allMedia.count { it.isPhoto }
    val remainingPhotos = max(0, totalPhotos - (progress * totalPhotos).toInt())
    val estimatedMinutes = if (scanRatePerMinute > 0f) {
        max(1, ceil(remainingPhotos / scanRatePerMinute).toInt())
    } else {
        max(1, ceil(remainingPhotos / 2000.0).toInt())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ReliveGray100)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 12.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clip(CircleShape)
                    .background(ReliveWhite)
                    .bounceClick(onClick = onBack)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.organize_back),
                    tint = ReliveBlack
                )
            }

            Text(
                text = stringResource(R.string.organize_deep_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ReliveBlack,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 56.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ReliveGray800),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .then(staggeredRevealModifier(0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isScanning) {
                                stringResource(R.string.organize_deep_scanning)
                            } else {
                                stringResource(R.string.organize_deep_complete)
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ReliveWhite
                        )

                        Text(
                            text = "$percent%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ReliveWhite.copy(alpha = 0.9f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = ReliveWhite,
                        trackColor = ReliveWhite.copy(alpha = 0.3f)
                    )

                    if (isScanning) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.organize_deep_desc_detailed, estimatedMinutes),
                            fontSize = 12.sp,
                            color = ReliveWhite.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            FeatureCard(
                title = stringResource(R.string.organize_deep_similar_title),
                subtitle = stringResource(R.string.organize_deep_similar_subtitle),
                backgroundColor = ReliveParticleBlue,
                count = similarCount,
                onClick = onSimilarClick,
                modifier = staggeredRevealModifier(1)
            )

            Spacer(modifier = Modifier.height(12.dp))

            FeatureCard(
                title = stringResource(R.string.organize_deep_low_quality_title),
                subtitle = stringResource(R.string.organize_deep_low_quality_subtitle),
                backgroundColor = ReliveParticleRed,
                count = lowQualityCount,
                onClick = onLowQualityClick,
                modifier = staggeredRevealModifier(2)
            )

            Spacer(modifier = Modifier.height(12.dp))

            FeatureCard(
                title = stringResource(R.string.organize_deep_video_compress_title),
                subtitle = stringResource(R.string.organize_deep_video_compress_subtitle),
                backgroundColor = RelivePurple500,
                count = videoCount,
                onClick = onVideoCompressClick,
                modifier = staggeredRevealModifier(3)
            )

            Spacer(modifier = Modifier.height(12.dp))

            FeatureCard(
                title = stringResource(R.string.organize_deep_image_compress_title),
                subtitle = stringResource(R.string.organize_deep_image_compress_subtitle),
                backgroundColor = ReliveGreen500,
                count = imageCompressCount,
                onClick = onImageCompressClick,
                modifier = staggeredRevealModifier(4)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 深度整理功能卡片。
 */
@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    backgroundColor: Color,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .bounceClick(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ReliveWhite
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = ReliveWhite.copy(alpha = 0.85f),
                    maxLines = 1
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(ReliveWhite.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (count == 0) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = ReliveWhite,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = if (count > 99) "99+" else count.toString(),
                        color = ReliveWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
