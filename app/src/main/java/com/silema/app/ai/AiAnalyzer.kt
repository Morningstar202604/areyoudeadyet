package com.silema.app.ai

import com.silema.app.data.VitalRecord

interface AiAnalyzer {
    /** Generate a health analysis report from vital records */
    suspend fun analyze(
        records: List<VitalRecord>,
        context: AnalysisContext,
    ): Result<HealthInsight>

    /** Get natural language risk explanation */
    suspend fun explainRisk(records: List<VitalRecord>): Result<String>

    /** Suggest lifestyle recommendations based on trends */
    suspend fun suggestRecommendations(records: List<VitalRecord>): Result<List<String>>

    /** Check if AI service is available */
    suspend fun isAvailable(): Boolean
}

data class AnalysisContext(
    val patientAge: Int? = null,
    // "male" | "female"
    val patientGender: String? = null,
    val medicalHistory: List<String> = emptyList(),
    val medications: List<String> = emptyList(),
)

data class HealthInsight(
    val summary: String,
    // 0.0 (healthy) to 1.0 (critical)
    val riskScore: Float,
    val findings: List<Finding>,
    val recommendations: List<String>,
    val generatedAt: Long,
)

data class Finding(
    // "心率", "血压", etc.
    val category: String,
    // "正常", "偏高", "异常"
    val status: String,
    val detail: String,
    // "low" | "medium" | "high"
    val severity: String,
)
