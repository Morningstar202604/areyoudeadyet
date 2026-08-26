package com.silema.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 品牌色
val BrandRed = Color(0xFFB71C1C)
val BrandRedBright = Color(0xFFD32F2F)

// 风险分级色（高对比、色弱友好：同时用文字表达）
val LevelNormal = Color(0xFF2E7D32)
val LevelWatch = Color(0xFF9A6700)
val LevelWarning = Color(0xFFE65100)
val LevelCritical = Color(0xFFB71C1C)

fun riskColor(level: com.silema.app.data.RiskLevel): Color = when (level) {
    com.silema.app.data.RiskLevel.NORMAL -> LevelNormal
    com.silema.app.data.RiskLevel.WATCH -> LevelWatch
    com.silema.app.data.RiskLevel.WARNING -> LevelWarning
    com.silema.app.data.RiskLevel.CRITICAL -> LevelCritical
}

// 适老化排版：整体字号比常规 App 大，正文不低于 20sp，同时保证小屏不溢出
private val ElderTypography = Typography(
    headlineLarge = TextStyle(fontSize = 38.sp, fontWeight = FontWeight.Bold, lineHeight = 46.sp),
    headlineMedium = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp),
    headlineSmall = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp),
    titleLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp),
    titleMedium = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.SemiBold, lineHeight = 29.sp),
    titleSmall = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Medium, lineHeight = 27.sp),
    bodyLarge = TextStyle(fontSize = 21.sp, lineHeight = 31.sp),
    bodyMedium = TextStyle(fontSize = 19.sp, lineHeight = 28.sp),
    bodySmall = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 16.sp),
    labelSmall = TextStyle(fontSize = 14.sp)
)

private val LightColors: ColorScheme = lightColorScheme(
    primary = BrandRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF775652),
    onSecondary = Color.White,
    background = Color(0xFFFFF7EF),
    onBackground = Color(0xFF221916),
    surface = Color.White,
    onSurface = Color(0xFF221916),
    surfaceVariant = Color(0xFFF5DDDA),
    onSurfaceVariant = Color(0xFF534340),
    outline = Color(0xFF857370),
    error = LevelCritical,
    onError = Color.White
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFF8B1010),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFE7BDB7),
    onSecondary = Color(0xFF442925),
    background = Color(0xFF1A110E),
    onBackground = Color(0xFFF1DFDC),
    surface = Color(0xFF241A17),
    onSurface = Color(0xFFF1DFDC),
    surfaceVariant = Color(0xFF534340),
    onSurfaceVariant = Color(0xFFD8C2BE),
    outline = Color(0xFFA08C89),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun SilemaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = ElderTypography,
        content = content
    )
}
