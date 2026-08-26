package com.silema.app.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalSource
import com.silema.app.data.VitalType
import com.silema.app.engine.HealthReport
import com.silema.app.engine.RiskEngine
import com.silema.app.store.AppRepository
import com.silema.app.ui.components.*
import com.silema.app.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private val REPORT_TABS = listOf("趋势", "周报", "睡眠")
private val CHART_TYPES = listOf(
    VitalType.HEART_RATE, VitalType.SYSTOLIC, VitalType.DIASTOLIC,
    VitalType.SPO2, VitalType.TEMPERATURE, VitalType.STEPS,
    VitalType.SLEEP, VitalType.STRESS
)

private fun chartName(type: VitalType): String = when (type) {
    VitalType.HEART_RATE -> "心率"
    VitalType.SYSTOLIC -> "收缩压"
    VitalType.DIASTOLIC -> "舒张压"
    VitalType.SPO2 -> "血氧"
    VitalType.TEMPERATURE -> "体温"
    VitalType.STEPS -> "步数"
    VitalType.SLEEP -> "睡眠"
    VitalType.STRESS -> "压力"
}

private fun fmt(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)

@Composable
fun ReportScreen(records: List<VitalRecord>) {
    var tab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = tab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                if (tab < tabPositions.size) {
                    val position = tabPositions[tab]
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.offset(position.left).width(position.width),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        ) {
            REPORT_TABS.forEachIndexed { i, label ->
                Tab(
                    selected = tab == i,
                    onClick = { tab = i },
                    text = {
                        Text(
                            label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        when (tab) {
            0 -> TrendTab(records)
            1 -> WeeklyTab(records)
            2 -> SleepTab(records)
        }
    }
}

// ── Trend Tab ───────────────────────────────────────
@Composable
private fun TrendTab(records: List<VitalRecord>) {
    var selected by remember { mutableStateOf(VitalType.HEART_RATE) }

    val series = records
        .filter { it.typeId == selected.id }
        .sortedBy { it.timestampMillis }
        .takeLast(60)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "趋势分析",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Type selector chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CHART_TYPES.chunked(4).forEach { rowTypes ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rowTypes.forEach { type ->
                            TrendChip(
                                label = chartName(type),
                                selected = type == selected,
                                modifier = Modifier.weight(1f),
                                onClick = { selected = type }
                            )
                        }
                    }
                }
            }
        }

        // Sparkline chart
        item {
            SectionTitle("${chartName(selected)} 走势")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (series.size >= 2) {
                        Sparkline(
                            values = series.map { it.value },
                            color = riskColor(RiskEngine.metricLevel(selected, series.last().value)),
                            fillColor = riskColor(RiskEngine.metricLevel(selected, series.last().value)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "最近 ${series.size} 次测量",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        EmptyState(
                            icon = Icons.Filled.DateRange,
                            title = "数据不足",
                            message = "至少需要2次测量才能显示趋势图"
                        )
                    }
                }
            }
        }

        // Stats row
        if (series.isNotEmpty()) {
            item {
                SectionTitle("统计概览")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val values = series.map { it.value }
                    StatMiniTile(label = "最新", value = formatVal(values.last()), color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                    StatMiniTile(label = "最高", value = formatVal(values.max()), color = LevelCritical, modifier = Modifier.weight(1f))
                    StatMiniTile(label = "最低", value = formatVal(values.min()), color = LevelNormal, modifier = Modifier.weight(1f))
                    StatMiniTile(label = "平均", value = formatVal(values.average()), color = BrandBlue, modifier = Modifier.weight(1f))
                }
            }
        }

        // Weekly summary
        val weekAgo = System.currentTimeMillis() - 7L * 24 * 3600 * 1000
        val weekly = records.filter { it.typeId == selected.id && it.timestampMillis >= weekAgo }

        item {
            SectionTitle("近 7 天小结")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (weekly.isEmpty()) {
                        Text(
                            "最近 7 天没有${chartName(selected)}记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val weekValues = weekly.map { it.value }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatMiniTile(label = "次数", value = "${weekly.size}", color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                            StatMiniTile(label = "均值", value = formatVal(weekValues.average()), color = BrandBlue, modifier = Modifier.weight(1f))
                            StatMiniTile(label = "最高", value = formatVal(weekValues.max()), color = LevelCritical, modifier = Modifier.weight(1f))
                            StatMiniTile(label = "最低", value = formatVal(weekValues.min()), color = LevelNormal, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // History list
        item {
            SectionTitle("历史记录")
        }
        val history = series.sortedByDescending { it.timestampMillis }.take(20)
        if (history.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.History,
                    title = "暂无记录",
                    message = "去「录入」页添加数据"
                )
            }
        } else {
            items(history) { rec ->
                val t = rec.type ?: return@items
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                ) {
                    Text(
                        text = RiskEngine.clockText(rec.timestampMillis),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${formatVal(rec.value)} ${t.unit}" +
                            if (rec.source == VitalSource.HEALTH_CONNECT) " ·穿戴" else "",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// ── Weekly Tab ──────────────────────────────────────
@Composable
private fun WeeklyTab(records: List<VitalRecord>) {
    val context = LocalContext.current
    val workouts by AppRepository.workouts.collectAsState()
    val assessment = remember(records) { RiskEngine.evaluate(records) }
    val report = remember(records, workouts) {
        HealthReport.weekly(records, workouts, alertCount = assessment.alerts.size)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "周度报告",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "统计区间：最近 7 天，对比再前 7 天",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Summary
        item {
            SectionTitle("一句话总结")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    report.summary.forEach { line ->
                        Text(
                            text = "· $line",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Metrics comparison
        item { SectionTitle("指标对比（本周 vs 上周）") }
        if (report.metrics.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.DateRange,
                    title = "暂无数据",
                    message = "录入或同步数据后，这里会生成完整对比"
                )
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Header
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("指标", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("本周", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("对比", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(4.dp))

                        report.metrics.forEach { m ->
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Text(
                                    m.type.displayName.substringBefore("("),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "${fmt(m.thisWeekAvg)}${m.unit}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                                Text(
                                    when {
                                        m.deltaPct == null -> "—"
                                        kotlin.math.abs(m.deltaPct) < 2 -> "持平"
                                        else -> "${if (m.deltaPct > 0) "↑" else "↓"} ${fmt(kotlin.math.abs(m.deltaPct))}%"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = when {
                                        m.deltaPct == null -> MaterialTheme.colorScheme.onSurfaceVariant
                                        m.deltaPct > 2 -> MaterialTheme.colorScheme.error
                                        m.deltaPct < -2 -> LevelNormal
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.weight(1f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                            }
                        }
                    }
                }
            }
        }

        // Overview stats
        item {
            SectionTitle("本周概览")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatMiniTile(label = "预警", value = "${report.alertCount}", color = if (report.alertCount > 0) LevelCritical else LevelNormal, modifier = Modifier.weight(1f))
                StatMiniTile(label = "运动", value = "${report.workoutCount}次", color = BrandBlue, modifier = Modifier.weight(1f))
                StatMiniTile(label = "公里", value = String.format("%.1f", report.workoutKm), color = BrandGreen, modifier = Modifier.weight(1f))
                report.sleepAvgHours?.let {
                    StatMiniTile(label = "睡眠", value = String.format("%.1f", it), color = BrandPurple, modifier = Modifier.weight(1f))
                }
            }
        }

        // Share button
        item {
            BigButton(
                text = "分享这份报告给家人",
                container = MaterialTheme.colorScheme.secondary,
                icon = Icons.Filled.Share,
                onClick = {
                    val text = buildString {
                        appendLine("【周度健康报告】")
                        report.summary.forEach { appendLine("· $it") }
                        appendLine("（来自健康监测应用）")
                    }
                    runCatching {
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text),
                                "分享周报"
                            )
                        )
                    }
                }
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// ── Sleep Tab ───────────────────────────────────────
@Composable
private fun SleepTab(records: List<VitalRecord>) {
    var bedText by remember { mutableStateOf("23:00") }
    var wakeText by remember { mutableStateOf("06:30") }
    var msg by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "睡眠记录",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "手动记录昨晚的入睡与起床时间，自动计算时长",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Entry card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = bedText,
                            onValueChange = { bedText = it },
                            label = { Text("入睡 (HH:mm)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = wakeText,
                            onValueChange = { wakeText = it },
                            label = { Text("起床 (HH:mm)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    BigButton(
                        text = "保存昨晚睡眠",
                        container = BrandPurple,
                        icon = Icons.Filled.Save,
                        onClick = {
                            val bed = parseHm(bedText)
                            val wake = parseHm(wakeText)
                            if (bed == null || wake == null) {
                                msg = "时间格式应为 HH:mm，例如 23:00"
                                return@BigButton
                            }
                            var bedDt = LocalDate.now().minusDays(1).atTime(bed)
                            var wakeDt = LocalDate.now().atTime(wake)
                            if (!wakeDt.isAfter(bedDt)) bedDt = bedDt.minusDays(1)
                            val hours = java.time.Duration.between(bedDt, wakeDt).toMinutes() / 60.0
                            if (hours < 1 || hours > 16) {
                                msg = "计算出的睡眠时长 ${"%.1f".format(hours)} 小时不合理，请检查时间"
                                return@BigButton
                            }
                            AppRepository.addRecord(
                                VitalRecord.of(
                                    VitalType.SLEEP, hours,
                                    wakeDt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                                    VitalSource.MANUAL
                                )
                            )
                            msg = "已保存：睡眠 ${"%.1f".format(hours)} 小时 ✓"
                        }
                    )

                    msg?.let { m ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (m.startsWith("已")) LevelNormal.copy(alpha = 0.08f)
                                else LevelWarning.copy(alpha = 0.08f)
                            )
                        ) {
                            Text(m, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(14.dp))
                        }
                    }
                }
            }
        }

        // Sleep history
        item { SectionTitle("最近 14 条") }
        val sleepRecords = records.filter { it.typeId == VitalType.SLEEP.id }
            .sortedByDescending { it.timestampMillis }
            .take(14)

        if (sleepRecords.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.History,
                    title = "暂无睡眠记录",
                    message = "上面记录昨晚的入睡和起床时间"
                )
            }
        } else {
            items(sleepRecords) { rec ->
                val hours = rec.value
                val qualityColor = when {
                    hours >= 7.5 -> LevelNormal
                    hours >= 6.0 -> LevelWatch
                    else -> LevelWarning
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(qualityColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.DateRange,
                                contentDescription = null,
                                tint = qualityColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${fmt(hours)} 小时",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                RiskEngine.clockText(rec.timestampMillis),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            when {
                                hours >= 7.5 -> "充足"
                                hours >= 6.0 -> "偏少"
                                else -> "不足"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = qualityColor
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun TrendChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bgColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        onClick = onClick
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun StatMiniTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                color = color,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

private fun formatVal(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)

private fun parseHm(s: String): LocalTime? = runCatching {
    val parts = s.trim().split(":")
    LocalTime.of(parts[0].toInt(), parts[1].toInt())
}.getOrNull()
