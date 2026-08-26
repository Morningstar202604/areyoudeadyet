package com.silema.app.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalSource
import com.silema.app.data.VitalType
import com.silema.app.engine.RiskEngine
import com.silema.app.store.AppRepository
import com.silema.app.ui.components.*
import com.silema.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private val ENTRY_TYPES = listOf(
    VitalType.HEART_RATE,
    VitalType.SYSTOLIC,
    VitalType.DIASTOLIC,
    VitalType.SPO2,
    VitalType.TEMPERATURE,
    VitalType.STEPS
)

private fun validRange(type: VitalType): ClosedFloatingPointRange<Double> = when (type) {
    VitalType.HEART_RATE -> 20.0..260.0
    VitalType.SYSTOLIC -> 40.0..300.0
    VitalType.DIASTOLIC -> 20.0..200.0
    VitalType.SPO2 -> 30.0..100.0
    VitalType.TEMPERATURE -> 30.0..45.0
    VitalType.STEPS -> 0.0..200000.0
    VitalType.SLEEP -> 0.0..24.0
    VitalType.STRESS -> 0.0..100.0
}

private fun stepOf(type: VitalType): Double = when (type) {
    VitalType.STEPS -> 500.0
    else -> 1.0
}

private fun shortName(type: VitalType): String = when (type) {
    VitalType.HEART_RATE -> "心率"
    VitalType.SYSTOLIC -> "收缩压"
    VitalType.DIASTOLIC -> "舒张压"
    VitalType.SPO2 -> "血氧"
    VitalType.TEMPERATURE -> "体温"
    VitalType.STEPS -> "步数"
    VitalType.SLEEP -> "睡眠"
    VitalType.STRESS -> "压力"
}

private fun formatValue(t: VitalType, v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)

@Composable
fun EntryScreen(records: List<VitalRecord>, onDone: () -> Unit = {}) {
    var selected by remember { mutableStateOf(VitalType.HEART_RATE) }
    var primaryText by remember { mutableStateOf("") }
    var secondaryText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var savedTick by remember { mutableStateOf(0) }

    LaunchedEffect(savedTick) {
        if (savedTick > 0) {
            kotlinx.coroutines.delay(900)
            onDone()
        }
    }

    val showSecondary = selected == VitalType.SYSTOLIC

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "专业录入",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "选择指标，精确录入您的体征数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Type selector - 3x2 grid
        item {
            SectionTitle("选择指标")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ENTRY_TYPES.chunked(3).forEach { rowTypes ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowTypes.forEach { type ->
                            EntryTypeChip(
                                label = shortName(type),
                                selected = type == selected,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    selected = type
                                    message = null
                                }
                            )
                        }
                        repeat(3 - rowTypes.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        // Input card
        item {
            SectionTitle("输入数值")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (showSecondary)
                            "填写血压：高压（收缩压）和低压（舒张压）"
                        else "填写${selected.displayName}（单位：${selected.unit}）",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    NumberField(
                        label = if (showSecondary) "高压·收缩压" else selected.displayName,
                        value = primaryText,
                        onValue = { primaryText = it; message = null },
                        onStep = { delta ->
                            primaryText = applyStep(primaryText, delta * stepOf(selected), selected)
                            message = null
                        }
                    )
                    if (showSecondary) {
                        Spacer(modifier = Modifier.height(12.dp))
                        NumberField(
                            label = "低压·舒张压",
                            value = secondaryText,
                            onValue = { secondaryText = it; message = null },
                            onStep = { delta ->
                                secondaryText = applyStep(secondaryText, delta * stepOf(VitalType.DIASTOLIC), VitalType.DIASTOLIC)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    BigButton(
                        text = "保存记录",
                        container = MaterialTheme.colorScheme.primary,
                        icon = Icons.Filled.Check,
                        onClick = {
                            val p = primaryText.toDoubleOrNull()
                            if (p == null || p !in validRange(selected)) {
                                message = "数值不合法：请检查是否填对（${shortName(selected)} 合理范围 ${validRange(selected)}）"
                                return@BigButton
                            }
                            val now = System.currentTimeMillis()
                            AppRepository.addRecord(VitalRecord.of(selected, p, now, VitalSource.MANUAL))
                            if (showSecondary) {
                                val d = secondaryText.toDoubleOrNull()
                                if (d == null || d !in validRange(VitalType.DIASTOLIC)) {
                                    message = "低压不合法，高压已保存；请补填低压（合理范围 ${validRange(VitalType.DIASTOLIC)}）"
                                    return@BigButton
                                }
                                AppRepository.addRecord(VitalRecord.of(VitalType.DIASTOLIC, d, now, VitalSource.MANUAL))
                            }
                            primaryText = ""
                            secondaryText = ""
                            message = "已保存 ✓ 马上回到首页看风险评估…"
                            savedTick++
                        }
                    )

                    message?.let { msg ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (msg.startsWith("已")) LevelNormal.copy(alpha = 0.08f)
                                else LevelWarning.copy(alpha = 0.08f)
                            )
                        ) {
                            Text(
                                msg,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // Recent records
        item {
            SectionTitle("最近记录")
        }
        val recent = records.sortedByDescending { it.timestampMillis }.take(12)
        if (recent.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.History,
                    title = "暂无记录",
                    message = "从上面的按钮选一个指标开始录入"
                )
            }
        } else {
            items(recent) { rec ->
                RecordCard(rec)
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun EntryTypeChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bgColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        onClick = onClick
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    onStep: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value,
            onValueChange = { s -> onValue(s.filter { it.isDigit() || it == '.' }) },
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            FilledIconButton(
                onClick = { onStep(1) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "增加")
            }
            Spacer(modifier = Modifier.height(4.dp))
            FilledIconButton(
                onClick = { onStep(-1) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "减少")
            }
        }
    }
}

@Composable
private fun RecordCard(rec: VitalRecord) {
    val t = rec.type ?: return
    val level = RiskEngine.metricLevel(t, rec.value)
    val levelColor = riskColor(level)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
                    .background(levelColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    level.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = levelColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${shortName(t)}  ${formatValue(t, rec.value)} ${t.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    RiskEngine.clockText(rec.timestampMillis) + when (rec.source) {
                        VitalSource.HEALTH_CONNECT -> " · 穿戴设备"
                        VitalSource.DEMO -> " · 演示"
                        VitalSource.BLE -> " · 蓝牙"
                        VitalSource.PPG_CAMERA -> " · 摄像头"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { AppRepository.removeRecord(rec.typeId, rec.timestampMillis) }
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun applyStep(current: String, delta: Double, type: VitalType): String {
    val base = current.toDoubleOrNull() ?: 0.0
    val next = (base + delta).coerceIn(validRange(type))
    return if (next == next.toLong().toDouble()) next.toLong().toString()
    else String.format("%.1f", next)
}
