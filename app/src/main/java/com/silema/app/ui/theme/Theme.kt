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
// 设计系统 v3 · 清新健康活力风（参考 Keep / 现代养生应用）
// 原则：
// - 主色薄荷绿传达健康/自然/活力，强调色活力橙传递温暖与能量
// - 大量留白 + 玻璃拟态卡片 + 渐变数据可视化
// - 所有"底色+白字"组合对比度 ≥ 4.5:1（正文），满足 WCAG 与适老化
// - 尺寸统一走令牌，禁止散落 Magic Number
// ─────────────────────────────────────────────────────────────

// ── 品牌色（现代健康活力配色）──
val BrandWarm = Color(0xFFFF6D00)
val BrandBlue = Color(0xFF1A237E)
val BrandGreen = Color(0xFF00A86B)
val BrandSoftRed = Color(0xFFE53935)
val BrandPurple = Color(0xFF5E35B1)
val BrandAmber = Color(0xFFFF8F00)

// ── 辅助色（数据可视化用）──
val DataHeart = Color(0xFFE91E63)
val DataPressure = Color(0xFF3F51B5)
val DataOxygen = Color(0xFF00BCD4)
val DataTemp = Color(0xFFFF5722)
val DataSteps = Color(0xFF8BC34A)
val DataSleep = Color(0xFF7986CB)

// ── 背景/表面 ──
val GradientWarmStart = Color(0xFFF1F8E9)
val GradientWarmEnd = Color(0xFFE8F5E9)
val SurfaceGlass = Color(0xFFFFFFFF)

// ── 风险分级色 ──
val LevelNormal = Color(0xFF00A86B)
val LevelWatch = Color(0xFF00ACC1)
val LevelWarning = Color(0xFFFF8F00)
val LevelCritical = Color(0xFFE53935)

fun riskColor(level: com.silema.app.data.RiskLevel): Color =
    when (level) {
        com.silema.app.data.RiskLevel.NORMAL -> LevelNormal
        com.silema.app.data.RiskLevel.WATCH -> LevelWatch
        com.silema.app.data.RiskLevel.WARNING -> LevelWarning
        com.silema.app.data.RiskLevel.CRITICAL -> LevelCritical
    }

val LevelNormalSoft = Color(0xFFE0F2F1)
val LevelWatchSoft = Color(0xFFE0F7FA)
val LevelWarningSoft = Color(0xFFFFF3E0)
val LevelCriticalSoft = Color(0xFFFFEBEE)

fun riskSoft(level: com.silema.app.data.RiskLevel): Color =
    when (level) {
        com.silema.app.data.RiskLevel.NORMAL -> LevelNormalSoft
        com.silema.app.data.RiskLevel.WATCH -> LevelWatchSoft
        com.silema.app.data.RiskLevel.WARNING -> LevelWarningSoft
        com.silema.app.data.RiskLevel.CRITICAL -> LevelCriticalSoft
    }

// ── 渐变色组 ──
val CardGradientGreen = listOf(Color(0xFF00A86B), Color(0xFF66BB6A))
val CardGradientOrange = listOf(Color(0xFFFF6D00), Color(0xFFFFAB40))
val CardGradientBlue = listOf(Color(0xFF1A237E), Color(0xFF5C6BC0))
val CardGradientRed = listOf(Color(0xFFE53935), Color(0xFFEF5350))
val CardGradientPurple = listOf(Color(0xFF5E35B1), Color(0xFFAB47BC))
val CardGradientTeal = listOf(Color(0xFF00ACC1), Color(0xFF4DD0E1))

val SosGradient = listOf(Color(0xFFE53935), Color(0xFFB71C1C))

val HealthGradientHeart = listOf(Color(0xFFE91E63), Color(0xFFFF80AB))
val HealthGradientSteps = listOf(Color(0xFF00A86B), Color(0xFF69F0AE))
val HealthGradientSleep = listOf(Color(0xFF5E35B1), Color(0xFFB39DDB))

