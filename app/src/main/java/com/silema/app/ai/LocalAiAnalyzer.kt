package com.silema.app.ai

import com.silema.app.data.Assessment
import com.silema.app.data.RiskLevel
import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalType
import com.silema.app.engine.RiskEngine

/**
 * 真实在端健康分析器：完全本地运行，基于 RiskEngine 规则引擎，
 * 不依赖任何云端大模型。把 RiskEngine 的 Assessment 转译为 AI 报告所需的 HealthInsight。
 *
 * 设计取舍：之前 AiAnalyzerProvider 永远返回 MockAiAnalyzer（假数据）。
 * 这里用已有的真实规则引擎做推理，使 AI 健康分析在本地构建下即为真实能力。
 */
class LocalAiAnalyzer : AiAnalyzer {
    override suspend fun analyze(
        records: List<VitalRecord>,
        context: AnalysisContext,
    ): Result<HealthInsight> {
        if (records.isEmpty()) {
            return Result.success(
                HealthInsight(
                    summary = "暂无体征数据，无法分析。请先在「录入」页添加血压、心率、血氧等记录。",
                    riskScore = 0f,
                    findings = emptyList(),
                    recommendations = listOf("每天固定时间测量核心指标", "保持规律作息与均衡饮食", "如有不适请及时就医"),
                    generatedAt = System.currentTimeMillis(),
                ),
            )
        }
        val assessment = RiskEngine.evaluate(records)
        return Result.success(
            HealthInsight(
                summary = buildSummary(assessment, records.size),
                riskScore = levelToScore(assessment.level),
                findings = buildFindings(records, assessment),
                recommendations = buildRecommendations(assessment),
                generatedAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun explainRisk(records: List<VitalRecord>): Result<String> {
        val assessment = RiskEngine.evaluate(records)
        return Result.success(
            if (assessment.alerts.isEmpty()) {
                "当前各项指标均在安全范围内。分析基于规则引擎与统计模型（个人基线偏差、趋势回归、组合规则），所有计算均在设备本地完成，数据不上传云端。"
            } else {
                "检测到 ${assessment.alerts.size} 项需要关注的信号，最高级别为「${assessment.level.label}」。${
                    assessment.alerts.first().why
                } 数据仅供健康参考，不构成医疗诊断；紧急情况请拨打 120。"
            },
        )
    }

    override suspend fun suggestRecommendations(records: List<VitalRecord>): Result<List<String>> =
        Result.success(buildRecommendations(RiskEngine.evaluate(records)))

    override suspend fun isAvailable(): Boolean = true

    /** Assessment 级别映射到 0~1 风险评分，供 AI 报告环形仪表着色。 */
    private fun levelToScore(level: RiskLevel): Float =
        when (level) {
            RiskLevel.NORMAL -> 0.1f
            RiskLevel.WATCH -> 0.35f
            RiskLevel.WARNING -> 0.6f
            RiskLevel.CRITICAL -> 0.85f
        }

    private fun buildSummary(
        assessment: Assessment,
        total: Int,
    ): String {
        val base =
            when (assessment.level) {
                RiskLevel.NORMAL -> "整体健康状况良好，各项指标均在安全范围。"
                RiskLevel.WATCH -> "部分指标处于注意区间，建议持续关注。"
                RiskLevel.WARNING -> "存在需要重视的健康信号，建议尽快处理。"
                RiskLevel.CRITICAL -> "检测到高危信号，请立即按建议采取行动或拨打 120。"
            }
        val extra =
            if (assessment.alerts.isNotEmpty()) {
                "本次分析覆盖 $total 条记录，识别出 ${assessment.alerts.size} 项关注点。"
            } else {
                "本次分析覆盖 $total 条记录。"
            }
        return base + extra
    }

    /**
     * 从告警生成发现；同时为核心指标（心率/血氧/血压/体温）补「正常」发现，
     * 让报告完整——有告警的指标不再重复展示正常卡片（避免自相矛盾）。
     */
    private fun buildFindings(
        records: List<VitalRecord>,
        assessment: Assessment,
    ): List<Finding> {
        val out = mutableListOf<Finding>()
        for (alert in assessment.alerts) {
            out +=
                Finding(
                    category = alert.metric,
                    status = alert.level.label,
                    detail = "${alert.measured}：${alert.problem}",
                    severity =
                        when (alert.level) {
                            RiskLevel.CRITICAL -> "high"
                            RiskLevel.WARNING -> "medium"
                            else -> "low"
                        },
                )
        }
        val coreTypes =
            listOf(
                VitalType.HEART_RATE,
                VitalType.SPO2,
                VitalType.SYSTOLIC,
                VitalType.DIASTOLIC,
                VitalType.TEMPERATURE,
            )
        for (type in coreTypes) {
            val latest = records.filter { it.typeId == type.id }.maxByOrNull { it.timestampMillis } ?: continue
            val base = baseName(type)
            val covered =
                assessment.alerts.any {
                    it.metric.contains(base) || (base == "血压" && it.metric.contains("压"))
                }
            if (covered) continue
            out +=
                Finding(
                    category = type.displayName.substringBefore("("),
                    status = "正常",
                    detail = "最近测量 ${formatValue(type, latest.value)}，处于安全范围",
                    severity = "low",
                )
        }
        return out
    }

    /** 从告警处置建议去重提取，最多 4 条；无告警时给通用健康建议。 */
    private fun buildRecommendations(assessment: Assessment): List<String> {
        if (assessment.alerts.isEmpty()) {
            return listOf(
                "保持每天固定时间测量核心指标",
                "维持规律作息与均衡饮食",
                "适度运动、保持心情平稳",
                "定期整理数据，复诊时供医生参考",
            )
        }
        val seen = LinkedHashSet<String>()
        for (alert in assessment.alerts.sortedByDescending { it.level.rank }) {
            seen.add(alert.action)
            if (seen.size >= 4) break
        }
        return seen.toList()
    }

    private fun baseName(type: VitalType): String =
        when (type) {
            VitalType.SYSTOLIC, VitalType.DIASTOLIC -> "血压"
            VitalType.HEART_RATE -> "心率"
            VitalType.SPO2 -> "血氧"
            VitalType.TEMPERATURE -> "体温"
            else -> type.displayName.substringBefore("(")
        }

    private fun formatValue(
        type: VitalType,
        v: Double,
    ): String =
        if (type == VitalType.TEMPERATURE) {
            String.format("%.1f", v) + type.unit
        } else {
            v.toLong().toString() + type.unit
        }
}
