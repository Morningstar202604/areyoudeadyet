package com.silema.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalSource
import com.silema.app.data.VitalType
import com.silema.app.engine.RiskEngine
import com.silema.app.store.AppRepository
import com.silema.app.ui.components.SectionTitle

private val ENTRY_TYPES = listOf(
    VitalType.HEART_RATE,
    VitalType.SPO2,
    VitalType.TEMPERATURE,
    VitalType.SYSTOLIC,
    VitalType.DIASTOLIC,
    VitalType.STEPS
)

/** 录入合法范围：超出范围直接拒绝保存，防止手滑录错导致误报警或漏报。 */
private fun validRange(type: VitalType): ClosedFloatingPointRange<Double> = when (type) {
    VitalType.HEART_RATE -> 20.0..260.0
    VitalType.SYSTOLIC -> 40.0..300.0
    VitalType.DIASTOLIC -> 20.0..200.0
    VitalType.SPO2 -> 30.0..100.0
    VitalType.TEMPERATURE -> 30.0..45.0
    VitalType.STEPS -> 0.0..200000.0
}

private fun stepOf(type: VitalType): Double = when (type) {
    VitalType.STEPS -> 500.0
    else -> 1.0
}

@Composable
fun EntryScreen(records: List<VitalRecord>, onDone: () -> Unit = {}) {
    var selected by remember { mutableStateOf(VitalType.HEART_RATE) }
    var primaryText by remember { mutableStateOf("") }
    var secondaryText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var savedTick by remember { mutableStateOf(0) }

    // 保存成功后稍作停留，让用户看到确认信息，再回到首页看风险评估结果
    LaunchedEffect(savedTick) {
        if (savedTick > 0) {
            kotlinx.coroutines.delay(900)
            onDone()
        }
    }

    val showSecondary = selected == VitalType.SYSTOLIC

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("录入体征", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        // 类型选择：两行大按钮，选中即高亮
        ENTRY_TYPES.chunked(3).forEach { rowTypes ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowTypes.forEach { t ->
                    TypeChip(
                        label = shortName(t),
                        selected = t == selected,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selected = t
                            message = null
                        }
                    )
                }
                repeat(3 - rowTypes.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = if (showSecondary)
                "填写血压：高压（收缩压）和低压（舒张压）"
            else "填写${selected.displayName}（单位：${selected.unit}）",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(10.dp))

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
            Spacer(Modifier.height(10.dp))
            NumberField(
                label = "低压·舒张压",
                value = secondaryText,
                onValue = { secondaryText = it; message = null },
                onStep = { delta -> secondaryText = applyStep(secondaryText, delta * stepOf(selected), selected) }
            )
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val p = primaryText.toDoubleOrNull()
                if (p == null || p !in validRange(selected)) {
                    message = "数值不合法：请检查是否填对（${shortName(selected)} 合理范围 ${validRange(selected)}）"
                    return@Button
                }
                val now = System.currentTimeMillis()
                AppRepository.addRecord(VitalRecord.of(selected, p, now, VitalSource.MANUAL))
                if (showSecondary) {
                    val d = secondaryText.toDoubleOrNull()
                    if (d == null || d !in validRange(VitalType.DIASTOLIC)) {
                        message = "低压不合法，高压已保存；请补填低压（合理范围 ${validRange(VitalType.DIASTOLIC)}）"
                        return@Button
                    }
                    AppRepository.addRecord(VitalRecord.of(VitalType.DIASTOLIC, d, now, VitalSource.MANUAL))
                }
                primaryText = ""
                secondaryText = ""
                message = "已保存 ✓ 马上回到首页看风险评估…"
                savedTick++
            },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().height(72.dp)
        ) {
            Text("保存", style = MaterialTheme.typography.titleMedium)
        }
        message?.let {
            Spacer(Modifier.height(10.dp))
            EmptyHint(text = it)
        }

        SectionTitle("最近记录")
        val recent = records.sortedByDescending { it.timestampMillis }.take(12)
        if (recent.isEmpty()) {
            EmptyHint(text = "还没有任何记录。从上面的按钮选一个指标开始。")
        } else {
            recent.forEach { rec ->
                RecordRow(rec)
                Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
        Spacer(Modifier.width(8.dp))
        StepperButton(icon = Icons.Filled.KeyboardArrowDown, desc = "减少", onClick = { onStep(-1) })
        Spacer(Modifier.width(6.dp))
        StepperButton(icon = Icons.Filled.Add, desc = "增加", onClick = { onStep(1) })
    }
}

@Composable
private fun StepperButton(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondary
        ),
        modifier = Modifier.height(56.dp).width(56.dp)
    ) {
        Icon(icon, contentDescription = desc)
    }
}

private fun applyStep(current: String, delta: Double, type: VitalType): String {
    val base = current.toDoubleOrNull() ?: 0.0
    val next = (base + delta).coerceIn(validRange(type))
    return if (next == next.toLong().toDouble()) next.toLong().toString()
    else String.format("%.1f", next)
}

@Composable
private fun RecordRow(rec: VitalRecord) {
    val t = rec.type
    if (t == null) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "${t.displayName.substringBefore("(")}  ${formatValue(t, rec.value)} ${t.unit}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = RiskEngine.clockText(rec.timestampMillis) + when (rec.source) {
                    VitalSource.HEALTH_CONNECT -> " · 来自穿戴设备"
                    VitalSource.DEMO -> " · 演示"
                    else -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FilledIconButton(
            onClick = { AppRepository.removeRecord(rec.typeId, rec.timestampMillis) },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.height(48.dp).width(48.dp)
        ) {
            Icon(Icons.Filled.Delete, contentDescription = "删除这条记录")
        }
    }
}

private fun formatValue(t: VitalType, v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)

private fun shortName(type: VitalType): String = when (type) {
    VitalType.HEART_RATE -> "心率"
    VitalType.SYSTOLIC -> "血压·高压"
    VitalType.DIASTOLIC -> "血压·低压"
    VitalType.SPO2 -> "血氧"
    VitalType.TEMPERATURE -> "体温"
    VitalType.STEPS -> "步数"
}
