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

// ── 品牌色 ──────────────────────────────────────────────
val BrandWarm = Color(0xFFFF8A65)        // 柔和橙（主品牌色）
val BrandBlue = Color(0xFF5C9CE6)        // 家庭蓝
val BrandGreen = Color(0xFF66BB6A)       // 安心绿
val BrandSoftRed = Color(0xFFE57373)     // 柔和红（非刺眼）
val BrandPurple = Color(0xFF9575CD)      // 柔和紫
val BrandAmber = Color(0xFFFFB74D)       // 暖琥珀

// ── 背景渐变色 ────────────────────────────────────────
val GradientWarmStart = Color(0xFFFFF8F0) // 暖白底
val GradientWarmEnd = Color(0xFFFFF0E6)   // 暖橙底

// ── 风险分级色 ────────────────────────────────────────
val LevelNormal = Color(0xFF66BB6A)
val LevelWatch = Color(0xFFFFB74D)
val LevelWarning = Color(0xFFFF8A65)
val LevelCritical = Color(0xFFE57373)

fun riskColor(level: com.silema.app.data.RiskLevel): Color = when (level) {
    com.silema.app.data.RiskLevel.NORMAL -> LevelNormal
    com.silema.app.data.RiskLevel.WATCH -> LevelWatch
    com.silema.app.data.RiskLevel.WARNING -> LevelWarning
    com.silema.app.data.RiskLevel.CRITICAL -> LevelCritical
}

// ── 渐变色组（卡片/横幅用）──────────────────────────
val CardGradientBlue = listOf(Color(0xFF5C9CE6), Color(0xFF7AB8F5))
val CardGradientOrange = listOf(Color(0xFFFF8A65), Color(0xFFFFAB91))
val CardGradientGreen = listOf(Color(0xFF66BB6A), Color(0xFF81C784))
val CardGradientRed = listOf(Color(0xFFE57373), Color(0xFFEF9A9A))
val CardGradientPurple = listOf(Color(0xFF9575CD), Color(0xFFB39DDB))

// ── 适老化排版：大字号 + 宽松行距 ────────────────────
private val ElderTypography = Typography(
    headlineLarge = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold, lineHeight = 44.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp),
    titleMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
    titleSmall = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium, lineHeight = 26.sp),
    bodyLarge = TextStyle(fontSize = 20.sp, lineHeight = 30.sp),
    bodyMedium = TextStyle(fontSize = 18.sp, lineHeight = 27.sp),
    bodySmall = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp),
    labelMedium = TextStyle(fontSize = 15.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
)

private val LightColors: ColorScheme = lightColorScheme(
    primary = BrandWarm,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFF0E6),
    onPrimaryContainer = Color(0xFF5D2906),
    secondary = BrandBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE9FA),
    onSecondaryContainer = Color(0xFF1A3A5C),
    tertiary = BrandGreen,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8F5E9),
    onTertiaryContainer = Color(0xFF1B5E20),
    background = GradientWarmStart,
    onBackground = Color(0xFF2D2016),
    surface = Color.White,
    onSurface = Color(0xFF2D2016),
    surfaceVariant = Color(0xFFF5EDE8),
    onSurfaceVariant = Color(0xFF6D5D52),
    outline = Color(0xFFBFB3AA),
    outlineVariant = Color(0xFFE8DDD6),
    error = BrandSoftRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFF7F1D1D),
    inverseSurface = Color(0xFF3E2E24),
    inverseOnSurface = Color(0xFFFFF7EF),
    inversePrimary = Color(0xFFFFB68A)
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFFFFB68A),
    onPrimary = Color(0xFF5D2906),
    primaryContainer = Color(0xFF8B4513),
    onPrimaryContainer = Color(0xFFFFF0E6),
    secondary = Color(0xFFA8CBF0),
    onSecondary = Color(0xFF1A3A5C),
    secondaryContainer = Color(0xFF2C4A6E),
    onSecondaryContainer = Color(0xFFDCE9FA),
    tertiary = Color(0xFFA5D6A7),
    onTertiary = Color(0xFF1B5E20),
    tertiaryContainer = Color(0xFF2E7D32),
    onTertiaryContainer = Color(0xFFE8F5E9),
    background = Color(0xFF1A1108),
    onBackground = Color(0xFFF0E8E0),
    surface = Color(0xFF241A10),
    onSurface = Color(0xFFF0E8E0),
    surfaceVariant = Color(0xFF3E2E24),
    onSurfaceVariant = Color(0xFFD8C8BE),
    outline = Color(0xFF8D7D72),
    outlineVariant = Color(0xFF5D4D42),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFF0E8E0),
    inverseOnSurface = Color(0xFF1A1108),
    inversePrimary = Color(0xFF8B4513)
)

@Composable
fun SilemaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = ElderTypography,
        content = content
    )
}
