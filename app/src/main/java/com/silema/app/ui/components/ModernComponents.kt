package com.silema.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.silema.app.ui.theme.AppElevation
import com.silema.app.ui.theme.AppShapes
import com.silema.app.ui.theme.AppSize
import com.silema.app.ui.theme.LocalSilemaThemeColors

/**
 * 现代进度环组件（参考 Keep 数据展示风格）。
 *
 * 用于展示健康数据完成度、风险等级、目标达成率等。
 * 带渐变色彩和动画效果。
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    strokeWidth: Dp = AppSize.progressRingWidth,
    gradientColors: List<Color>,
    trackColor: Color = MaterialTheme.colorScheme.outlineVariant,
    centerContent: @Composable () -> Unit = {},
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "progressRing",
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            // 背景轨道
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
            )
            // 进度弧（渐变）
            drawArc(
                brush =
                    Brush.linearGradient(
                        colors = gradientColors,
                        start = Offset.Zero,
                        end = Offset(size.toPx(), size.toPx()),
                    ),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
            )
        }
        // 中心内容
        centerContent()
    }
}

/**
 * 玻璃拟态卡片组件（现代毛玻璃质感）。
 *
 * 半透明背景 + 柔和阴影 + 大圆角，用于 Dashboard 数据卡片。
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Unspecified,
    elevation: Dp = AppElevation.medium,
    content: @Composable () -> Unit,
) {
    val themeColors = LocalSilemaThemeColors.current
    val bgColor = if (backgroundColor == Color.Unspecified) {
        themeColors.surfaceGlass
    } else {
        backgroundColor
    }
    Card(
        modifier = modifier,
        shape = AppShapes.card,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
    ) {
        content()
    }
}

/**
 * 现代数据块组件（参考 Keep 首页数据网格）。
 *
 * 图标 + 数值 + 单位 + 标签，紧凑而清晰。
 */
@Composable
fun DataTile(
    icon: ImageVector,
    iconTint: Color,
    value: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier,
    trend: String? = null,
    trendColor: Color = Color(0xFF00A86B),
) {
    GlassCard(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 图标 + 趋势
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp),
                    )
                }
                if (trend != null) {
                    Text(
                        text = trend,
                        style = MaterialTheme.typography.labelSmall,
                        color = trendColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            // 数值 + 单位
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            // 标签
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 渐变横幅组件（用于风险评估、SOS 入口等重要信息）。
 */
@Composable
fun GradientBanner(
    title: String,
    subtitle: String,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(AppShapes.banner),
        shape = AppShapes.banner,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        onClick = onClick ?: {},
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(gradientColors))
                    .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
        }
    }
}

/**
 * 快速操作按钮组件（参考 Keep 底部快捷入口）。
 */
@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(AppShapes.card)
                    .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * 迷你趋势图组件（Sparkline，用于数据卡片内）。
 */
@Composable
fun MiniTrendChart(
    data: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 32.dp,
) {
    if (data.isEmpty()) {
        // 空数据时显示占位线
        Canvas(
            modifier = modifier
                .fillMaxWidth()
                .height(height),
        ) {
            drawLine(
                color = color.copy(alpha = 0.2f),
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        return
    }

    val max = data.maxOrNull() ?: 1f
    val min = data.minOrNull() ?: 0f
    val range = max - min

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height),
    ) {
        val path =
            androidx.compose.ui.graphics
                .Path()
        val stepX = size.width / (data.size - 1).coerceAtLeast(1)

        data.forEachIndexed { index, value ->
            val x = index * stepX
            val y = size.height - ((value - min) / range.coerceAtLeast(0.001f)) * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}
