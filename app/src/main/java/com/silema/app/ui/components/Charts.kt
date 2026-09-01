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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 趋势图组件（折线图 + 渐变填充）。
 *
 * 用于展示健康数据的时间趋势，如心率变化、血压趋势等。
 * 带动画效果，数据变化时平滑过渡。
 */
@Composable
fun TrendChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF00A86B),
    gradientColors: List<Color>? = null,
    showFill: Boolean = true,
    showDots: Boolean = true,
    height: Int = 120,
) {
    if (data.isEmpty()) return

    val max = data.maxOrNull() ?: 1f
    val min = data.minOrNull() ?: 0f
    val range = max - min

    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800),
        label = "trendChart",
    )

    val lineGradient = gradientColors ?: listOf(color, color.copy(alpha = 0.6f))
    val fillGradient = gradientColors ?: listOf(color.copy(alpha = 0.3f), color.copy(alpha = 0.05f))

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height.dp),
    ) {
        val path = Path()
        val fillPath = Path()
        val stepX = size.width / (data.size - 1).coerceAtLeast(1)

        data.forEachIndexed { index, value ->
            val x = index * stepX
            val y = size.height - ((value - min) / range.coerceAtLeast(0.001f)) * size.height * animatedProgress

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            // 数据点
            if (showDots) {
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y),
                )
                drawCircle(
                    color = color,
                    radius = 3.dp.toPx(),
                    center = Offset(x, y),
                )
            }
        }

        // 填充渐变
        if (showFill) {
            fillPath.lineTo(size.width, size.height)
            fillPath.close()
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(fillGradient),
            )
        }

        // 折线
        drawPath(
            path = path,
            brush = Brush.linearGradient(lineGradient),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

/**
 * 柱状图组件。
 *
 * 用于展示分类数据对比，如每日步数、每周运动量等。
 * 带动画效果，柱子从底部生长。
 */
@Composable
fun BarChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF00A86B),
    gradientColors: List<Color>? = null,
    height: Int = 120,
) {
    if (data.isEmpty()) return

    val max = data.maxOfOrNull { it.second } ?: 1f

    val barGradient = gradientColors ?: listOf(color, color.copy(alpha = 0.7f))

    Column(modifier = modifier) {
        // 柱状图区域
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(height.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            data.forEach { (label, value) ->
                val barHeight = (value / max) * height
                val animatedHeight by animateFloatAsState(
                    targetValue = barHeight,
                    animationSpec = tween(durationMillis = 600),
                    label = "barChart_$label",
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // 数值
                    Text(
                        text = value.toInt().toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                    )
                    // 柱子
                    Box(
                        modifier =
                            Modifier
                                .width(24.dp)
                                .height(animatedHeight.dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(Brush.verticalGradient(barGradient)),
                    )
                }
            }
        }

        // 标签
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            data.forEach { (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * 数据卡片组件（带趋势图）。
 *
 * 整合标题、数值、单位、趋势图的完整数据展示卡片。
 */
@Composable
fun DataCardWithChart(
    title: String,
    value: String,
    unit: String,
    trend: String?,
    trendColor: Color,
    chartData: List<Float>,
    chartColor: Color,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (trend != null) {
                    Text(
                        text = trend,
                        style = MaterialTheme.typography.labelMedium,
                        color = trendColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // 数值行
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            // 趋势图
            TrendChart(
                data = chartData,
                color = chartColor,
                height = 80,
            )
        }
    }
}
