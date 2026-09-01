package com.silema.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silema.app.data.RiskLevel
import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalType
import com.silema.app.engine.RiskEngine
import com.silema.app.ui.components.GlassCard
import com.silema.app.ui.components.GradientBanner
import com.silema.app.ui.components.ProgressRing
import com.silema.app.ui.theme.AppSpacing
import com.silema.app.ui.theme.BrandGreen
import com.silema.app.ui.theme.CardGradientGreen
import com.silema.app.ui.theme.CardGradientOrange
import com.silema.app.ui.theme.DataHeart
import com.silema.app.ui.theme.DataOxygen
import com.silema.app.ui.theme.DataPressure
import com.silema.app.ui.theme.DataSteps
import com.silema.app.ui.theme.HealthGradientSteps
import com.silema.app.ui.theme.riskColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 健康报告屏幕 V3 — 现代健康活力风。
 *
 * 展示健康评分、各项指标趋势、风险评估、导出报告。
 */
@Composable
fun ReportScreenV3(records: List<VitalRecord>) {
    val assessment = remember(records) { RiskEngine.evaluate(records) }
    val latest =
        remember(records) {
            records.groupBy { it.typeId }.mapValues { (_, list) -> list.maxByOrNull { it.timestampMillis } }
        }

    val healthScore =
        when (assessment.level) {
            RiskLevel.NORMAL -> 85
            RiskLevel.WATCH -> 70
            RiskLevel.WARNING -> 55
            RiskLevel.CRITICAL -> 40
        }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFF1F8E9), Color(0xFFE8F5E9), Color(0xFFFFFFFF)),
                    ),
                ).padding(horizontal = AppSpacing.screenPad),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
        contentPadding =
            androidx.compose.foundation.layout
                .PaddingValues(top = AppSpacing.xxl, bottom = 100.dp),
    ) {
        // 1. 标题
        item {
            Text(
                text = "健康报告",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = SimpleDateFormat("yyyy年M月d日", Locale.CHINA).format(Date()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 2. 健康评分进度环
        item {
            GlassCard {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "健康评分",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    ProgressRing(
                        progress = healthScore / 100f,
                        size = 160.dp,
                        strokeWidth = 12.dp,
                        gradientColors =
                            when (assessment.level) {
                                RiskLevel.NORMAL -> HealthGradientSteps
                                RiskLevel.WATCH -> listOf(Color(0xFF00ACC1), Color(0xFF4DD0E1))
                                RiskLevel.WARNING -> listOf(Color(0xFFFF8F00), Color(0xFFFFAB40))
                                RiskLevel.CRITICAL -> listOf(Color(0xFFE53935), Color(0xFFEF5350))
                            },
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = healthScore.toString(),
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold,
                                color = riskColor(assessment.level),
                            )
                            Text(
                                text = "/ 100",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        text = assessment.level.label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = riskColor(assessment.level),
                    )
                    Text(
                        text = assessment.alerts.firstOrNull()?.problem ?: "整体健康状况良好，继续保持",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }

        // 3. 各项指标概览
        item {
            Text(
                text = "指标概览",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))

            val metrics =
                listOf(
                    Triple(
                        Icons.Default.Favorite,
                        DataHeart,
                        Triple(
                            latest[VitalType.HEART_RATE.id]?.value?.toInt()?.toString() ?: "--",
                            "次/分",
                            "心率",
                        ),
                    ),
                    Triple(
                        Icons.Default.MonitorHeart,
                        DataPressure,
                        Triple(
                            latest[VitalType.SYSTOLIC.id]?.value?.toInt()?.toString() ?: "--",
                            "mmHg",
                            "血压",
                        ),
                    ),
                    Triple(
                        Icons.Default.HealthAndSafety,
                        DataOxygen,
                        Triple(
                            latest[VitalType.SPO2.id]?.value?.toInt()?.toString() ?: "--",
                            "%",
                            "血氧",
                        ),
                    ),
                    Triple(
                        Icons.Default.TrendingUp,
                        DataSteps,
                        Triple(
                            latest[VitalType.STEPS.id]?.value?.toInt()?.toString() ?: "0",
                            "步",
                            "步数",
                        ),
                    ),
                )

            metrics.forEach { (icon, color, data) ->
                MetricRow(
                    icon = icon,
                    color = color,
                    value = data.first,
                    unit = data.second,
                    label = data.third,
                    status = "正常",
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))
            }
        }

        // 4. 导出报告
        item {
            GradientBanner(
                title = "导出健康报告",
                subtitle = "导出 PDF / JSON 格式，方便就医时提供给医生",
                gradientColors = CardGradientGreen,
                icon = Icons.Default.Download,
            )
        }

        // 5. AI 健康分析入口
        item {
            GradientBanner(
                title = "AI 健康分析",
                subtitle = "智能分析健康趋势，提供个性化建议",
                gradientColors = CardGradientOrange,
                icon = Icons.Default.TrendingUp,
            )
        }
    }
}

/**
 * 指标行组件。
 */
@Composable
private fun MetricRow(
    icon: ImageVector,
    color: Color,
    value: String,
    unit: String,
    label: String,
    status: String,
) {
    GlassCard {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandGreen,
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp, bottom = 4.dp),
                )
            }
        }
    }
}
