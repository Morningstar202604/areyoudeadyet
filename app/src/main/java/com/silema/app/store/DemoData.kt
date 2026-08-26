package com.silema.app.store

import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalSource
import com.silema.app.data.VitalType
import java.time.LocalDate
import java.time.ZoneId

/**
 * 演示数据生成器：一键生成过去 7 天的完整体征历史，
 * 让新用户立刻看到评估、趋势、预警的全部效果。
 * 数值为固定的合理示例（含一次"注意"级波动），全部标记为 demo 来源，
 * 可在守护页一键清空。
 */
object DemoData {

    fun generate(nowMillis: Long = System.currentTimeMillis()): List<VitalRecord> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        fun at(daysAgo: Int, hour: Int, minute: Int): Long =
            today.minusDays(daysAgo.toLong())
                .atTime(hour, minute)
                .atZone(zone)
                .toInstant()
                .toEpochMilli()

        val out = mutableListOf<VitalRecord>()
        fun add(type: VitalType, value: Double, daysAgo: Int, hour: Int, minute: Int) {
            var ts = at(daysAgo, hour, minute)
            // 深夜加载时，"今天早上"的固定时刻可能在未来 —— 钳制到当前时间之前
            if (ts > nowMillis) ts = nowMillis - 60_000L
            out += VitalRecord.of(type, value, ts, VitalSource.DEMO)
        }

        // 第 7~5 天前：整体平稳的正常基线
        for (d in 6 downTo 4) {
            add(VitalType.HEART_RATE, 71.0, d, 8, 10)
            add(VitalType.SYSTOLIC, 126.0, d, 8, 10)
            add(VitalType.DIASTOLIC, 80.0, d, 8, 10)
            add(VitalType.SPO2, 97.0, d, 8, 10)
            add(VitalType.TEMPERATURE, 36.4, d, 8, 10)
            add(VitalType.STEPS, 2800.0 + d * 150, d, 20, 30)

            add(VitalType.HEART_RATE, 68.0, d, 20, 30)
            add(VitalType.SYSTOLIC, 122.0, d, 20, 30)
            add(VitalType.DIASTOLIC, 78.0, d, 20, 30)
            add(VitalType.SPO2, 98.0, d, 20, 30)
            add(VitalType.TEMPERATURE, 36.3, d, 20, 30)
        }

        // 第 3 天前：血压进入"注意"区间，晚上自行回落 —— 演示预警与复查价值
        add(VitalType.HEART_RATE, 74.0, 3, 8, 10)
        add(VitalType.SYSTOLIC, 143.0, 3, 8, 10)
        add(VitalType.DIASTOLIC, 91.0, 3, 8, 10)
        add(VitalType.SPO2, 96.0, 3, 8, 10)
        add(VitalType.TEMPERATURE, 36.5, 3, 8, 10)
        add(VitalType.STEPS, 3600.0, 3, 20, 30)
        add(VitalType.HEART_RATE, 72.0, 3, 20, 30)
        add(VitalType.SYSTOLIC, 131.0, 3, 20, 30)
        add(VitalType.DIASTOLIC, 85.0, 3, 20, 30)
        add(VitalType.SPO2, 97.0, 3, 20, 30)
        add(VitalType.TEMPERATURE, 36.4, 3, 20, 30)

        // 第 2 天前：傍晚心率偏快一次（注意级），夜间恢复
        add(VitalType.HEART_RATE, 70.0, 2, 8, 10)
        add(VitalType.SYSTOLIC, 127.0, 2, 8, 10)
        add(VitalType.DIASTOLIC, 81.0, 2, 8, 10)
        add(VitalType.SPO2, 97.0, 2, 8, 10)
        add(VitalType.TEMPERATURE, 36.4, 2, 8, 10)
        add(VitalType.STEPS, 4100.0, 2, 20, 30)
        add(VitalType.HEART_RATE, 104.0, 2, 18, 45)
        add(VitalType.SYSTOLIC, 129.0, 2, 19, 30)
        add(VitalType.DIASTOLIC, 82.0, 2, 19, 30)
        add(VitalType.SPO2, 96.0, 2, 19, 30)
        add(VitalType.TEMPERATURE, 36.5, 2, 19, 30)
        add(VitalType.HEART_RATE, 73.0, 2, 21, 30)
        add(VitalType.SYSTOLIC, 124.0, 2, 21, 30)
        add(VitalType.DIASTOLIC, 79.0, 2, 21, 30)
        add(VitalType.SPO2, 98.0, 2, 21, 30)
        add(VitalType.TEMPERATURE, 36.4, 2, 21, 30)

        // 昨天：完全正常
        add(VitalType.HEART_RATE, 69.0, 1, 8, 5)
        add(VitalType.SYSTOLIC, 125.0, 1, 8, 5)
        add(VitalType.DIASTOLIC, 79.0, 1, 8, 5)
        add(VitalType.SPO2, 98.0, 1, 8, 5)
        add(VitalType.TEMPERATURE, 36.3, 1, 8, 5)
        add(VitalType.STEPS, 5200.0, 1, 20, 0)
        add(VitalType.HEART_RATE, 67.0, 1, 20, 0)
        add(VitalType.SYSTOLIC, 121.0, 1, 20, 0)
        add(VitalType.DIASTOLIC, 77.0, 1, 20, 0)
        add(VitalType.SPO2, 98.0, 1, 20, 0)
        add(VitalType.TEMPERATURE, 36.4, 1, 20, 0)

        // 今天早上：已测一轮，数值正常 —— 用户可以接着录今晚的数据
        add(VitalType.HEART_RATE, 76.0, 0, 8, 15)
        add(VitalType.SYSTOLIC, 128.0, 0, 8, 15)
        add(VitalType.DIASTOLIC, 82.0, 0, 8, 15)
        add(VitalType.SPO2, 97.0, 0, 8, 15)
        add(VitalType.TEMPERATURE, 36.5, 0, 8, 15)
        add(VitalType.STEPS, 1860.0, 0, 12, 30)

        return out.sortedByDescending { it.timestampMillis }
    }
}