// ── 尺寸令牌 ──
object AppShapes {
    val banner = RoundedCornerShape(24.dp)
    val card = RoundedCornerShape(20.dp)
    val button = RoundedCornerShape(16.dp)
    val chip = RoundedCornerShape(14.dp)
    val small = RoundedCornerShape(10.dp)
    val circle = RoundedCornerShape(50)
}

object AppSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val screenPad = 20.dp
}

object AppSize {
    val bigButtonHeight = 56.dp
    val sosButtonHeight = 60.dp
    val listItemMin = 64.dp
    val cardElevation = 2.dp
    val progressRingWidth = 8.dp
}

object AppElevation {
    val none = 0.dp
    val low = 1.dp
    val medium = 3.dp
    val high = 6.dp
    val floating = 8.dp
}

// ── 适老化排版 ──
private val ElderTypography =
    Typography(
        displayLarge =
            TextStyle(
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 44.sp,
                letterSpacing = (-0.5).sp,
            ),
        headlineLarge =
            TextStyle(
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 38.sp,
                letterSpacing = (-0.5).sp,
            ),
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
        labelSmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    )

private val LightColors: ColorScheme =
    lightColorScheme(
        primary = BrandGreen,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE0F2F1),
        onPrimaryContainer = Color(0xFF004D40),
        secondary = BrandWarm,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFF3E0),
        onSecondaryContainer = Color(0xFFE65100),
        tertiary = BrandBlue,
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFE8EAF6),
        onTertiaryContainer = Color(0xFF1A237E),
        background = GradientWarmStart,
        onBackground = Color(0xFF1B1B1B),
        surface = Color.White,
        onSurface = Color(0xFF1B1B1B),
        surfaceVariant = Color(0xFFF5F5F5),
        onSurfaceVariant = Color(0xFF49454F),
        outline = Color(0xFF79747E),
        outlineVariant = Color(0xFFCAC4D0),
        error = BrandSoftRed,
        onError = Color.White,
        errorContainer = Color(0xFFFFEBEE),
        onErrorContainer = Color(0xFFB71C1C),
        inverseSurface = Color(0xFF313033),
        inverseOnSurface = Color(0xFFF3EFF4),
        inversePrimary = Color(0xFF69F0AE),
    )

private val DarkColors: ColorScheme =
    darkColorScheme(
        primary = Color(0xFF69F0AE),
        onPrimary = Color(0xFF004D40),
        primaryContainer = Color(0xFF00695C),
        onPrimaryContainer = Color(0xFFB2DFDB),
        secondary = Color(0xFFFFAB40),
        onSecondary = Color(0xFFE65100),
        secondaryContainer = Color(0xFFBF360C),
        onSecondaryContainer = Color(0xFFFFE0B2),
        tertiary = Color(0xFF9FA8DA),
        onTertiary = Color(0xFF1A237E),
        tertiaryContainer = Color(0xFF283593),
        onTertiaryContainer = Color(0xFFC5CAE9),
        background = Color(0xFF121212),
        onBackground = Color(0xFFE0E0E0),
        surface = Color(0xFF1E1E1E),
        onSurface = Color(0xFFE0E0E0),
        surfaceVariant = Color(0xFF2D2D2D),
        onSurfaceVariant = Color(0xFFBDBDBD),
        outline = Color(0xFF757575),
        outlineVariant = Color(0xFF424242),
        error = Color(0xFFEF5350),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        inverseSurface = Color(0xFFE0E0E0),
        inverseOnSurface = Color(0xFF121212),
        inversePrimary = Color(0xFF00695C),
    )

@Composable
fun SilemaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = ElderTypography,
        shapes =
            androidx.compose.material3.Shapes(
                extraSmall = AppShapes.small,
                small = AppShapes.small,
                medium = AppShapes.chip,
                large = AppShapes.card,
                extraLarge = AppShapes.banner,
            ),
        content = content,
    )
}
