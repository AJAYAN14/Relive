package com.jian.relive.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ReliveDarkColorScheme = darkColorScheme(
    primary = ReliveWhite,
    onPrimary = ReliveBlack,
    secondary = ReliveGray400,
    onSecondary = ReliveBlack,
    tertiary = ReliveBlue400,
    background = ReliveBlack,
    onBackground = ReliveWhite,
    surface = ReliveGray900,
    onSurface = ReliveWhite,
    surfaceVariant = ReliveGray800,
    onSurfaceVariant = ReliveGray300,
    outline = ReliveGray700,
    error = ReliveRed500,
    onError = ReliveWhite
)

private val ReliveLightColorScheme = lightColorScheme(
    primary = ReliveGray900,
    onPrimary = ReliveWhite,
    secondary = ReliveGray600,
    onSecondary = ReliveWhite,
    tertiary = ReliveBlue500,
    background = ReliveGray50,
    onBackground = ReliveGray900,
    surface = ReliveWhite,
    onSurface = ReliveGray900,
    surfaceVariant = ReliveGray100,
    onSurfaceVariant = ReliveGray700,
    outline = ReliveGray200,
    error = ReliveRed600,
    onError = ReliveWhite
)

@Composable
fun ReliveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ReliveDarkColorScheme else ReliveLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ReliveTypography,
        content = content
    )
}
