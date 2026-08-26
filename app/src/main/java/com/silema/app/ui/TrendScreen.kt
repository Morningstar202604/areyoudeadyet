package com.silema.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.silema.app.data.RiskLevel
import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalSource
import com.silema.app.data.VitalType
import com.silema.app.engine.RiskEngine
import com.silema.app.store.AppRepository
import com.silema.app.ui.components.SectionTitle
import com.silema.app.ui.components.Sparkline
import com.silema.app.ui.theme.riskColor

@Composable
fun TrendScreen(records: List<VitalRecord>) {
    var selected by remember { mutableStateOf(VitalType.HEART_RATE) }

    val series = records
        .filter { it.typeId == selected.id }
        .sortedBy { it.timestampMillis }
        .takeLast(60)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("趋势与历史", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        CHART_TYPES.chunked(3).forEach { rowTypes ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowTypes.forEach { t ->
                    ChartChip(
                        label = chartName(t),
                        selected = t == selected,
                        modifier = Modifier.weight(1f),
                        onClick = { selected = t }
                    )
                }
                repeat(3 - rowTypes.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(8.dp))
        }

        SectionTitle(chartName(selected) + " 近期走势（最多 60 次）")
        if (series.isEmpty()) {
            Text(
                text = "该指标还没有数据。去「录入」页添加，或到「守护」页同步穿戴设备。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        } else {
            Sparkline(
                values = series.map { it.value },
                color = riskColor(RiskLevel.NORMAL),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            StatsRow(series)
        }

        // 近 7 天小结：只统计最近 7 天的数据，反映当前状态而不是全部历史
        val weekAgo = System.currentTimeMillis() - 7L * 24 * 3600 * 1000
        val weekly = records.filter { it.typeId == selected.id && it.timestampMillis >= weekAgo }
        SectionTitle("近 7 天小结")
        if (weekly.isEmpty()) {
            Text(
                text = "最近 7 天没有${chartName(selected)}记录。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            )
        } else {
            StatsRow(weekly)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "最近 7 天共 ${weekly.size} 次测量",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SectionTitle("历史记录（最近 30 条）")
        if (series.isEmpty()) {
            Text(
                text = "暂无记录",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            )
        } else {
            series.sortedByDescending { it.timestampMillis }.take(30).forEach { rec ->
                HistoryRow(rec)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatsRow(series: List<VitalRecord>) {
    val values = series.map { it.value }
    val latest = series.last()
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCell("最新", format(latest.value), Modifier.weight(1f))
        StatCell("最高", format(values.max()), Modifier.weight(1f))
        StatCell("最低", format(values.min()), Modifier.weight(1f))
        StatCell("平均", format(values.average()), Modifier.weight(1f))
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HistoryRow(rec: VitalRecord) {
    val t = rec.type ?: return
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
            text = "${format(rec.value)} ${t.unit}" +
                if (rec.source == VitalSource.HEALTH_CONNECT) " ·穿戴" else "",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ChartChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier.height(56.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

private val CHART_TYPES = listOf(
    VitalType.HEART_RATE,
    VitalType.SYSTOLIC,
    VitalType.DIASTOLIC,
    VitalType.SPO2,
    VitalType.TEMPERATURE,
    VitalType.STEPS,
    VitalType.SLEEP,
    VitalType.STRESS
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

private fun format(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)
