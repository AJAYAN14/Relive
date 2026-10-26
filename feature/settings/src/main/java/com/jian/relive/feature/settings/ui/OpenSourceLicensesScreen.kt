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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.relive.core.ui.animation.bounceClick
import com.jian.relive.core.ui.theme.ReliveBlack
import com.jian.relive.core.ui.theme.ReliveGray100
import com.jian.relive.core.ui.theme.ReliveGray500
import com.jian.relive.core.ui.theme.ReliveWhite
import com.jian.relive.feature.settings.R

/**
 * 开源软件许可信息数据模型。
 */
data class OpenSourceLibrary(
    val name: String,
    val author: String,
    val license: String,
    val url: String
)

/**
 * 开源协议声明屏幕。
 */
@Composable
fun OpenSourceLicensesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val layoutDirection = LocalLayoutDirection.current
    val uriHandler = LocalUriHandler.current

    val libraries = listOf(
        OpenSourceLibrary("Jetpack Compose", "Google", "Apache 2.0", "https://developer.android.com/jetpack/compose"),
        OpenSourceLibrary("Kotlin", "JetBrains", "Apache 2.0", "https://kotlinlang.org/"),
        OpenSourceLibrary("Coil", "Coil Contributors", "Apache 2.0", "https://coil-kt.github.io/coil/"),
        OpenSourceLibrary("AndroidX Media3", "Google", "Apache 2.0", "https://developer.android.com/media/media3"),
        OpenSourceLibrary("Google ML Kit", "Google", "Apache 2.0", "https://developers.google.com/ml-kit"),
        OpenSourceLibrary("Gson", "Google", "Apache 2.0", "https://github.com/google/gson")
    )

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
                    tint = ReliveBlack,
                    modifier = if (layoutDirection == LayoutDirection.Rtl) {
                        Modifier.size(24.dp)
                    } else {
                        Modifier.size(24.dp)
                    }
                )
            }

            Text(
                text = stringResource(R.string.settings_open_source_licenses),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ReliveBlack,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 56.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            items(items = libraries, key = { it.name }) { lib ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ReliveGray100)
                        .clickable { uriHandler.openUri(lib.url) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = lib.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ReliveBlack
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${lib.author} · ${lib.license}",
                            fontSize = 13.sp,
                            color = ReliveGray500
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        tint = ReliveGray500,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
