package com.silema.app.engine

import com.silema.app.ble.BleCodec
import com.silema.app.data.RiskLevel
import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalSource
import com.silema.app.data.VitalType
import com.silema.app.ppg.PpgAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * 统计、血流动力学、PPG、BLE 协议解析测试。
 */
class StatsAndMathTest {

    private val now = System.currentTimeMillis()

    // ---------- 1. 数学原语 ----------

    @Test
    fun `mean 计算正确`() {
        val result = Stats.mean(listOf(1.0, 2.0, 3.0, 4.0))
        assertEquals(2.5, result, 1e-9)
    }

    @Test
    fun `std 总体标准差计算正确`() {
        val data = listOf(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0)
        assertEquals(2.0, Stats.std(data), 1e-9)
    }

    @Test
    fun `最小二乘斜率精确线性`() {
        val x = listOf(0.0, 1.0, 2.0, 3.0)
        val y = listOf(1.0, 3.0, 5.0, 7.0)
        assertEquals(2.0, Stats.leastSquaresSlope(x, y), 1e-9)
    }

    // ---------- 2. 血流动力学公式 ----------

    @Test
    fun `MAP 120_80 约等于 93_33`() {
        assertEquals(93.3333, VitalsMath.meanArterialPressure(120, 80), 0.001)
    }

    @Test
    fun `休克指数计算正确`() {
        assertEquals(0.5, VitalsMath.shockIndex(75, 150), 1e-9)
    }

    @Test
    fun `SI 大于等于1触发危险级休克指数告警`() {
        val result = RiskEngine.evaluate(mkVitals(110, 100, 70, 97, 36.5), now)
        val siAlert = result.alerts.any {
            it.metric == "休克指数" && it.level == RiskLevel.CRITICAL
        }
        assertTrue("SI>=1.0 应触发 CRITICAL 休克指数告警", siAlert)
    }

    @Test
    fun `MAP 低于65触发平均动脉压告警`() {
        // 90/52 → MAP = 52 + (90-52)/3 = 64.67 < 65
        val result = RiskEngine.evaluate(mkVitals(60, 90, 52, 98, 36.5), now)
        val mapAlert = result.alerts.any { it.metric == "平均动脉压" }
        assertTrue("MAP<65 应触发平均动脉压告警", mapAlert)
    }

    // ---------- 3. 个人基线 z-score ----------

    @Test
    fun `个人基线偏差触发 z-score 告警`() {
        val records = mutableListOf<VitalRecord>()
        val priors = doubleArrayOf(116.0, 120.0, 122.0, 126.0, 130.0)
        for (i in priors.indices) {
            records += VitalRecord(
                VitalType.SYSTOLIC.id, priors[i],
                now - (i + 1) * 86_400_000L, VitalSource.MANUAL
            )
            records += VitalRecord(
                VitalType.DIASTOLIC.id, 78.0,
                now - (i + 1) * 86_400_000L, VitalSource.MANUAL
            )
        }
        records += VitalRecord(VitalType.SYSTOLIC.id, 140.0, now - 60_000, VitalSource.MANUAL)
        records += VitalRecord(VitalType.DIASTOLIC.id, 88.0, now - 60_000, VitalSource.MANUAL)

        val result = RiskEngine.evaluate(records, now)
        val baseAlert = result.alerts.any {
            it.metric.startsWith("基线偏差") && it.problem.contains("|z|")
        }
        assertTrue("应触发基线偏差 z-score 告警", baseAlert)
    }

    // ---------- 4. 趋势回归 ----------

