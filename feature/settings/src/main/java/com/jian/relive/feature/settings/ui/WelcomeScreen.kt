package com.jian.relive.feature.settings.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.relive.core.common.language.AppLanguage
import com.jian.relive.core.ui.animation.bounceClick
import com.jian.relive.core.ui.animation.staggeredRevealModifier
import com.jian.relive.core.ui.composition.LocalHapticsEnabled
import com.jian.relive.core.ui.theme.ReliveBlack
import com.jian.relive.core.ui.theme.ReliveBlue100
import com.jian.relive.core.ui.theme.ReliveBlue500
import com.jian.relive.core.ui.theme.ReliveBlueSky600
import com.jian.relive.core.ui.theme.ReliveGray100
import com.jian.relive.core.ui.theme.ReliveGray200
import com.jian.relive.core.ui.theme.ReliveGray500
import com.jian.relive.core.ui.theme.RelivePrimary
import com.jian.relive.core.ui.theme.ReliveGray600
import com.jian.relive.core.ui.theme.ReliveGray900
import com.jian.relive.core.ui.theme.ReliveGreen100
import com.jian.relive.core.ui.theme.ReliveGreen600
import com.jian.relive.core.ui.theme.ReliveIndigo100
import com.jian.relive.core.ui.theme.ReliveIndigo700
import com.jian.relive.core.ui.theme.ReliveParticleBlue
import com.jian.relive.core.ui.theme.ReliveParticleGold
import com.jian.relive.core.ui.theme.ReliveParticleGreen
import com.jian.relive.core.ui.theme.ReliveParticleLightBlue
import com.jian.relive.core.ui.theme.ReliveParticleOrange
import com.jian.relive.core.ui.theme.ReliveParticleRed
import com.jian.relive.core.ui.theme.ReliveParticleTeal
import com.jian.relive.core.ui.theme.ReliveWhite
import com.jian.relive.feature.settings.R
import java.util.UUID
import kotlin.math.absoluteValue

/**
 * 首次启动引导与权限申请屏幕。
 */
