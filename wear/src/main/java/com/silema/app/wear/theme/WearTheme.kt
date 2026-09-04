package com.silema.app.wear.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Typography
import com.silema.app.data.RiskLevel

// ── Ambient Mode 状态 ──
@Immutable
data class WearAmbientState(
    val isAmbient: Boolean,
)

val LocalWearAmbientState =
    staticCompositionLocalOf {
        WearAmbientState(isAmbient = false)
    }

// ── 颜色定义 ──
private val WearColors =
    Colors(
        primary = Color(0xFF69F0AE),
        primaryVariant = Color(0xFF00E676),
        secondary = Color(0xFF42A5F5),
        secondaryVariant = Color(0xFF80D8FF),
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E1E),
        error = Color(0xFFEF5350),
        onPrimary = Color.Black,
        onSecondary = Color.Black,
        onBackground = Color.White,
        onSurface = Color.White,
        onError = Color.Black,
    )

private val WearAmbientColors =
    Colors(
        primary = Color(0xFF80CBC4),
        primaryVariant = Color(0xFF80CBC4),
        secondary = Color(0xFF90CAF9),
        secondaryVariant = Color(0xFF90CAF9),
        background = Color.Black,
        surface = Color.Black,
        error = Color(0xFFE57373),
        onPrimary = Color.Black,
        onSecondary = Color.Black,
        onBackground = Color.White,
        onSurface = Color.White,
        onError = Color.Black,
    )

// ── 适老化大字体 ──
private val WearTypography =
    Typography(
        defaultFontFamily = androidx.compose.ui.text.font.FontFamily.Default,
    )

@Composable
fun WearTheme(
    ambientState: WearAmbientState = WearAmbientState(isAmbient = false),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalWearAmbientState provides ambientState) {
        MaterialTheme(
            colors = if (ambientState.isAmbient) WearAmbientColors else WearColors,
            typography = WearTypography,
            content = content,
        )
    }
}

fun riskColor(level: RiskLevel): Color =
    when (level) {
        RiskLevel.NORMAL -> Color(0xFF69F0AE)
        RiskLevel.WATCH -> Color(0xFF80DEEA)
        RiskLevel.WARNING -> Color(0xFFFFB74D)
        RiskLevel.CRITICAL -> Color(0xFFEF5350)
    }

val riskLabel: (RiskLevel) -> String = { it.label }
