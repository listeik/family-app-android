package com.listeik.familyapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

private val LightColors = lightColorScheme(
    primary = Color(0xFF356B4C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEEDB),
    onPrimaryContainer = Color(0xFF173C29),
    secondary = Color(0xFF6C5D87),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E0F5),
    onSecondaryContainer = Color(0xFF332B45),
    tertiary = Color(0xFF925E35),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE7BC),
    onTertiaryContainer = Color(0xFF493116),
    background = Color(0xFFF7F9F4),
    surface = Color(0xFFFFFCF8),
    surfaceVariant = Color(0xFFE9ECE5),
    outline = Color(0xFF737A73),
    outlineVariant = Color(0xFFC3C9C1),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA8D5B7),
    onPrimary = Color(0xFF0A3825),
    primaryContainer = Color(0xFF244D35),
    secondary = Color(0xFFD1C3EC),
    secondaryContainer = Color(0xFF493E5D),
    tertiary = Color(0xFFFFC990),
    tertiaryContainer = Color(0xFF5C4022),
    background = Color(0xFF151713),
    surface = Color(0xFF1B1D19),
    surfaceVariant = Color(0xFF424741),
)

private val FamilyTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 30.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 26.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontSize = 17.sp,
        lineHeight = 23.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    ),
)

private val FamilyShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun FamilyAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = FamilyTypography,
        shapes = FamilyShapes,
        content = content,
    )
}
