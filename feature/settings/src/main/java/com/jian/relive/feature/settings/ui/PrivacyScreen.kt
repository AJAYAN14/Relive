package com.jian.relive.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.relive.core.ui.animation.bounceClick
import com.jian.relive.core.ui.animation.staggeredRevealModifier
import com.jian.relive.core.ui.theme.ReliveBlack
import com.jian.relive.core.ui.theme.ReliveBlue100
import com.jian.relive.core.ui.theme.ReliveBlueSky600
import com.jian.relive.core.ui.theme.ReliveGray100
import com.jian.relive.core.ui.theme.ReliveGray600
import com.jian.relive.core.ui.theme.ReliveGreen100
import com.jian.relive.core.ui.theme.ReliveGreen600
import com.jian.relive.core.ui.theme.ReliveIndigo100
import com.jian.relive.core.ui.theme.ReliveIndigo700
import com.jian.relive.core.ui.theme.ReliveWhite
import com.jian.relive.feature.settings.R

/**
 * 隐私政策屏幕。
 */
@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

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
                text = stringResource(R.string.settings_privacy_policy),
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
            Spacer(modifier = Modifier.height(16.dp))

            PrivacyCard(
                title = stringResource(R.string.settings_privacy_sec1_title),
                content = stringResource(R.string.settings_privacy_sec1_content),
                icon = Icons.Outlined.Storage,
                color = ReliveGray100,
                iconColor = ReliveGray600,
                modifier = Modifier.then(staggeredRevealModifier(0))
            )

            Spacer(modifier = Modifier.height(24.dp))

            PrivacyCard(
                title = stringResource(R.string.settings_privacy_sec2_title),
                content = stringResource(R.string.settings_privacy_sec2_content),
                icon = Icons.Outlined.Security,
                color = ReliveGreen100,
                iconColor = ReliveGreen600,
                modifier = Modifier.then(staggeredRevealModifier(1))
            )

            Spacer(modifier = Modifier.height(24.dp))

            PrivacyCard(
                title = stringResource(R.string.settings_privacy_sec3_title),
                content = stringResource(R.string.settings_privacy_sec3_content),
                icon = Icons.Outlined.Lock,
                color = ReliveBlue100,
                iconColor = ReliveBlueSky600,
                modifier = Modifier.then(staggeredRevealModifier(2))
            )

            Spacer(modifier = Modifier.height(24.dp))

            PrivacyCard(
                title = stringResource(R.string.settings_privacy_sec4_title),
                content = stringResource(R.string.settings_privacy_sec4_content),
                icon = Icons.Outlined.Code,
                color = ReliveIndigo100,
                iconColor = ReliveIndigo700,
                modifier = Modifier.then(staggeredRevealModifier(3)),
                onClick = {
                    uriHandler.openUri("https://github.com/xigua222/photoo")
                }
            )

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun PrivacyCard(
    title: String,
    content: String,
    icon: ImageVector,
    color: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(color)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ReliveBlack.copy(alpha = 0.8f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = content,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            color = ReliveBlack.copy(alpha = 0.7f)
        )
    }
}
