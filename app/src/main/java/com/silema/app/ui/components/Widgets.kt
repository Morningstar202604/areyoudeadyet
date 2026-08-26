package com.silema.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.silema.app.data.RiskLevel
import com.silema.app.ui.theme.riskColor

@Composable
fun LevelPill(level: RiskLevel) {
    val bg = riskColor(level)
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Text(
            text = level.label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
    }
}

/**
 * 顶部状态横幅：颜色 + 图标 + 大字标题三重表达，
 * 保证色弱老人也能第一时间分辨状态。
 */
@Composable
fun StatusBanner(
    level: RiskLevel,
    headline: String,
    subline: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val bg = riskColor(level)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
        Text(
            text = subline,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.95f),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/** 单个体征卡片：大数值 + 状态描边 + 最近测量时间；level 为 null 表示"还没有数据"。 */
@Composable
fun VitalCard(
    label: String,
    valueText: String,
    timeText: String,
    noteText: String?,
    level: RiskLevel?,
    modifier: Modifier = Modifier
) {
    val accent = level?.let { riskColor(it) } ?: Color(0xFFBDB5AF)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (level == null) 1.dp else 2.dp,
                color = accent,
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (level != null) {
                    LevelPill(level)
                } else {
                    Text(
                        text = "待测量",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = valueText,
                style = MaterialTheme.typography.headlineSmall,
                color = if (level == null) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp),
                maxLines = 1
            )
            Text(
                text = timeText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 1
            )
            if (noteText != null) {
                Text(
                    text = noteText,
                    style = MaterialTheme.typography.bodySmall,
                    color = accent,
                    maxLines = 3,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/** 小统计块（今日概览用）。 */
@Composable
fun StatTile(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 6.dp)
    ) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = valueColor, maxLines = 1)
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/** 简易折线趋势图（Canvas 手绘，零第三方依赖）。 */
@Composable
fun Sparkline(
    values: List<Double>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxWidth().height(120.dp)) {
        if (values.isEmpty()) return@Canvas
        if (values.size == 1) {
            drawCircle(color, radius = 8f, center = Offset(size.width / 2f, size.height / 2f))
            return@Canvas
        }
        val minV = values.min()
        val maxV = values.max()
        val span = (maxV - minV).takeIf { it > 1e-9 } ?: 1.0
        val stepX = size.width / (values.size - 1)
        val padY = 24f
        val usableH = size.height - padY * 2
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y = (padY + usableH * (1f - ((v - minV) / span)).toFloat()).let {
                if (it.isNaN()) size.height / 2f else it
            }
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        // 末点高亮
        val lastX = (values.size - 1) * stepX
        val lastY = (padY + usableH * (1f - ((values.last() - minV) / span))).toFloat()
        drawCircle(color, radius = 12f, center = Offset(lastX, lastY))
    }
}

/** 全宽大按钮：高度 76dp，字号跟随主题放大。 */
@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    container: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = Color.White,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = contentColor
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)
    )
}
