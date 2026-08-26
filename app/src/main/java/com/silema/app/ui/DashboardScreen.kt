package com.silema.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.silema.app.data.AlertItem
import com.silema.app.data.Assessment
import com.silema.app.data.RiskLevel
import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalType
import com.silema.app.engine.RiskEngine
import com.silema.app.store.AppRepository
import com.silema.app.store.DemoData
import com.silema.app.ui.components.BigButton
import com.silema.app.ui.components.SectionTitle
import com.silema.app.ui.components.StatTile
import com.silema.app.ui.components.StatusBanner
import com.silema.app.ui.components.VitalCard
import com.silema.app.ui.theme.LevelNormal
import com.silema.app.ui.theme.riskColor
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

private fun bannerHeadline(level: RiskLevel): String = when (level) {
    RiskLevel.NORMAL -> "活着，一切正常"
    RiskLevel.WATCH -> "活着，但有地方要留心"
    RiskLevel.WARNING -> "有明确健康警告，别拖"
    RiskLevel.CRITICAL -> "危险！必须马上处理"
}

private fun bannerIcon(level: RiskLevel): ImageVector = when (level) {
    RiskLevel.NORMAL -> Icons.Filled.CheckCircle
    RiskLevel.WATCH -> Icons.Filled.Info
    RiskLevel.WARNING -> Icons.Filled.Warning
    RiskLevel.CRITICAL -> Icons.Filled.Phone
}

