package com.scrabbl.ai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFC85A3E),
    onPrimary = Color.White,
    secondary = Color(0xFF3E7BC8),
    background = Color(0xFFFDF6E7),
    surface = Color(0xFFFFF8EC),
    onBackground = Color(0xFF201410),
    onSurface = Color(0xFF201410),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFEF8567),
    onPrimary = Color(0xFF351308),
    secondary = Color(0xFF7DB0EA),
    background = Color(0xFF12100E),
    surface = Color(0xFF1A1613),
    onBackground = Color(0xFFF3E7D3),
    onSurface = Color(0xFFF3E7D3),
)

@Composable
fun ScrabblTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