    @Test
    fun `连续一周收缩压上升触发趋势警告`() {
        val records = mutableListOf<VitalRecord>()
        for (d in 6 downTo 0) {
            val v = 112 + (6 - d) * 3.0
            val ts = now - d * 86_400_000L
            records += VitalRecord(VitalType.SYSTOLIC.id, v, ts - 8 * 3600_000L, VitalSource.MANUAL)
            records += VitalRecord(VitalType.SYSTOLIC.id, v, ts - 20 * 3600_000L, VitalSource.MANUAL)
            records += VitalRecord(VitalType.DIASTOLIC.id, 76.0, ts - 8 * 3600_000L, VitalSource.MANUAL)
            records += VitalRecord(VitalType.HEART_RATE.id, 70.0, ts - 8 * 3600_000L, VitalSource.MANUAL)
            records += VitalRecord(VitalType.SPO2.id, 98.0, ts - 8 * 3600_000L, VitalSource.MANUAL)
            records += VitalRecord(VitalType.TEMPERATURE.id, 36.4, ts - 8 * 3600_000L, VitalSource.MANUAL)
        }

        val result = RiskEngine.evaluate(records, now)
        val trendAlert = result.alerts.any {
            it.metric.startsWith("趋势·血压") && it.level == RiskLevel.WARNING
        }
        assertTrue("连续上升应触发趋势 WARNING", trendAlert)
    }

    // ---------- 5. PPG 分析器 ----------

    @Test
    fun `合成60bpm脉搏波分析正确`() {
        val ppg = PpgAnalyzer()
        val fps = 30
        for (f in 0 until fps * 35) {
            val tMs = f * 1000L / fps
            val phase = (tMs % 1000) / 1000.0
            val pulse = 900 * Math.exp(-Math.pow((phase - 0.15) / 0.05, 2.0)) +
                300 * Math.exp(-Math.pow((phase - 0.40) / 0.08, 2.0))
            val noise = ((f % 7) - 3) * 2.5
            ppg.addSample(tMs, 5000 + pulse + noise)
        }
        val result = ppg.analyze()
        assertNotNull("PPG 分析不应返回 null", result)
        assertTrue("BPM 应接近 60", abs(result!!.bpm - 60.0) <= 4.0)
        assertTrue("心跳数应 >= 20", result.beatCount >= 20)
    }

    @Test
    fun `信号不足时返回 null 不编数字`() {
        val ppg = PpgAnalyzer()
        for (f in 0 until 30 * 5) {
            ppg.addSample(f * 33L, 5000.0)
        }
        assertNull("短信号应返回 null", ppg.analyze())
    }

    // ---------- 6. BLE 协议解析 ----------

    @Test
    fun `BLE 心率 u8 解析`() {
        val data = byteArrayOf(0x00, 72)
        assertEquals(72.0, BleCodec.parseHeartRate(data), 0.0)
    }

    @Test
    fun `BLE 心率 u16 解析`() {
        val data = byteArrayOf(0x01, 0xB4.toByte(), 0x00) // 180
        assertEquals(180.0, BleCodec.parseHeartRate(data), 0.0)
    }

    @Test
    fun `BLE SFLOAT 36_5 解析`() {
        val data = byteArrayOf(0x6D.toByte(), 0xF1.toByte())
        assertEquals(36.5, BleCodec.sfloat(data, 0), 0.0)
    }

    @Test
    fun `BLE SFLOAT 负数解析`() {
        val data = byteArrayOf(0xEF.toByte(), 0xFE.toByte()) // -27.3
        assertEquals(-27.3, BleCodec.sfloat(data, 0), 0.0)
    }

    @Test
    fun `BLE 血压解析`() {
        val data = byteArrayOf(0x00, 0x78, 0x00, 0x50, 0x00, 0x00, 0x00)
        val result = BleCodec.parseBloodPressure(data)
        assertNotNull(result)
        assertEquals(120.0, result!![0], 0.0)
        assertEquals(80.0, result[1], 0.0)
    }

    // ---------- 辅助 ----------

    private fun mkVitals(
        hr: Double, sys: Double, dia: Double, spo2: Double, temp: Double
    ): List<VitalRecord> = listOf(
        VitalRecord(VitalType.HEART_RATE.id, hr, now - 1000, VitalSource.MANUAL),
        VitalRecord(VitalType.SYSTOLIC.id, sys, now - 1000, VitalSource.MANUAL),
        VitalRecord(VitalType.DIASTOLIC.id, dia, now - 1000, VitalSource.MANUAL),
        VitalRecord(VitalType.SPO2.id, spo2, now - 1000, VitalSource.MANUAL),
        VitalRecord(VitalType.TEMPERATURE.id, temp, now - 2000, VitalSource.MANUAL)
    )
}