@Composable
fun WelcomeScreen(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    hasManageStorage: Boolean,
    onRequestManageStorage: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    var step by remember { mutableIntStateOf(0) }
    val maxStep = 4
    var isAgreementChecked by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ReliveGray100)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        StepIndicator(current = step, total = maxStep + 1)
        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInHorizontally { it * direction } + fadeIn())
                    .togetherWith(slideOutHorizontally { -it * direction } + fadeOut())
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { target ->
            when (target) {
                0 -> WelcomeIntroCard(
                    title = stringResource(R.string.settings_welcome_title),
                    subtitle = stringResource(R.string.settings_welcome_subtitle)
                )
                1 -> WelcomeLanguageStep(
                    title = stringResource(R.string.settings_welcome_lang_title),
                    subtitle = stringResource(R.string.settings_welcome_lang_subtitle),
                    currentLanguage = currentLanguage,
                    onLanguageSelected = onLanguageSelected
                )
                2 -> WelcomeAgreementStep(
                    onAgreementChanged = { isAgreementChecked = it },
                    isChecked = isAgreementChecked
                )
                3 -> WelcomePermissionStep(
                    title = if (!hasPermission) {
                        stringResource(R.string.settings_welcome_perm_title)
                    } else {
                        stringResource(R.string.settings_welcome_manage_storage_title)
                    },
                    subtitle = if (!hasPermission) {
                        stringResource(R.string.settings_welcome_perm_subtitle)
                    } else {
                        stringResource(R.string.settings_welcome_manage_storage_desc)
                    },
                    buttonLabel = if (!hasPermission) {
                        stringResource(R.string.settings_welcome_grant_perm)
                    } else {
                        stringResource(R.string.settings_welcome_manage_storage_title)
                    },
                    grantedLabel = stringResource(R.string.settings_welcome_perm_granted),
                    hasPermission = hasPermission,
                    hasManageStorage = hasManageStorage,
                    onRequestPermission = onRequestPermission,
                    onRequestManageStorage = onRequestManageStorage
                )
                else -> WelcomeStartCard(
                    title = stringResource(R.string.settings_welcome_start_title),
                    subtitle = stringResource(R.string.settings_welcome_start_subtitle)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step > 0) {
                TextButton(
                    onClick = { step = (step - 1).coerceAtLeast(0) },
                    modifier = Modifier
                        .height(44.dp)
                        .bounceClick(onClick = { step = (step - 1).coerceAtLeast(0) })
                ) {
                    Text(
                        text = stringResource(R.string.settings_back),
                        fontSize = 14.sp,
                        color = ReliveBlack
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(64.dp))
            }

            val isLast = step == maxStep
            val canContinue = when (step) {
                2 -> isAgreementChecked
                3 -> hasPermission
                else -> true
            }

            val buttonLabel = if (isLast) {
                stringResource(R.string.settings_welcome_start_button)
            } else {
                stringResource(R.string.settings_welcome_next)
            }

            Button(
                onClick = {
                    if (isLast) {
                        onFinish()
                    } else if (canContinue) {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        step += 1
                    }
                },
                enabled = isLast || canContinue,
                colors = ButtonDefaults.buttonColors(containerColor = RelivePrimary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .height(44.dp)
                    .bounceClick(onClick = {
                        if (isLast) {
                            onFinish()
                        } else if (canContinue) {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            step += 1
                        }
                    })
            ) {
                Text(
                    text = buttonLabel,
                    color = ReliveWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StepIndicator(current: Int, total: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        repeat(total) { index ->
            val isActive = index == current
            Box(
                modifier = Modifier
                    .size(if (isActive) 12.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (isActive) ReliveGray900 else ReliveGray200)
            )
        }
    }
}

@Composable
private fun WelcomeIntroCard(title: String, subtitle: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ReliveWhite),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = ReliveBlack,
                modifier = Modifier.then(staggeredRevealModifier(0))
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = ReliveBlack.copy(alpha = 0.7f),
                lineHeight = 20.sp,
                modifier = Modifier.then(staggeredRevealModifier(1))
            )
        }
    }
}

@Composable
private fun WelcomeLanguageStep(
    title: String,
    subtitle: String,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit
) {
    val pageId = remember { UUID.randomUUID().toString() }

    val colors = listOf(
        ReliveParticleGold,
        ReliveParticleRed,
        ReliveParticleTeal,
        ReliveParticleBlue,
        ReliveParticleGreen,
        ReliveParticleOrange,
        ReliveParticleLightBlue
    )
    val sortedLanguages = remember(currentLanguage) {
        AppLanguage.entries.sortedByDescending { it == currentLanguage }
    }
    val listState = rememberLazyListState()

    LaunchedEffect(currentLanguage) {
        listState.scrollToItem(0)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = ReliveWhite),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ReliveBlack,
                modifier = Modifier.then(staggeredRevealModifier(0, key = pageId + "title"))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = ReliveBlack.copy(alpha = 0.6f),
                modifier = Modifier.then(staggeredRevealModifier(1, key = pageId + "subtitle"))
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(
                    items = sortedLanguages,
                    key = { _, lang -> lang.code }
                ) { index, lang ->
                    val isSelected = lang == currentLanguage
                    val colorIndex = lang.ordinal.absoluteValue % colors.size

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors[colorIndex])
                            .bounceClick(onClick = { onLanguageSelected(lang) })
                            .then(
                                staggeredRevealModifier(
                                    index = index + 2,
                                    key = pageId + lang.name,
                                    skipAnimation = index > 8
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = lang.displayName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ReliveBlack
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(ReliveWhite),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = ReliveBlack,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(26.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeAgreementStep(
    onAgreementChanged: (Boolean) -> Unit,
    isChecked: Boolean
) {
    val uriHandler = LocalUriHandler.current

    Card(
        colors = CardDefaults.cardColors(containerColor = ReliveWhite),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.settings_welcome_terms_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ReliveBlack,
                modifier = Modifier.then(staggeredRevealModifier(0))
            )
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                TextButton(
                    onClick = {
                        uriHandler.openUri("https://itlueqqx8t.feishu.cn/wiki/TOCQw5y08iJQ1FkOAKYcnJEWnlf")
                    },
                    modifier = Modifier.then(staggeredRevealModifier(1))
                ) {
                    Text(
                        text = stringResource(R.string.settings_terms_of_service),
                        fontSize = 14.sp,
                        color = ReliveBlue500
                    )
                }

                TextButton(
                    onClick = {
                        uriHandler.openUri("https://itlueqqx8t.feishu.cn/wiki/NiG9wQ3LTigcGokeBHEcHvIjngd")
                    },
                    modifier = Modifier.then(staggeredRevealModifier(2))
                ) {
                    Text(
                        text = stringResource(R.string.settings_privacy_policy),
                        fontSize = 14.sp,
                        color = ReliveBlue500
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAgreementChanged(!isChecked) }
                    .then(staggeredRevealModifier(3))
                    .padding(vertical = 8.dp)
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { onAgreementChanged(it) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = ReliveGray900,
                        uncheckedColor = ReliveGray500
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.settings_welcome_agree),
                    fontSize = 14.sp,
                    color = ReliveBlack,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun WelcomePermissionStep(
    title: String,
    subtitle: String,
    buttonLabel: String,
    grantedLabel: String,
    hasPermission: Boolean,
    hasManageStorage: Boolean,
    onRequestPermission: () -> Unit,
    onRequestManageStorage: () -> Unit
) {
    val showManageStorage = hasPermission && !hasManageStorage

    Card(
        colors = CardDefaults.cardColors(containerColor = ReliveWhite),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ReliveBlack,
                    modifier = Modifier.then(staggeredRevealModifier(0))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = ReliveBlack.copy(alpha = 0.6f),
                    modifier = Modifier.then(staggeredRevealModifier(1))
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!showManageStorage) {
                        PrivacySummaryRow(
                            title = stringResource(R.string.settings_privacy_sec1_title),
                            content = stringResource(R.string.settings_privacy_sec1_content),
                            icon = Icons.Outlined.Storage,
                            color = ReliveGray100,
                            iconColor = ReliveGray600,
                            modifier = Modifier.then(staggeredRevealModifier(2))
                        )
                        PrivacySummaryRow(
                            title = stringResource(R.string.settings_privacy_sec2_title),
                            content = stringResource(R.string.settings_privacy_sec2_content),
                            icon = Icons.Outlined.Security,
                            color = ReliveGreen100,
                            iconColor = ReliveGreen600,
                            modifier = Modifier.then(staggeredRevealModifier(3))
                        )
                        PrivacySummaryRow(
                            title = stringResource(R.string.settings_privacy_sec3_title),
                            content = stringResource(R.string.settings_privacy_sec3_content),
                            icon = Icons.Outlined.Lock,
                            color = ReliveBlue100,
                            iconColor = ReliveBlueSky600,
                            modifier = Modifier.then(staggeredRevealModifier(4))
                        )
                        PrivacySummaryRow(
                            title = stringResource(R.string.settings_privacy_sec4_title),
                            content = stringResource(R.string.settings_privacy_sec4_content),
                            icon = Icons.Outlined.Code,
                            color = ReliveIndigo100,
                            iconColor = ReliveIndigo700,
                            modifier = Modifier.then(staggeredRevealModifier(5))
                        )
                    } else {
                        PrivacySummaryRow(
                            title = stringResource(R.string.settings_welcome_manage_storage_title),
                            content = stringResource(R.string.settings_welcome_manage_storage_desc),
                            icon = Icons.Outlined.Storage,
                            color = ReliveGray100,
                            iconColor = ReliveGray600,
                            modifier = Modifier.then(staggeredRevealModifier(2))
                        )
                    }
                }
            }
            Button(
                onClick = {
                    if (!hasPermission) onRequestPermission()
                    else if (showManageStorage) onRequestManageStorage()
                },
                enabled = !hasPermission || showManageStorage,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!hasPermission || showManageStorage) RelivePrimary else ReliveGray200
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text(
                    text = if (!hasPermission) {
                        buttonLabel
                    } else if (showManageStorage) {
                        stringResource(R.string.settings_welcome_manage_storage_title)
                    } else {
                        grantedLabel
                    },
                    color = if (!hasPermission || showManageStorage) ReliveWhite else ReliveBlack.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun WelcomeStartCard(title: String, subtitle: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ReliveWhite),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = ReliveBlack,
                modifier = Modifier.then(staggeredRevealModifier(0))
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = ReliveBlack.copy(alpha = 0.7f),
                lineHeight = 20.sp,
                modifier = Modifier.then(staggeredRevealModifier(1))
            )
        }
    }
}

@Composable
private fun PrivacySummaryRow(
    title: String,
    content: String,
    icon: ImageVector,
    color: Color,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ReliveBlack.copy(alpha = 0.8f)
            )
            Text(
                text = content,
                fontSize = 11.sp,
                color = ReliveBlack.copy(alpha = 0.6f),
                lineHeight = 16.sp
            )
        }
    }
}
