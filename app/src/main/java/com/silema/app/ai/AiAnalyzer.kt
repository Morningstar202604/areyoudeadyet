package com.silema.app.ai

import com.silema.app.data.VitalRecord

interface AiAnalyzer {
    /** Generate a health analysis report from vital records */
    suspend fun analyze(records: List<VitalRecord>, context: AnalysisContext): Result<HealthInsight>
    
    /** Get natural language risk explanation */
    suspend fun explainRisk(records: List<VitalRecord>): Result<String>
    
    /** Suggest lifestyle recommendations based on trends */
    suspend fun suggestRecommendations(records: List<VitalRecord>): Result<List<String>>
    
    /** Check if AI service is available */
    suspend fun isAvailable(): Boolean
}

data class AnalysisContext(
    val patientAge: Int? = null,
    val patientGender: String? = null,  // "male" | "female"
    val medicalHistory: List<String> = emptyList(),
    val medications: List<String> = emptyList()
)

data class HealthInsight(
    val summary: String,
    val riskScore: Float,       // 0.0 (healthy) to 1.0 (critical)
    val findings: List<Finding>,
    val recommendations: List<String>,
    val generatedAt: Long
)

data class Finding(
    val category: String,       // "心率", "血压", etc.
    val status: String,         // "正常", "偏高", "异常"
    val detail: String,
    val severity: String        // "low" | "medium" | "high"
)