package com.campus.service.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CampusColors = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = Color(0xFF00796B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDEBE5),
    onSecondaryContainer = Color(0xFF00201C),
    tertiary = Color(0xFFEF6C00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0B2),
    onTertiaryContainer = Color(0xFF3E2000)
)

@Composable
fun CampusTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CampusColors, content = content)
}