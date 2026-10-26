package com.jian.relive.feature.settings.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.relive.core.common.language.AppLanguage
import com.jian.relive.core.ui.animation.bounceClick
import com.jian.relive.core.ui.animation.staggeredRevealModifier
import com.jian.relive.core.ui.composition.LocalHapticsEnabled
import com.jian.relive.core.ui.theme.ReliveBlack
import com.jian.relive.core.ui.theme.ReliveGray100
import com.jian.relive.core.ui.theme.ReliveGray500
import com.jian.relive.core.ui.theme.ReliveWhite
import com.jian.relive.feature.settings.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 设置主界面屏幕。
 */
@Composable
fun SettingsScreen(
    batchSize: Int,
    queueOrder: String,
    muteVideos: Boolean,
    soundEffectsEnabled: Boolean,
    hapticsEnabled: Boolean,
    avatarType: String,
    currentLanguage: AppLanguage,
    onBatchSizeChange: (Int) -> Unit,
    onQueueOrderChange: (String) -> Unit,
    onMuteVideosChange: (Boolean) -> Unit,
    onSoundEffectsChange: (Boolean) -> Unit,
    onHapticsEnabledChange: (Boolean) -> Unit,
    onAvatarChange: (String) -> Unit,
    onReleaseKept: () -> Unit,
    onLanguageClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onOpenSourceClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val layoutDirection = LocalLayoutDirection.current
    val uriHandler = LocalUriHandler.current

    var releasedMessageVisible by remember { mutableStateOf(false) }
    var showReleaseDialog by remember { mutableStateOf(false) }

    if (showReleaseDialog) {
        AlertDialog(
            onDismissRequest = { showReleaseDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.settings_release_dialog_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = stringResource(R.string.settings_release_dialog_content))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReleaseKept()
                        scope.launch {
                            releasedMessageVisible = true
                            delay(2000)
                            releasedMessageVisible = false
                        }
                        showReleaseDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.settings_confirm),
                        color = ReliveBlack,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showReleaseDialog = false }) {
                    Text(
                        text = stringResource(R.string.settings_cancel),
                        color = ReliveGray500
                    )
                }
            },
            containerColor = ReliveWhite,
            textContentColor = ReliveBlack,
            titleContentColor = ReliveBlack
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ReliveWhite)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, bottom = 24.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .bounceClick(onClick = onBack)
                    .clip(CircleShape)
                    .background(ReliveBlack.copy(alpha = 0.05f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.settings_back),
                    tint = ReliveBlack
                )
            }

            Text(
                text = stringResource(R.string.settings_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ReliveBlack,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 56.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_language),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ReliveBlack,
                modifier = Modifier.then(staggeredRevealModifier(0))
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ReliveGray100)
                    .bounceClick(onClick = onLanguageClick)
                    .then(staggeredRevealModifier(1))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentLanguage.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ReliveBlack
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = ReliveGray500
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_privacy_policy),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ReliveBlack,
                modifier = Modifier.then(staggeredRevealModifier(2))
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ReliveGray100)
                    .bounceClick(onClick = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPrivacyClick()
                    })
                    .then(staggeredRevealModifier(3))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_privacy_policy),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ReliveBlack
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = ReliveGray500
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(staggeredRevealModifier(4)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.settings_mute_videos),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ReliveBlack,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = muteVideos,
                    onCheckedChange = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onMuteVideosChange(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ReliveWhite,
                        checkedTrackColor = ReliveBlack,
                        uncheckedThumbColor = ReliveWhite,
                        uncheckedTrackColor = ReliveGray500.copy(alpha = 0.3f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(staggeredRevealModifier(5)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.settings_sound_effects),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ReliveBlack,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = soundEffectsEnabled,
                    onCheckedChange = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSoundEffectsChange(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ReliveWhite,
                        checkedTrackColor = ReliveBlack,
                        uncheckedThumbColor = ReliveWhite,
                        uncheckedTrackColor = ReliveGray500.copy(alpha = 0.3f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(staggeredRevealModifier(6)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.settings_haptics),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ReliveBlack,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = hapticsEnabled,
                    onCheckedChange = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onHapticsEnabledChange(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ReliveWhite,
                        checkedTrackColor = ReliveBlack,
                        uncheckedThumbColor = ReliveWhite,
                        uncheckedTrackColor = ReliveGray500.copy(alpha = 0.3f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.settings_batch_size),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ReliveBlack,
                modifier = Modifier.then(staggeredRevealModifier(7))
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = ReliveGray100),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(staggeredRevealModifier(8))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "10", fontSize = 12.sp, color = ReliveGray500)
                        Text(text = "$batchSize", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ReliveBlack)
                        Text(text = "30", fontSize = 12.sp, color = ReliveGray500)
                    }
                    Slider(
                        value = batchSize.toFloat(),
                        onValueChange = { value ->
                            val rounded = value.roundToInt().coerceIn(10, 30)
                            if (rounded != batchSize) {
                                if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onBatchSizeChange(rounded)
                            }
                        },
                        valueRange = 10f..30f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = ReliveBlack,
                            activeTrackColor = ReliveBlack,
                            inactiveTrackColor = ReliveGray500.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.settings_queue_order),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ReliveBlack,
                modifier = Modifier.then(staggeredRevealModifier(9))
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(staggeredRevealModifier(10)),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = queueOrder == "random",
                    onClick = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onQueueOrderChange("random")
                    },
                    label = { Text(stringResource(R.string.settings_order_random)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ReliveBlack,
                        selectedLabelColor = ReliveWhite,
                        containerColor = ReliveGray100,
                        labelColor = ReliveBlack
                    ),
                    border = null,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .bounceClick()
                )

                FilterChip(
                    selected = queueOrder == "time",
                    onClick = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onQueueOrderChange("time")
                    },
                    label = { Text(stringResource(R.string.settings_order_time)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ReliveBlack,
                        selectedLabelColor = ReliveWhite,
                        containerColor = ReliveGray100,
                        labelColor = ReliveBlack
                    ),
                    border = null,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .bounceClick()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (currentLanguage != AppLanguage.Arabic) {
                Text(
                    text = stringResource(R.string.settings_avatar),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ReliveBlack,
                    modifier = Modifier.then(staggeredRevealModifier(11))
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(staggeredRevealModifier(12)),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = avatarType == "girl",
                        onClick = {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAvatarChange("girl")
                        },
                        label = { Text(stringResource(R.string.settings_avatar_girl)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ReliveBlack,
                            selectedLabelColor = ReliveWhite,
                            containerColor = ReliveGray100,
                            labelColor = ReliveBlack
                        ),
                        border = null,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .bounceClick()
                    )

                    FilterChip(
                        selected = avatarType == "boy",
                        onClick = {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAvatarChange("boy")
                        },
                        label = { Text(stringResource(R.string.settings_avatar_boy)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ReliveBlack,
                            selectedLabelColor = ReliveWhite,
                            containerColor = ReliveGray100,
                            labelColor = ReliveBlack
                        ),
                        border = null,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .bounceClick()
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            } else {
                LaunchedEffect(Unit) {
                    if (avatarType != "boy") {
                        onAvatarChange("boy")
                    }
                }
            }

            Text(
                text = stringResource(R.string.settings_release_kept),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ReliveBlack,
                modifier = Modifier.then(staggeredRevealModifier(13))
            )
            Text(
                text = stringResource(R.string.settings_release_kept_desc),
                fontSize = 14.sp,
                color = ReliveGray500,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .then(staggeredRevealModifier(14))
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showReleaseDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = ReliveBlack),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .then(staggeredRevealModifier(15))
            ) {
                if (releasedMessageVisible) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = ReliveWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_released),
                        color = ReliveWhite
                    )
                } else {
                    Text(
                        text = stringResource(R.string.settings_release_kept),
                        color = ReliveWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .then(staggeredRevealModifier(16)),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(onClick = {
                    uriHandler.openUri("https://itlueqqx8t.feishu.cn/wiki/TOCQw5y08iJQ1FkOAKYcnJEWnlf")
                }) {
                    Text(
                        text = stringResource(R.string.settings_terms_of_service),
                        fontSize = 12.sp,
                        color = ReliveGray500
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                TextButton(onClick = {
                    uriHandler.openUri("https://itlueqqx8t.feishu.cn/wiki/NiG9wQ3LTigcGokeBHEcHvIjngd")
                }) {
                    Text(
                        text = stringResource(R.string.settings_privacy_policy),
                        fontSize = 12.sp,
                        color = ReliveGray500
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(staggeredRevealModifier(17)),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = onOpenSourceClick) {
                    Text(
                        text = stringResource(R.string.settings_open_source_licenses),
                        fontSize = 12.sp,
                        color = ReliveGray500
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(staggeredRevealModifier(18)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${stringResource(R.string.settings_version)} 2.0  ${stringResource(R.string.settings_copyright)} 2026",
                    fontSize = 12.sp,
                    color = ReliveGray500.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
