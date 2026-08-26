package com.silema.app.ai

import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalType

class MockAiAnalyzer : AiAnalyzer {
    override suspend fun analyze(records: List<VitalRecord>, context: AnalysisContext): Result<HealthInsight> {
        val findings = mutableListOf<Finding>()
        val hr = records.filter { it.typeId == VitalType.HEART_RATE.id }
        val spo2 = records.filter { it.typeId == VitalType.SPO2.id }
        
        hr.lastOrNull()?.let { r ->
            val status = when {
                r.value < 60 -> "偏低"
                r.value > 100 -> "偏高"
                else -> "正常"
            }
            findings.add(Finding("心率", status, "最近测量 ${r.value.toInt()} 次/分", 
                if (r.value in 60.0..100.0) "low" else "medium"))
        }
        
        spo2.lastOrNull()?.let { r ->
            val status = if (r.value < 95) "偏低" else "正常"
            findings.add(Finding("血氧", status, "最近测量 ${r.value.toInt()}%", 
                if (r.value < 95) "medium" else "low"))
        }
        
        val riskScore = if (findings.any { it.severity == "high" }) 0.8f
        else if (findings.any { it.severity == "medium" }) 0.4f else 0.15f
        
        return Result.success(HealthInsight(
            summary = "基于最近 ${records.size} 条记录的分析。${if (riskScore < 0.3f) "整体健康状况良好。" else "部分指标需要关注。"}",
            riskScore = riskScore,
            findings = findings,
            recommendations = listOf(
                "保持规律作息，每天测量体征",
                "注意饮食均衡，适量运动",
                "如有不适请及时就医"
            ),
            generatedAt = System.currentTimeMillis()
        ))
    }
    
    override suspend fun explainRisk(records: List<VitalRecord>): Result<String> {
        return Result.success("基于规则引擎和统计模型的综合评估。数据仅供健康参考，不构成医疗诊断。")
    }
    
    override suspend fun suggestRecommendations(records: List<VitalRecord>): Result<List<String>> {
        return Result.success(listOf("保持规律作息", "适量运动", "均衡饮食", "定期测量体征"))
    }
    
    override suspend fun isAvailable(): Boolean = true
}