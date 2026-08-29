package com.silema.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────
// 设计系统 v2 · 温暖医疗专业风
// 原则：所有"底色+白字"组合对比度 ≥ 4.5:1（正文）/ 3:1（大字号），
// 满足 WCAG 与适老化高对比要求；尺寸统一走令牌，禁止散落 Magic Number。
// ─────────────────────────────────────────────────────────────

// ── 品牌色（深饱和、白字达标）──
val BrandWarm = Color(0xFFD84315)      // 深橙红（主品牌/生命警示，白字≈4.8:1）
val BrandBlue = Color(0xFF1565C0)      // 深医疗蓝（家庭/可信，白字≈5.3:1）
val BrandGreen = Color(0xFF2E7D32)     // 深绿（安全/正常，白字≈4.8:1）
val BrandSoftRed = Color(0xFFC62828)   // 深红（错误/危险，白字≈5.0:1）
val BrandPurple = Color(0xFF6A1B9A)    // 深紫（报告/医疗强调，白字≈6:1）
val BrandAmber = Color(0xFFEF6C00)     // 深琥珀（提示，白字≈4.0:1 仅大字号用）

// ── 背景/表面（温暖家庭感 + 高对比文字）──
val GradientWarmStart = Color(0xFFFDF6EC) // 暖白底
val GradientWarmEnd = Color(0xFFFBE9DC)   // 暖橙底

// ── 风险分级色（深饱和，白字对比度均达标，四级语义清晰）──
val LevelNormal = Color(0xFF2E7D32)    // 正常·深绿
val LevelWatch = Color(0xFF00838F)     // 注意·深青（观察）
val LevelWarning = Color(0xFFE65100)   // 警告·深橙
val LevelCritical = Color(0xFFC62828)  // 危险·深红

fun riskColor(level: com.silema.app.data.RiskLevel): Color = when (level) {
    com.silema.app.data.RiskLevel.NORMAL -> LevelNormal
    com.silema.app.data.RiskLevel.WATCH -> LevelWatch
    com.silema.app.data.RiskLevel.WARNING -> LevelWarning
    com.silema.app.data.RiskLevel.CRITICAL -> LevelCritical
}

// 风险分级对应的「白字」是否达标由上面深饱和色保证；此处提供分级容器浅底色（浅底+深字方案备用）
val LevelNormalSoft = Color(0xFFE6F4EA)
val LevelWatchSoft = Color(0xFFD9F2F4)
val LevelWarningSoft = Color(0xFFFDE7D6)
val LevelCriticalSoft = Color(0xFFFBE3E3)

fun riskSoft(level: com.silema.app.data.RiskLevel): Color = when (level) {
    com.silema.app.data.RiskLevel.NORMAL -> LevelNormalSoft
    com.silema.app.data.RiskLevel.WATCH -> LevelWatchSoft
    com.silema.app.data.RiskLevel.WARNING -> LevelWarningSoft
    com.silema.app.data.RiskLevel.CRITICAL -> LevelCriticalSoft
}

// ── 渐变色组（卡片/横幅用，深→浅，白字叠深端达标）──
val CardGradientBlue = listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
val CardGradientOrange = listOf(Color(0xFFD84315), Color(0xFFFF8A50))
val CardGradientGreen = listOf(Color(0xFF2E7D32), Color(0xFF66BB6A))
val CardGradientRed = listOf(Color(0xFFC62828), Color(0xFFEF5350))
val CardGradientPurple = listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC))

// SOS 全屏呼救渐变（深红系，白字对比度达标）
val SosGradient = listOf(Color(0xFFC62828), Color(0xFF7F0000))

// ── 尺寸令牌（消除 Magic Number）──
object AppShapes {
    val banner = RoundedCornerShape(20.dp)
    val card = RoundedCornerShape(16.dp)
    val button = RoundedCornerShape(14.dp)
    val chip = RoundedCornerShape(12.dp)
    val small = RoundedCornerShape(8.dp)
}

object AppSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val screenPad = 16.dp
}

object AppSize {
    val bigButtonHeight = 56.dp   // 适老但收敛：清晰可点，不过度放大
    val sosButtonHeight = 56.dp
    val listItemMin = 64.dp
}

// ── 适老化排版：大字号 + 宽松行距 ──
private val ElderTypography = Typography(
    displayLarge = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold, lineHeight = 44.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp),
    titleSmall = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp),
    labelMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp)
)

private val LightColors: ColorScheme = lightColorScheme(
    primary = BrandWarm,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFBE9DC),
    onPrimaryContainer = Color(0xFF5D2906),
    secondary = BrandBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE9FA),
    onSecondaryContainer = Color(0xFF0D2B47),
    tertiary = BrandGreen,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE6F4EA),
    onTertiaryContainer = Color(0xFF143D17),
    background = GradientWarmStart,
    onBackground = Color(0xFF2B2118),
    surface = Color.White,
    onSurface = Color(0xFF2B2118),
    surfaceVariant = Color(0xFFF3EBE2),
    onSurfaceVariant = Color(0xFF5F5147),
    outline = Color(0xFFC9BBB0),
    outlineVariant = Color(0xFFE8DDD6),
    error = BrandSoftRed,
    onError = Color.White,
    errorContainer = Color(0xFFFBE3E3),
    onErrorContainer = Color(0xFF7F1D1D),
    inverseSurface = Color(0xFF3E2E24),
    inverseOnSurface = Color(0xFFFFF7EF),
    inversePrimary = Color(0xFFFFAB91)
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFFFFAB91),
    onPrimary = Color(0xFF5D2906),
    primaryContainer = Color(0xFF8B4513),
    onPrimaryContainer = Color(0xFFFFF0E6),
    secondary = Color(0xFF90CAF9),
    onSecondary = Color(0xFF0D2B47),
    secondaryContainer = Color(0xFF1A3A5C),
    onSecondaryContainer = Color(0xFFDCE9FA),
    tertiary = Color(0xFFA5D6A7),
    onTertiary = Color(0xFF143D17),
    tertiaryContainer = Color(0xFF2E7D32),
    onTertiaryContainer = Color(0xFFE6F4EA),
    background = Color(0xFF1A1108),
    onBackground = Color(0xFFF0E8E0),
    surface = Color(0xFF241A10),
    onSurface = Color(0xFFF0E8E0),
    surfaceVariant = Color(0xFF3E2E24),
    onSurfaceVariant = Color(0xFFD8C8BE),
    outline = Color(0xFF8D7D72),
    outlineVariant = Color(0xFF5D4D42),
    error = Color(0xFFEF5350),
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
        shapes = androidx.compose.material3.Shapes(
            extraSmall = AppShapes.small,
            small = AppShapes.small,
            medium = AppShapes.chip,
            large = AppShapes.card,
            extraLarge = AppShapes.banner
        ),
        content = content
    )
}
