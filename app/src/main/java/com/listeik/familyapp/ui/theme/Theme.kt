package com.listeik.familyapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF236A4B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC2EAD4),
    onPrimaryContainer = Color(0xFF082116),
    secondary = Color(0xFF59633A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E8B7),
    tertiary = Color(0xFF8A4F20),
    tertiaryContainer = Color(0xFFFFDBC2),
    background = Color(0xFFF8FAF7),
    surface = Color(0xFFF8FAF7),
    surfaceVariant = Color(0xFFE1E5E0),
    outline = Color(0xFF717873),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA7D0BA),
    onPrimary = Color(0xFF0A3825),
    primaryContainer = Color(0xFF24513A),
    secondary = Color(0xFFC4CC9D),
    tertiary = Color(0xFFFFB781),
    background = Color(0xFF111412),
    surface = Color(0xFF111412),
    surfaceVariant = Color(0xFF414743),
)

@Composable
fun FamilyAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
