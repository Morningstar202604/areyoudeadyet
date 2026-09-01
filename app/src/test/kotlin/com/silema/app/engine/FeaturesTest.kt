package com.silema.app.engine

import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalSource
import com.silema.app.data.VitalType
import com.silema.app.data.Workout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * 功能特性测试：压力指数、Haversine 距离、周报对比。
 */
class FeaturesTest {
    private val now = System.currentTimeMillis()

    // ---------- 1. 压力指数 ----------

    @Test
    fun `压力指数低端 RMSSD 10 应接近 100`() {
        val s = StressMath.fromRmssd(10.0)
        assertTrue("RMSSD=10 压力应 >= 95，实际 $s", s >= 95)
    }

    @Test
    fun `压力指数高端 RMSSD 100 应接近 0`() {
        val s = StressMath.fromRmssd(100.0)
        assertTrue("RMSSD=100 压力应 <= 5，实际 $s", s <= 5)
    }

    @Test
    fun `压力指数单调递减`() {
        val s10 = StressMath.fromRmssd(10.0)
        val s50 = StressMath.fromRmssd(50.0)
        val s100 = StressMath.fromRmssd(100.0)
        assertTrue("压力应随 RMSSD 增大而减小: $s10 > $s50 > $s100", s10 > s50 && s50 > s100)
    }

    @Test
    fun `压力指数钳位在 0_100`() {
        val s = StressMath.fromRmssd(9999.0)
        assertTrue("压力应在 [0,100]，实际 $s", s in 0..100)
    }

    // ---------- 2. Haversine 距离 ----------

    @Test
    fun `同点距离为 0`() {
        val d = Stats.haversineMeters(39.9087, 116.3975, 39.9087, 116.3975)
        assertEquals(0.0, d, 1e-6)
    }

    @Test
    fun `纬度差 0_01 度约 1112 米`() {
        val d = Stats.haversineMeters(39.90, 116.3975, 39.91, 116.3975)
        assertTrue("0.01° 纬度差应约 1111.9m，实际 $d", abs(d - 1111.9) < 5.0)
    }

    // ---------- 3. 周报对比 ----------

    @Test
    fun `周报心率本周与上周对比正确`() {
        val records = mutableListOf<VitalRecord>()
        // 上周：心率均值 80
        for (day in 8..14) {
            val ts = now - day * 86_400_000L
            records += VitalRecord(VitalType.HEART_RATE.id, 78.0, ts, VitalSource.MANUAL)
            records += VitalRecord(VitalType.HEART_RATE.id, 82.0, ts + 3600_000L, VitalSource.MANUAL)
        }
        // 本周：心率均值 70 + 睡眠 8h
        for (day in 1..7) {
            val ts = now - day * 86_400_000L
            records += VitalRecord(VitalType.HEART_RATE.id, 68.0, ts, VitalSource.MANUAL)
            records += VitalRecord(VitalType.HEART_RATE.id, 72.0, ts + 3600_000L, VitalSource.MANUAL)
            records += VitalRecord(VitalType.SLEEP.id, 8.0, ts, VitalSource.MANUAL)
        }

        val workouts =
            listOf(
                Workout("w1", "walk", now - 86_400_000L, 1_800_000L, 2000.0, 68.7, emptyList()),
            )

        val report = HealthReport.weekly(records, workouts, now, 2)
        val hr = report.metrics.firstOrNull { it.type == VitalType.HEART_RATE }

        assertNotNull("周报应包含心率指标", hr)
        assertEquals("本周心率均值应约 70.15", 70.1538, hr!!.thisWeekAvg!!, 0.01)
        assertEquals("上周心率均值应约 79.2", 79.2, hr.lastWeekAvg!!, 0.01)
        assertNotNull("变化百分比不应为 null", hr.deltaPct)
        assertTrue("心率应下降约 11.4%，实际 ${hr.deltaPct}", abs(hr.deltaPct!! + 11.4216) < 0.1)
    }

    @Test
    fun `周报睡眠均值正确`() {
        val records = mutableListOf<VitalRecord>()
        for (day in 1..7) {
            val ts = now - day * 86_400_000L
            records += VitalRecord(VitalType.SLEEP.id, 8.0, ts, VitalSource.MANUAL)
        }
        val report = HealthReport.weekly(records, emptyList(), now, 2)
        assertNotNull("睡眠均值不应为 null", report.sleepAvgHours)
        assertEquals(8.0, report.sleepAvgHours!!, 0.01)
    }

    @Test
    fun `周报运动统计正确`() {
        val workouts =
            listOf(
                Workout("w1", "walk", now - 86_400_000L, 1_800_000L, 2000.0, 68.7, emptyList()),
            )
        val report = HealthReport.weekly(emptyList(), workouts, now, 2)
        assertEquals(1, report.workoutCount)
        assertEquals(2.0, report.workoutKm, 0.01)
    }

    @Test
    fun `周报摘要包含关键指标`() {
        val records = mutableListOf<VitalRecord>()
        for (day in 1..7) {
            val ts = now - day * 86_400_000L
            records += VitalRecord(VitalType.HEART_RATE.id, 70.0, ts, VitalSource.MANUAL)
            records += VitalRecord(VitalType.SLEEP.id, 7.5, ts, VitalSource.MANUAL)
        }
        val report = HealthReport.weekly(records, emptyList(), now, 2)
        assertTrue("摘要应包含静息心率", report.summary.any { it.contains("静息心率") })
        assertTrue("摘要应包含睡眠", report.summary.any { it.contains("睡眠") })
    }
}
