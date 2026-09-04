package com.silema.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.silema.app.data.RiskLevel
import com.silema.app.ui.theme.AppShapes
import com.silema.app.ui.theme.AppSize
import com.silema.app.ui.theme.AppSpacing
import com.silema.app.ui.theme.riskColor

// ── 圆形风险徽章（深饱和底色 + 白字，对比度达标）──
@Composable
fun LevelBadge(
    level: RiskLevel,
    modifier: Modifier = Modifier,
) {
    val bg = riskColor(level)
    Box(
        modifier =
            modifier
                .clip(CircleShape)
                .background(bg)
                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = level.label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ── 渐变状态横幅（卡片式）──
@Composable
fun StatusBanner(
    level: RiskLevel,
    headline: String,
    subline: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.banner,
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(gradientColors))
                    .padding(AppSpacing.xxl),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Spacer(modifier = Modifier.width(AppSpacing.lg))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    Text(
                        text = subline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f),
                    )
                }
            }
        }
    }
}

// ── 专业体征卡片 ──
@Composable
fun VitalCard(
    label: String,
    valueText: String,
    timeText: String,
    noteText: String?,
    level: RiskLevel?,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val accent = level?.let { riskColor(it) } ?: MaterialTheme.colorScheme.outline
    Card(
        modifier =
            modifier
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .fillMaxWidth(),
        shape = AppShapes.card,
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(AppSpacing.lg)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (icon != null) {
                    Box(
                        modifier =
                            Modifier
                                .size(36.dp)
                                .clip(AppShapes.small)
                                .background(accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(AppSpacing.sm))
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (level != null) LevelBadge(level)
            }
            Spacer(modifier = Modifier.height(AppSpacing.md))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color =
                        if (level != null) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    maxLines = 1,
                )
            }
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = timeText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            if (noteText != null) {
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                Text(
                    text = noteText,
                    style = MaterialTheme.typography.bodySmall,
                    color = accent,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ── 统计磁贴（带图标+大数字）──
@Composable
fun StatTile(
    label: String,
    value: String,
    valueColor: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = AppShapes.chip,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = AppSpacing.lg, horizontal = AppSpacing.sm),
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = valueColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = valueColor,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

// ── 渐变卡片（首页特殊区域用）──
@Composable
fun GradientCard(
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.card,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(gradientColors)),
        ) {
            content()
        }
    }
}

// ── 简易折线趋势图 ──
@Composable
fun Sparkline(
    values: List<Double>,
    color: Color,
    modifier: Modifier = Modifier,
    fillColor: Color? = null,
) {
    Canvas(modifier = modifier.fillMaxWidth().height(100.dp)) {
        if (values.isEmpty()) return@Canvas
        if (values.size == 1) {
            drawCircle(color, radius = 8f, center = Offset(size.width / 2f, size.height / 2f))
            return@Canvas
        }
        val minV = values.min()
        val maxV = values.max()
        val span = (maxV - minV).takeIf { it > 1e-9 } ?: 1.0
        val stepX = size.width / (values.size - 1)
        val padY = 20f
        val usableH = size.height - padY * 2
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y =
                (padY + usableH * (1f - ((v - minV) / span)).toFloat()).let {
                    if (it.isNaN()) size.height / 2f else it
                }
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        if (fillColor != null) {
            val fillPath =
                Path().apply {
                    addPath(path)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
            drawPath(fillPath, fillColor.copy(alpha = 0.1f))
        }
        val strokePx = 2.5.dp.toPx()
        drawPath(path, color, style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val lastX = (values.size - 1) * stepX
        val lastY = (padY + usableH * (1f - ((values.last() - minV) / span))).toFloat()
        drawCircle(color, radius = strokePx * 1.8f, center = Offset(lastX, lastY))
    }
}

// ── 进度环（圆弧进度）──
@Composable
fun ProgressRing(
    progress: Float,
    color: Color,
    trackColor: Color,
    sizeDp: Int,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(sizeDp.dp)) {
        val stroke = 10.dp.toPx()
        val sweepAngle = 360f * progress.coerceIn(0f, 1f)
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

// ── 全宽大按钮（适老化 76dp）──
@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    container: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = Color.White,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = AppShapes.button,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = container,
                contentColor = contentColor,
            ),
        modifier =
            modifier
                .fillMaxWidth()
                .height(AppSize.bigButtonHeight),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(AppSpacing.sm))
        }
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

// ── 段落标题 ──
@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = AppSpacing.lg, bottom = AppSpacing.sm),
    )
}

// ── 列表项卡片 ──
@Composable
fun ListItemCard(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = AppShapes.chip,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
        ) {
            if (icon != null) {
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(AppShapes.chip)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(modifier = Modifier.width(AppSpacing.md))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            trailing?.invoke()
        }
    }
}

// ── 信息条 ──
@Composable
fun InfoBar(
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .clip(AppShapes.chip)
                .background(containerColor)
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
        )
    }
}

// ── 空状态占位 ──
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(56.dp),
        )
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(AppSpacing.xs))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