@Composable
fun DashboardScreen(
    records: List<VitalRecord>,
    tts: com.silema.app.util.TtsController,
    onGoSos: () -> Unit,
    onGoEntry: () -> Unit,
    onGoDevices: () -> Unit,
    onGoWorkout: () -> Unit,
    onGoGuardian: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val assessment = remember(records) { RiskEngine.evaluate(records) }
    var introVisible by remember {
        mutableStateOf(records.isEmpty() && !AppRepository.introDismissed)
    }

    // 危险级别时自动语音播报一次（级别变化才触发）
    LaunchedEffect(assessment.level) {
        if (assessment.level == RiskLevel.CRITICAL) {
            val first = assessment.alerts.firstOrNull { it.level == RiskLevel.CRITICAL }
            if (first != null) {
                tts.speak("危险警报。${first.problem}。${first.action}")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // ---- 头部：标题独立成列，避免长文案并排溢出 ----
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(text = "死了吗？", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "健康监测 · 生命管理 · 今天是 ${todayText()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            androidx.compose.material3.IconButton(onClick = onGoGuardian) {
                androidx.compose.material3.Icon(
                    Icons.Filled.Settings,
                    contentDescription = "守护与设置"
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        if (introVisible && records.isEmpty()) {
            IntroCard(
                onDemo = {
                    scope.launch {
                        AppRepository.loadDemoData()
                        AppRepository.demoLoaded = true
                        AppRepository.introDismissed = true
                        introVisible = false
                    }
                },
                onStart = {
                    AppRepository.introDismissed = true
                    introVisible = false
                    onGoEntry()
                }
            )
            Spacer(Modifier.height(14.dp))
        }

        StatusBanner(
            level = assessment.level,
            headline = bannerHeadline(assessment.level),
            subline = assessment.alerts.firstOrNull()?.action
                ?: if (records.isEmpty()) "先测量一轮血压、心率、血氧，我才能告诉你身体有没有风险。"
                else "继续按时测量血压、心率、血氧，保持记录。",
            icon = bannerIcon(assessment.level)
        )

        if (assessment.level >= RiskLevel.WARNING) {
            Spacer(Modifier.height(10.dp))
            BigButton(
                text = "用语音朗读警告内容",
                container = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = {
                    val spoken = assessment.alerts.joinToString("\n") { "${it.problem}。${it.action}" }
                    tts.speak(spoken)
                }
            )
        }

        VitalGrid(records, assessment)
        TodayOverview(records, assessment)
        GoalRings(records)

        SectionTitle("真实数据检测")
        BigButton(
            text = "摄像头实测心率（30秒，无需外设）",
            container = MaterialTheme.colorScheme.secondary,
            onClick = onGoDevices
        )
        Spacer(Modifier.height(10.dp))
        BigButton(
            text = "连接蓝牙心率带 / 血压计 / 血氧仪",
            container = MaterialTheme.colorScheme.secondary,
            onClick = onGoDevices
        )
        Spacer(Modifier.height(10.dp))
        BigButton(
            text = "开始运动记录（步行 / 跑步 GPS）",
            container = MaterialTheme.colorScheme.secondary,
            onClick = onGoWorkout
        )

        SectionTitle("当前问题清单（${assessment.alerts.size}）")
        if (assessment.alerts.isEmpty()) {
            EmptyHint(text = "没有发现异常指标。保持每天早晚各测一次，别等不舒服才测。")
        } else {
            assessment.alerts.forEach { AlertCard(it) }
        }

        if (assessment.missingToday.isNotEmpty()) {
            SectionTitle("今日缺项")
            EmptyHint(text = "还没测：${assessment.missingToday.joinToString("、")}。数据越全，预警越准。")
        }

        Spacer(Modifier.height(16.dp))
        BigButton(text = "SOS 一键呼救", container = Color(0xFFB71C1C), onClick = onGoSos)
        Spacer(Modifier.height(10.dp))
        BigButton(
            text = "去录入今日数据",
            container = MaterialTheme.colorScheme.secondary,
            onClick = onGoEntry
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun IntroCard(onDemo: () -> Unit, onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Text("第一次使用？", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "这个应用不会说「多喝水早睡觉」这种废话。它只做一件事：用医学阈值盯住你的血压、心率、血氧、体温，有危险就大声警告，并告诉你现在该做什么。\n\n先加载 7 天演示数据看看效果，或者直接开始给自己测量：",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        BigButton(
            text = "加载 7 天演示数据（推荐）",
            container = MaterialTheme.colorScheme.primary,
            onClick = onDemo
        )
        Spacer(Modifier.height(8.dp))
        BigButton(
            text = "不用了，直接开始录入",
            container = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            onClick = onStart
        )
    }
}

private fun todayText(): String {
    val d = LocalDate.now()
    return "${d.year}年${d.monthValue}月${d.dayOfMonth}日"
}

@Composable
private fun VitalGrid(records: List<VitalRecord>, assessment: Assessment) {
    val now = assessment.evaluatedAtMillis

    data class Disp(val value: String, val time: String, val level: RiskLevel?)

    fun displayOf(type: VitalType): Disp? {
        val rec = records.filter { it.typeId == type.id }.maxByOrNull { it.timestampMillis } ?: return null
        val v = rec.value
        val valueText = when (type) {
            VitalType.TEMPERATURE -> String.format("%.1f ℃", v)
            else -> formatNum(v)
        }
        return Disp(valueText, RiskEngine.relativeTime(rec.timestampMillis, now) + "测的", RiskEngine.metricLevel(type, v))
    }

    fun notMeasured(type: VitalType) = Disp("—", "还没有数据", null)

    SectionTitle("最新体征")

    val hr = displayOf(VitalType.HEART_RATE) ?: notMeasured(VitalType.HEART_RATE)
    val sp = displayOf(VitalType.SPO2) ?: notMeasured(VitalType.SPO2)
    val sysRec = records.filter { it.typeId == VitalType.SYSTOLIC.id }.maxByOrNull { it.timestampMillis }
    val diaRec = records.filter { it.typeId == VitalType.DIASTOLIC.id }.maxByOrNull { it.timestampMillis }
    val temp = displayOf(VitalType.TEMPERATURE) ?: notMeasured(VitalType.TEMPERATURE)
    val steps = displayOf(VitalType.STEPS) ?: notMeasured(VitalType.STEPS)

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        VitalCard("心率", hr.value, hr.time, null, hr.level, Modifier.weight(1f))
        VitalCard("血氧", sp.value, sp.time, null, sp.level, Modifier.weight(1f))
    }
    Spacer(Modifier.height(12.dp))

    if (sysRec != null && diaRec != null) {
        val bpLevel = maxOf(
            RiskEngine.metricLevel(VitalType.SYSTOLIC, sysRec.value),
            RiskEngine.metricLevel(VitalType.DIASTOLIC, diaRec.value)
        )
        VitalCard(
            label = "血压",
            valueText = "${sysRec.value.toInt()} / ${diaRec.value.toInt()} mmHg",
            timeText = RiskEngine.relativeTime(maxOf(sysRec.timestampMillis, diaRec.timestampMillis), now) + "测的",
            noteText = null,
            level = bpLevel
        )
    } else {
        VitalCard("血压", "—", "今天还没测，血压是老人最需要盯住的指标", null, null)
    }
    Spacer(Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        VitalCard("体温", temp.value, temp.time, null, temp.level, Modifier.weight(1f))
        VitalCard("步数", steps.value, steps.time, null, steps.level, Modifier.weight(1f))
    }
}

/** 今日概览：已测几项、待测几项、连续记录天数。 */
@Composable
private fun TodayOverview(records: List<VitalRecord>, assessment: Assessment) {
    val zone = ZoneId.systemDefault()
    val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()

    val measuredTypes = records
        .filter { it.timestampMillis >= todayStart }
        .mapNotNull { it.type?.id }
        .toSet()
    val coreCount = listOf("heart_rate", "systolic", "spo2").count { it in measuredTypes } +
        (if ("diastolic" in measuredTypes || "systolic" in measuredTypes) 1 else 0)

    var streak = 0
    run {
        var day = LocalDate.now(zone)
        if (records.none { sameDay(it.timestampMillis, day, zone) }) day = day.minusDays(1)
        while (records.any { sameDay(it.timestampMillis, day, zone) }) {
            streak++
            day = day.minusDays(1)
        }
    }

    SectionTitle("今日概览")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("今天已测核心项", "$coreCount / 3", LevelNormal, Modifier.weight(1f))
        StatTile("待关注问题", "${assessment.alerts.size}", riskColor(assessment.level), Modifier.weight(1f))
        StatTile("连续记录", "$streak 天", LevelNormal, Modifier.weight(1f))
    }
}

private fun sameDay(millis: Long, day: LocalDate, zone: ZoneId): Boolean =
    Instant.ofEpochMilli(millis).atZone(zone).toLocalDate() == day

private fun formatNum(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)

/** 单条告警卡：是什么 / 为什么危险 / 现在做什么，三段式直说。 */
@Composable
private fun AlertCard(alert: AlertItem) {
    val accent = riskColor(alert.level)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(2.dp, accent, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${alert.metric}｜${alert.measured}",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = alert.level.label,
                style = MaterialTheme.typography.titleSmall,
                color = accent
            )
        }
        LabeledLine("是什么问题", alert.problem)
        LabeledLine("为什么危险", alert.why)
        LabeledLine("现在就做", alert.action, emphasized = true)
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun LabeledLine(label: String, body: String, emphasized: Boolean = false) {
    Text(
        text = "【$label】$body",
        style = if (emphasized) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
        color = if (emphasized) riskColor(RiskLevel.WARNING) else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 8.dp)
    )
}

/** 今日目标三环：步数 / 测量 / 睡眠（对标运动健康活动环）。 */
@Composable
private fun GoalRings(records: List<VitalRecord>) {
    val zone = ZoneId.systemDefault()
    val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()

    val steps = records
        .filter { it.typeId == VitalType.STEPS.id && it.timestampMillis >= todayStart }
        .maxOfOrNull { it.value } ?: 0.0
    val stepsPct = (steps / AppRepository.stepsGoal).coerceIn(0.0, 1.0)

    val measuredTypes = records
        .filter { it.timestampMillis >= todayStart }
        .mapNotNull { it.typeId }
        .toSet()
    val measured = listOf("heart_rate", "systolic", "spo2").count { it in measuredTypes } / 3.0

    val sleepLast = records.filter { it.typeId == VitalType.SLEEP.id }
        .maxByOrNull { it.timestampMillis }?.value ?: 0.0
    val sleepPct = (sleepLast / AppRepository.sleepGoalHours).coerceIn(0.0, 1.0)

    SectionTitle("今日目标")
    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
        GoalRing("步数", stepsPct, "${steps.toInt()}\n目标${AppRepository.stepsGoal}")
        GoalRing("测量", measured, "${(measured * 3).roundToInt()}/3\n核心项")
        GoalRing("睡眠", sleepPct, "${String.format("%.1f", sleepLast)}h\n目标${AppRepository.sleepGoalHours}h")
    }
}

@Composable
private fun GoalRing(label: String, pct: Double, centerText: String) {
    val progressColor = if (pct >= 1.0) androidx.compose.ui.graphics.Color(0xFF2E7D32)
    else androidx.compose.ui.graphics.Color(0xFFE65100)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(104.dp)) {
            val stroke = 14f
            drawArc(
                color = androidx.compose.ui.graphics.Color(0x22888888),
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = progressColor,
                startAngle = -90f, sweepAngle = (360 * pct).toFloat(), useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "$label $centerText",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp)
    )
}
