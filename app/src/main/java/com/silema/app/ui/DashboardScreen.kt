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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalType
import com.silema.app.engine.RiskEngine
import com.silema.app.ui.components.BigButton
import com.silema.app.ui.components.GradientCard
import com.silema.app.ui.components.Sparkline
import com.silema.app.ui.components.SectionTitle
import com.silema.app.ui.components.StatTile
import com.silema.app.ui.components.StatusBanner
import com.silema.app.ui.components.VitalCard
import com.silema.app.ui.theme.BrandBlue
import com.silema.app.ui.theme.BrandGreen
import com.silema.app.ui.theme.BrandSoftRed
import com.silema.app.ui.theme.BrandWarm
import com.silema.app.ui.theme.CardGradientBlue
import com.silema.app.ui.theme.CardGradientGreen
import com.silema.app.ui.theme.CardGradientOrange
import com.silema.app.ui.theme.CardGradientRed
import com.silema.app.ui.theme.LevelCritical
import com.silema.app.ui.theme.LevelNormal
import com.silema.app.ui.theme.LevelWatch
import com.silema.app.ui.theme.riskColor
import com.silema.app.util.TtsController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    records: List<VitalRecord>,
    tts: TtsController,
    onGoSos: () -> Unit,
    onGoEntry: () -> Unit,
    onGoDevices: () -> Unit,
    onGoWorkout: () -> Unit,
    onGoGuardian: () -> Unit,
    onGoFamily: () -> Unit = {},
    onGoAi: () -> Unit = {},
    onGoMedical: () -> Unit = {}
) {
    val assessment = remember(records) { RiskEngine.evaluate(records) }
    val latest = remember(records) {
        records.groupBy { it.typeId }.mapValues { (_, v) -> v.maxByOrNull { it.timestampMillis } }
    }
    val today = remember(records) {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        records.filter { it.timestampMillis >= cal.timeInMillis }
    }

    val statusGradient = when (assessment.level) {
        com.silema.app.data.RiskLevel.CRITICAL -> CardGradientRed
        com.silema.app.data.RiskLevel.WARNING -> CardGradientOrange
        com.silema.app.data.RiskLevel.WATCH -> CardGradientOrange
        com.silema.app.data.RiskLevel.NORMAL -> CardGradientGreen
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── 问候语 + SOS ──
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                    val greeting = when {
                        hour < 6 -> "夜深了"
                        hour < 11 -> "早上好"
                        hour < 14 -> "中午好"
                        hour < 18 -> "下午好"
                        else -> "晚上好"
                    }
                    Text(
                        text = "$greeting 👋",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINESE).format(Date()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onGoSos,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(BrandSoftRed)
                ) {
                    Icon(Icons.Filled.Phone, contentDescription = "SOS", tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }
        }

        // ── 状态横幅 ──
        item {
            StatusBanner(
                level = assessment.level,
                headline = assessment.level.label,
                subline = if (assessment.alerts.isNotEmpty()) "有 ${assessment.alerts.size} 条预警需要关注"
                else "所有指标正常，继续保持",
                icon = when (assessment.level) {
                    com.silema.app.data.RiskLevel.NORMAL -> Icons.Filled.Favorite
                    com.silema.app.data.RiskLevel.WATCH -> Icons.Filled.Info
                    com.silema.app.data.RiskLevel.WARNING -> Icons.Filled.Warning
                    com.silema.app.data.RiskLevel.CRITICAL -> Icons.Filled.Warning
                },
                gradientColors = statusGradient
            )
        }

        // ── 体征卡片 2x2 ──
        val vitalCards = listOf(
            Triple("心率", VitalType.HEART_RATE, Icons.Filled.Favorite),
            Triple("血氧", VitalType.SPO2, Icons.Filled.Info),
            Triple("收缩压", VitalType.SYSTOLIC, Icons.Filled.Favorite),
            Triple("舒张压", VitalType.DIASTOLIC, Icons.Filled.Favorite)
        )
        items(vitalCards.chunked(2)) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (label, type, icon) ->
                    val record = latest[type.id]
                    val level = record?.let { RiskEngine.evaluate(listOf(it)).level }
                    VitalCard(
                        label = label,
                        valueText = record?.let { "%.0f".format(it.value) } ?: "--",
                        timeText = record?.let {
                            SimpleDateFormat("HH:mm", Locale.US).format(Date(it.timestampMillis))
                        } ?: "待测量",
                        noteText = null,
                        level = level,
                        icon = icon,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── 今日概览 ──
        item {
            SectionTitle("今日概览")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                val coreTypes = listOf(VitalType.HEART_RATE, VitalType.SPO2, VitalType.SYSTOLIC, VitalType.DIASTOLIC)
                val measured = today.count { it.typeId in coreTypes.map { t -> t.id } }
                StatTile(label = "已测量", value = "$measured", valueColor = BrandBlue, icon = Icons.Filled.DateRange, modifier = Modifier.weight(1f))
                StatTile(label = "预警数", value = "${assessment.alerts.size}", valueColor = if (assessment.alerts.isNotEmpty()) BrandSoftRed else BrandGreen, icon = Icons.Filled.Warning, modifier = Modifier.weight(1f))
                StatTile(label = "步数", value = latest[VitalType.STEPS.id]?.let { "${it.value.toInt()}" } ?: "0", valueColor = BrandWarm, icon = Icons.Filled.Home, modifier = Modifier.weight(1f))
            }
        }

        // ── 心率趋势迷你图 ──
        item {
            val hrRecords = remember(records) {
                records.filter { it.typeId == VitalType.HEART_RATE.id }
                    .sortedBy { it.timestampMillis }
                    .takeLast(30)
                    .map { it.value }
            }
            if (hrRecords.size >= 2) {
                GradientCard(gradientColors = CardGradientBlue) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("心率趋势", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Sparkline(values = hrRecords, color = Color.White, fillColor = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "最近 ${hrRecords.size} 次测量",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // ── 快捷操作 ──
        item {
            SectionTitle("快捷操作")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                QuickAction("录入", Icons.Filled.Add, BrandWarm, onGoEntry, Modifier.weight(1f))
                QuickAction("设备", Icons.Filled.Build, BrandBlue, onGoDevices, Modifier.weight(1f))
                QuickAction("家人", Icons.Filled.Phone, BrandGreen, onGoFamily, Modifier.weight(1f))
            }
        }

        // ── AI 分析入口 ──
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .then(Modifier.padding(0.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AI 健康分析", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("基于体征数据的智能分析与建议", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // ── 预警列表 ──
        if (assessment.alerts.isNotEmpty()) {
            item { SectionTitle("需要关注") }
            items(assessment.alerts) { alert ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = riskColor(alert.level).copy(alpha = 0.08f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(riskColor(alert.level))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(alert.metric, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(alert.level.label, style = MaterialTheme.typography.labelMedium, color = riskColor(alert.level))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("问题：${alert.problem}", style = MaterialTheme.typography.bodySmall)
                        Text("原因：${alert.why}", style = MaterialTheme.typography.bodySmall)
                        Text("建议：${alert.action}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // ── 医疗对接入口 ──
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("医疗对接", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("FHIR 标准导出、健康报告分享", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // ── 底部留白 ──
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}
