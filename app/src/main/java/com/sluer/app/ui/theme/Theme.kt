package com.sluer.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SluerGreenLight = Color(0xFF7BE3A8)
private val SluerDeep = Color(0xFF0B3D2E)

// AMOLED-friendly dark scheme (true black background) with the Sluer green.
private val DarkColors = darkColorScheme(
    primary = SluerGreenLight,
    onPrimary = Color(0xFF00391F),
    primaryContainer = SluerDeep,
    onPrimaryContainer = Color(0xFFB8F5CE),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE6EAE6),
    surface = Color(0xFF0A0F0C),
    onSurface = Color(0xFFE6EAE6),
    surfaceVariant = Color(0xFF16211A),
    onSurfaceVariant = Color(0xFFB7C6BC),
)

private val LightColors = lightColorScheme(
    primary = SluerDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBFF0D4),
    onPrimaryContainer = Color(0xFF00210F),
    background = Color(0xFFF7FBF8),
    onBackground = Color(0xFF171D19),
    surface = Color(0xFFF7FBF8),
    onSurface = Color(0xFF171D19),
    surfaceVariant = Color(0xFFDCE9E0),
    onSurfaceVariant = Color(0xFF3F4A42),
)

@Composable
fun SluerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
