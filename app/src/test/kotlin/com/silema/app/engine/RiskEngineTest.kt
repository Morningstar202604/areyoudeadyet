package com.silema.app.engine

import com.silema.app.data.RiskLevel
import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalSource
import com.silema.app.data.VitalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * RiskEngine 风险引擎单元测试。
 *
 * 覆盖：正常/危象/低血氧/组合规则/空数据/连续超标升级/告警三段式。
 */
class RiskEngineTest {

    private lateinit var engine: RiskEngine
    private val now = System.currentTimeMillis()

    @Before
    fun setUp() {
        engine = RiskEngine
    }

    @Test
    fun `全部正常时返回 NORMAL 且无告警`() {
        val result = engine.evaluate(mkVitals(72, 120, 78, 98, 36.5), now)
        assertEquals(RiskLevel.NORMAL, result.level)
        assertTrue(result.alerts.isEmpty())
    }

    @Test
    fun `高血压危象时返回 CRITICAL`() {
        val result = engine.evaluate(mkVitals(80, 185, 115, 97, 36.5), now)
        assertEquals(RiskLevel.CRITICAL, result.level)
        assertFalse(result.alerts.isEmpty())
    }

    @Test
    fun `低血氧时返回 CRITICAL`() {
        val result = engine.evaluate(mkVitals(85, 125, 80, 88, 36.5), now)
        assertEquals(RiskLevel.CRITICAL, result.level)
    }

    @Test
    fun `低血压加心跳快触发组合规则休克告警`() {
        val result = engine.evaluate(mkVitals(108, 95, 62, 96, 36.5), now)
        assertEquals(RiskLevel.CRITICAL, result.level)
        val hasCombo = result.alerts.any { it.metric.contains("+") }
        assertTrue("应检测到组合规则告警", hasCombo)
    }

    @Test
    fun `空数据返回 WARNING 不可沉默`() {
        val result = engine.evaluate(emptyList(), now)
        assertEquals(RiskLevel.WARNING, result.level)
    }

    @Test
    fun `连续3次心率超标自动升级`() {
        val records = mutableListOf<VitalRecord>()
        for (i in 3 downTo 1) {
            records += VitalRecord(
                typeId = VitalType.HEART_RATE.id,
                value = 105.0,
                timestampMillis = now - i * 3600_000L,
                source = VitalSource.MANUAL
            )
        }
        // 补充其他正常体征
        records += mkVitals(70, 118, 76, 97, 36.4)
            .filterNot { it.typeId == VitalType.HEART_RATE.id }

        val result = engine.evaluate(records, now)
        val escalated = result.alerts.any { it.problem.contains("连续 3 次") }
        assertTrue("连续3次超标应触发升级告警", escalated)
    }

    @Test
    fun `每条告警必须包含是什么为什么做什么三段`() {
        val scenarios = listOf(
            mkVitals(160, 190, 120, 85, 40.1),
            mkVitals(80, 185, 115, 97, 36.5)
        )
        for (vitals in scenarios) {
            val result = engine.evaluate(vitals, now)
            for (alert in result.alerts) {
                assertTrue("problem 不应为空", alert.problem.isNotEmpty())
                assertTrue("why 不应为空", alert.why.isNotEmpty())
                assertTrue("action 不应为空", alert.action.isNotEmpty())
            }
        }
    }

    // ---------- 辅助 ----------

    private fun mkVitals(
        hr: Number, sys: Number, dia: Number, spo2: Number, temp: Number
    ): List<VitalRecord> = listOf(
        VitalRecord(VitalType.HEART_RATE.id, hr.toDouble(), now - 1000, VitalSource.MANUAL),
        VitalRecord(VitalType.SYSTOLIC.id, sys.toDouble(), now - 1000, VitalSource.MANUAL),
        VitalRecord(VitalType.DIASTOLIC.id, dia.toDouble(), now - 1000, VitalSource.MANUAL),
        VitalRecord(VitalType.SPO2.id, spo2.toDouble(), now - 1000, VitalSource.MANUAL),
        VitalRecord(VitalType.TEMPERATURE.id, temp.toDouble(), now - 1000, VitalSource.MANUAL)
    )
}
