package com.silema.app.wear.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import com.silema.app.data.RiskLevel

// 手表端沿用手机端同一套品牌色与风险分级语义（深饱和、白字高对比），
// 适配 Wear OS 深色表盘背景，适老化：大字号、少层级。
private val WearColors = Colors(
    primary = Color(0xFFD84315),
    primaryVariant = Color(0xFFFF8A50),
    secondary = Color(0xFF1565C0),
    secondaryVariant = Color(0xFF42A5F5),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    error = Color(0xFFC62828),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.White
)

@Composable
fun WearTheme(content: @Composable () -> Unit) {
    // 复用 Wear 默认 Typography（已为圆屏优化），品牌色覆盖到 Colors。
    MaterialTheme(colors = WearColors, content = content)
}

fun riskColor(level: RiskLevel): Color = when (level) {
    RiskLevel.NORMAL -> Color(0xFF2E7D32)
    RiskLevel.WATCH -> Color(0xFF00838F)
    RiskLevel.WARNING -> Color(0xFFE65100)
    RiskLevel.CRITICAL -> Color(0xFFC62828)
}

val riskLabel: (RiskLevel) -> String = { it.label }
