package com.silema.app.engine

import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalType
import com.silema.app.data.Workout
import kotlin.math.ln
import kotlin.math.roundToInt

/** 压力指数：由 PPG 实测的 HRV(RMSSD) 对数线性映射到 0-100。 */
object StressMath {
    /**
     * RMSSD 10ms → 100 分（高压力），100ms → 0 分（放松），对数线性插值。
     * 这是基于公开 HRV 研究的估算，不是医疗诊断。
     */
    fun fromRmssd(rmssdMs: Double): Int {
        val v = rmssdMs.coerceIn(5.0, 200.0)
        val t = (ln(v) - ln(10.0)) / (ln(100.0) - ln(10.0))
        return (100.0 * (1.0 - t)).roundToInt().coerceIn(0, 100)
    }

    fun label(score: Int): String = when {
        score < 10 -> "极放松"
        score < 30 -> "放松"
        score < 55 -> "平稳"
        score < 75 -> "偏高"
        else -> "高压"
    }
}

/** 周健康报告生成器 —— 纯函数，输入全部记录，输出可直接展示的结构。 */
object HealthReport {

    data class MetricWeek(
        val type: VitalType,
        val thisWeekAvg: Double,
        val lastWeekAvg: Double?,
        val deltaPct: Double?,
        val count: Int,
        val unit: String
    )

    data class Weekly(
        val metrics: List<MetricWeek>,
        val alertCount: Int,
        val workoutCount: Int,
        val workoutKm: Double,
        val sleepAvgHours: Double?,
        val stressAvg: Int?,
        val summary: List<String>
    )

    fun weekly(
        records: List<VitalRecord>,
        workouts: List<Workout>,
        nowMillis: Long = System.currentTimeMillis(),
        alertCount: Int = 0
    ): Weekly {
        val weekStart = nowMillis - 7L * 24 * 3600_000
        val prevStart = nowMillis - 14L * 24 * 3600_000

        fun avgOf(type: VitalType, from: Long, to: Long): Pair<Double, Int>? {
            val vals = records.filter { it.typeId == type.id && it.timestampMillis in from..to }
            if (vals.isEmpty()) return null
            return Stats.mean(vals.map { it.value }) to vals.size
        }

        val metrics = mutableListOf<MetricWeek>()
        // 半开区间划分，边界时刻只属于上周：本周 (weekStart, now]，上周 [prevStart, weekStart]
        for (t in listOf(VitalType.HEART_RATE, VitalType.SYSTOLIC, VitalType.DIASTOLIC,
                         VitalType.SPO2, VitalType.TEMPERATURE)) {
            val tw = avgOf(t, weekStart + 1, nowMillis) ?: continue
            val lw = avgOf(t, prevStart, weekStart)
            metrics += MetricWeek(
                type = t,
                thisWeekAvg = tw.first,
                lastWeekAvg = lw?.first,
                deltaPct = lw?.first?.takeIf { it > 0 }?.let { (tw.first - it) / it * 100 },
                count = tw.second,
                unit = t.unit
            )
        }
        // 步数/睡眠/压力按"日均值"口径
        for (t in listOf(VitalType.STEPS, VitalType.SLEEP, VitalType.STRESS)) {
            val tw = avgOf(t, weekStart + 1, nowMillis) ?: continue
            val lw = avgOf(t, prevStart, weekStart)
            metrics += MetricWeek(t, tw.first, lw?.first,
                lw?.first?.takeIf { it > 0 }?.let { (tw.first - it) / it * 100 },
                tw.second, t.unit)
        }

        val weekWorkouts = workouts.filter { it.startMillis in weekStart..nowMillis }
        val sleepAvg = avgOf(VitalType.SLEEP, weekStart, nowMillis)?.first
        val stressAvg = avgOf(VitalType.STRESS, weekStart, nowMillis)?.first?.roundToInt()

        val summary = buildSummary(metrics, sleepAvg, stressAvg, weekWorkouts)

        return Weekly(
            metrics = metrics,
            alertCount = alertCount,
            workoutCount = weekWorkouts.size,
            workoutKm = weekWorkouts.sumOf { it.distanceKm },
            sleepAvgHours = sleepAvg,
            stressAvg = stressAvg,
            summary = summary
        )
    }

    private fun buildSummary(
        metrics: List<MetricWeek>,
        sleepAvg: Double?,
        stressAvg: Int?,
        workouts: List<Workout>
    ): List<String> {
        val lines = mutableListOf<String>()
        metrics.firstOrNull { it.type == VitalType.HEART_RATE }?.let { m ->
            val dir = m.deltaPct?.let { d -> if (d > 2) "比上周高 ${fmt1(d)}%" else if (d < -2) "比上周低 ${fmt1(-d)}%" else "与上周基本持平" } ?: "（上周无数据可比）"
            lines += "静息心率均值 ${fmt1(m.thisWeekAvg)} 次/分，$dir。"
        }
        metrics.firstOrNull { it.type == VitalType.SYSTOLIC }?.let { m ->
            val flag = if (m.thisWeekAvg >= 135) "，已进入偏高区间，建议带记录就诊" else ""
            lines += "收缩压均值 ${m.thisWeekAvg.roundToInt()} mmHg$flag。"
        }
        sleepAvg?.let {
            val advice = if (it < 6.5) "，低于 7 小时推荐值，试着提前半小时入睡" else "，睡眠充足"
            lines += "平均睡眠 ${fmt1(it)} 小时$advice。"
        }
        stressAvg?.let { lines += "平均压力指数 $it 分（${StressMath.label(it)}），来自 PPG 实测 HRV 估算。" }
        if (workouts.isNotEmpty()) {
            lines += "本周运动 ${workouts.size} 次，共 ${fmt1(workouts.sumOf { it.distanceKm })} 公里，继续保持。"
        } else {
            lines += "本周还没有运动记录，从一次 15 分钟步行开始。"
        }
        return lines
    }

    private fun fmt1(v: Double): String = String.format("%.1f", v)
}
