package com.silema.app.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalSource
import com.silema.app.data.VitalType
import com.silema.app.engine.HealthReport
import com.silema.app.engine.RiskEngine
import com.silema.app.engine.StressMath
import com.silema.app.store.AppRepository
import com.silema.app.ui.components.BigButton
import com.silema.app.ui.components.SectionTitle
import java.time.LocalDate
import java.time.LocalTime

private val REPORT_TABS = listOf("趋势", "周报", "睡眠")

private fun fmt(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)

@Composable
fun ReportScreen(records: List<VitalRecord>) {
    var tab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            REPORT_TABS.forEachIndexed { i, label ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
            }
        }
        when (tab) {
            0 -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                TrendScreen(records)
            }
            1 -> WeeklyReportPanel(records)
            else -> SleepLogPanel(records)
        }
    }
}

@Composable
private fun WeeklyReportPanel(records: List<VitalRecord>) {
    val context = LocalContext.current
    val workouts by AppRepository.workouts.collectAsState()
    val assessment = remember(records) { RiskEngine.evaluate(records) }
    val report = remember(records, workouts) {
        HealthReport.weekly(records, workouts, alertCount = assessment.alerts.size)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("本周健康报告", style = MaterialTheme.typography.headlineSmall)
        Text(
            "统计区间：最近 7 天，对比再前 7 天",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        SectionTitle("一句话总结")
        report.summary.forEach { line ->
            Text(
                text = "· $line",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        SectionTitle("指标对比（本周均值 vs 上周）")
        if (report.metrics.isEmpty()) {
            EmptyHint(text = "本周还没有数据。录入或同步后这里会生成完整对比。")
        } else {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("指标", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("本周", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("对比上周", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            report.metrics.forEach { m ->
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                ) {
                    Text(m.type.displayName.substringBefore("("), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${fmt(m.thisWeekAvg)}${m.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
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
                            m.deltaPct < -2 -> com.silema.app.ui.theme.LevelNormal
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }

        SectionTitle("本周概览")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBlock("${report.alertCount}", "活跃预警")
            StatBlock("${report.workoutCount}", "运动次数")
            StatBlock(String.format("%.1f", report.workoutKm), "运动公里")
            report.sleepAvgHours?.let { StatBlock(String.format("%.1f", it), "日均睡眠") }
            report.stressAvg?.let { StatBlock("$it", "压力指数") }
        }

        Spacer(Modifier.height(16.dp))
        BigButton(
            text = "分享这份报告给家人",
            container = MaterialTheme.colorScheme.secondary,
            onClick = {
                val text = buildString {
                    appendLine("【死了吗？周健康报告】")
                    report.summary.forEach { appendLine("· $it") }
                    appendLine("（来自 Are You Dead Yet App）")
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
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatBlock(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 10.dp)) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SleepLogPanel(records: List<VitalRecord>) {
    var bedText by remember { mutableStateOf("23:00") }
    var wakeText by remember { mutableStateOf("06:30") }
    var msg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("睡眠记录", style = MaterialTheme.typography.headlineSmall)
        Text(
            "手动记录昨晚的入睡与起床时间，自动计算时长并进入周报统计。（后续版本将支持穿戴设备自动写入）",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = bedText,
                onValueChange = { bedText = it },
                label = { Text("昨晚入睡 (HH:mm)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = wakeText,
                onValueChange = { wakeText = it },
                label = { Text("今早起床 (HH:mm)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        BigButton(
            text = "保存昨晚睡眠",
            container = MaterialTheme.colorScheme.secondary,
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
                    VitalRecord.of(VitalType.SLEEP, hours, wakeDt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(), VitalSource.MANUAL)
                )
                msg = "已保存：睡眠 ${"%.1f".format(hours)} 小时 ✓"
            }
        )
        msg?.let { Spacer(Modifier.height(8.dp)); EmptyHint(text = it) }

        SectionTitle("最近 14 条")
        val sleepRecords = records.filter { it.typeId == VitalType.SLEEP.id }
            .sortedByDescending { it.timestampMillis }
            .take(14)
        if (sleepRecords.isEmpty()) {
            EmptyHint(text = "还没有睡眠记录。")
        } else {
            sleepRecords.forEach { rec ->
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(RiskEngine.clockText(rec.timestampMillis) + " 醒来", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${String.format("%.1f", rec.value)} 小时", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun parseHm(s: String): LocalTime? = runCatching {
    val parts = s.trim().split(":")
    LocalTime.of(parts[0].toInt(), parts[1].toInt())
}.getOrNull()
