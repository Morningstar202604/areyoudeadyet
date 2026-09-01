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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sos
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silema.app.data.RiskLevel
import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalType
import com.silema.app.engine.RiskEngine
import com.silema.app.ui.components.DataTile
import com.silema.app.ui.components.GlassCard
import com.silema.app.ui.components.GradientBanner
import com.silema.app.ui.components.QuickActionButton
import com.silema.app.ui.theme.AppShapes
import com.silema.app.ui.theme.AppSpacing
import com.silema.app.ui.theme.BrandBlue
import com.silema.app.ui.theme.BrandGreen
import com.silema.app.ui.theme.BrandPurple
import com.silema.app.ui.theme.BrandWarm
import com.silema.app.ui.theme.CardGradientGreen
import com.silema.app.ui.theme.CardGradientOrange
import com.silema.app.ui.theme.CardGradientRed
import com.silema.app.ui.theme.DataHeart
import com.silema.app.ui.theme.DataOxygen
import com.silema.app.ui.theme.DataPressure
import com.silema.app.ui.theme.DataSleep
import com.silema.app.ui.theme.DataSteps
import com.silema.app.ui.theme.SosGradient
import com.silema.app.ui.theme.riskColor
import com.silema.app.util.TtsController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dashboard 首页 v3 — 现代健康活力风（参考 Keep 设计语言）。
 *
 * 布局结构：
 * 1. 顶部问候 + 日期
 * 2. 风险评估渐变横幅（进度环 + 状态）
 * 3. 核心数据网格（2x3：心率/血压/血氧/体温/步数/睡眠）
 * 4. 快速操作区（测量/设备/运动/家人/报告/SOS）
 * 5. 最近记录列表
 */
@Composable
fun DashboardScreenV3(
    records: List<VitalRecord>,
    tts: TtsController,
    onGoSos: () -> Unit,
    onGoEntry: () -> Unit,
    onGoDevices: () -> Unit,
    onGoWorkout: () -> Unit,
    onGoGuardian: () -> Unit,
    onGoFamily: () -> Unit = {},
    onGoAi: () -> Unit = {},
    onGoMedical: () -> Unit = {},
) {
    val assessment = remember(records) { RiskEngine.evaluate(records) }
    val latest =
        remember(records) {
            records.groupBy { it.typeId }.mapValues { (_, list) -> list.maxByOrNull { it.timestampMillis } }
        }

    val dateText =
        remember {
            SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(Date())
        }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFF1F8E9),
                            Color(0xFFE8F5E9),
                            Color(0xFFFFFFFF),
                        ),
                    ),
                ).padding(horizontal = AppSpacing.screenPad),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                top = AppSpacing.xxl,
                bottom = 100.dp,
            ),
    ) {
        // 1. 顶部问候 + 日期
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "早上好",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // 头像占位
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(CardGradientGreen)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        // 2. 风险评估渐变横幅 + 进度环
        item {
            val riskGradient =
                when (assessment.level) {
                    RiskLevel.NORMAL -> CardGradientGreen
                    RiskLevel.WATCH -> listOf(Color(0xFF00ACC1), Color(0xFF4DD0E1))
                    RiskLevel.WARNING -> CardGradientOrange
                    RiskLevel.CRITICAL -> CardGradientRed
                }

            GradientBanner(
                title = "健康状态：${assessment.level.label}",
                subtitle = assessment.alerts.firstOrNull()?.problem ?: "各项指标正常，继续保持",
                gradientColors = riskGradient,
                icon = Icons.Default.MonitorHeart,
            )
        }

        // 3. 核心数据网格（2x3）
        item {
            Text(
                text = "核心数据",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))

            val dataTiles =
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
                        Icons.Default.DirectionsWalk,
                        DataSteps,
                        Triple(
                            latest[VitalType.STEPS.id]?.value?.toInt()?.toString() ?: "0",
                            "步",
                            "步数",
                        ),
                    ),
                    Triple(
                        Icons.Default.NightsStay,
                        DataSleep,
                        Triple(
                            latest[VitalType.SLEEP.id]?.value?.toInt()?.toString() ?: "--",
                            "小时",
                            "睡眠",
                        ),
                    ),
                    Triple(
                        Icons.Default.LocalFireDepartment,
                        BrandWarm,
                        Triple(
                            latest[VitalType.TEMPERATURE.id]?.value?.toString() ?: "--",
                            "℃",
                            "体温",
                        ),
                    ),
                )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(420.dp),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                items(dataTiles) { (icon, color, data) ->
                    DataTile(
                        icon = icon,
                        iconTint = color,
                        value = data.first,
                        unit = data.second,
                        label = data.third,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // 4. 快速操作区
        item {
            Text(
                text = "快速操作",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))

            GlassCard {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(AppSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        QuickActionButton(
                            label = "测心率",
                            icon = Icons.Default.Favorite,
                            color = DataHeart,
                            onClick = onGoEntry,
                        )
                        QuickActionButton(
                            label = "设备",
                            icon = Icons.Default.MonitorHeart,
                            color = BrandBlue,
                            onClick = onGoDevices,
                        )
                        QuickActionButton(
                            label = "运动",
                            icon = Icons.Default.DirectionsWalk,
                            color = DataSteps,
                            onClick = onGoWorkout,
                        )
                        QuickActionButton(
                            label = "家人",
                            icon = Icons.Default.Phone,
                            color = BrandPurple,
                            onClick = onGoFamily,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        QuickActionButton(
                            label = "报告",
                            icon = Icons.Default.DateRange,
                            color = BrandGreen,
                            onClick = onGoMedical,
                        )
                        QuickActionButton(
                            label = "AI 分析",
                            icon = Icons.Default.TrendingUp,
                            color = BrandPurple,
                            onClick = onGoAi,
                        )
                        QuickActionButton(
                            label = "守护",
                            icon = Icons.Default.HealthAndSafety,
                            color = BrandBlue,
                            onClick = onGoGuardian,
                        )
                        QuickActionButton(
                            label = "SOS",
                            icon = Icons.Default.Sos,
                            color = Color(0xFFE53935),
                            onClick = onGoSos,
                        )
                    }
                }
            }
        }

        // 5. SOS 大按钮
        item {
            GradientBanner(
                title = "紧急呼救",
                subtitle = "一键呼叫紧急联系人，同时发送位置信息",
                gradientColors = SosGradient,
                icon = Icons.Default.Sos,
                onClick = onGoSos,
            )
        }

        // 6. 最近记录
        if (records.isNotEmpty()) {
            item {
                Text(
                    text = "最近记录",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))

                val recentRecords =
                    records
                        .sortedByDescending { it.timestampMillis }
                        .take(5)

                GlassCard {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(AppSpacing.md),
                    ) {
                        recentRecords.forEach { record ->
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = AppSpacing.sm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(36.dp)
                                                .clip(AppShapes.small)
                                                .background(riskColor(assessment.level).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = riskColor(assessment.level),
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = record.type?.displayName ?: record.typeId,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Text(
                                            text =
                                                SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
                                                    .format(Date(record.timestampMillis)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Text(
                                    text = "${record.value.toInt()} ${record.type?.unit ?: ""}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
