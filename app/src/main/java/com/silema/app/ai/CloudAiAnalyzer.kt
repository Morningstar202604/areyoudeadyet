package com.silema.app.ai

import com.silema.app.data.VitalRecord
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 云端 AI 分析器：通过 NewAPI 中转站调用大模型（DeepSeek/Qwen/GLM）。
 * 用户需在配置中填入 API Key，未配置时自动降级到 LocalAiAnalyzer。
 */
class CloudAiAnalyzer(
    private val baseUrl: String = "https://api.hcnsec.cn/v1",
    private val apiKey: String? = null,
    private val model: String = "qwen-plus"
) : AiAnalyzer {

    private val json = Json { ignoreUnknownKeys = true }
    private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    override suspend fun analyze(records: List<VitalRecord>, context: AnalysisContext): Result<HealthInsight> {
        if (apiKey.isNullOrEmpty()) {
            return Result.failure(IllegalStateException("未配置 API Key，请检查设置"))
        }
        return try {
            val prompt = buildPrompt(records, context)
            val response = callLLM(prompt)
            Result.success(parseResponse(response))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun explainRisk(records: List<VitalRecord>): Result<String> {
        if (apiKey.isNullOrEmpty()) {
            return Result.failure(IllegalStateException("未配置 API Key"))
        }
        return try {
            val prompt = "作为老年健康专家，请用通俗语言解释以下体征数据的风险（200字以内）：\n${formatRecords(records)}"
            val response = callLLM(prompt)
            Result.success(response.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun suggestRecommendations(records: List<VitalRecord>): Result<List<String>> {
        if (apiKey.isNullOrEmpty()) {
            return Result.failure(IllegalStateException("未配置 API Key"))
        }
        return try {
            val prompt = "基于以下老人体征数据，给出3-5条具体可操作的健康建议（每条不超过20字，用数字编号）：\n${formatRecords(records)}"
            val response = callLLM(prompt)
            val lines = response.split("\n")
                .filter { it.isNotBlank() }
                .map { it.replace(Regex("^\\d+[.、]"), "").trim() }
                .filter { it.isNotEmpty() }
            Result.success(lines.take(5))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isAvailable(): Boolean = !apiKey.isNullOrEmpty()

    /** 构造 LLM 提示词 */
    private fun buildPrompt(records: List<VitalRecord>, context: AnalysisContext): String {
        val sb = StringBuilder()
        sb.appendLine("你是一位老年健康管理专家，请根据以下体征数据生成健康分析报告：")
        sb.appendLine()
        if (context.patientAge != null) sb.appendLine("年龄：${context.patientAge}岁")
        if (context.patientGender != null) sb.appendLine("性别：${if (context.patientGender == "male") "男" else "女"}")
        if (context.medicalHistory.isNotEmpty()) sb.appendLine("病史：${context.medicalHistory.joinToString("、")}")
        if (context.medications.isNotEmpty()) sb.appendLine("用药：${context.medications.joinToString("、")}")
        sb.appendLine()
        sb.appendLine("最近体征数据：")
        records.sortedByDescending { it.timestampMillis }.take(20).forEach { r ->
            val type = r.type?.displayName ?: r.typeId
            val timeStr = dateFormat.format(r.timestampMillis)
            sb.appendLine("- $type: ${r.value} ${r.type?.unit ?: ""} ($timeStr)")
        }
        sb.appendLine()
        sb.appendLine("请按以下 JSON 格式输出（不要有其他文字）：")
        sb.appendLine("""
        {
          "summary": "一句话总结整体健康状况",
          "riskScore": 0.0-1.0之间的数字,
          "findings": [
            {"category": "指标名称", "status": "正常/偏高/异常", "detail": "详细说明", "severity": "low/medium/high"}
          ],
          "recommendations": ["建议1", "建议2", "建议3"]
        }
        """.trimIndent())
        return sb.toString()
    }

    /** 调用 NewAPI 中转站 */
    private fun callLLM(prompt: String): String {
        val url = URL("$baseUrl/chat/completions")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.setRequestProperty("Authorization", "Bearer $apiKey")
        conn.doOutput = true
        conn.connectTimeout = 15000
        conn.readTimeout = 30000

        val body = buildJsonObject {
            put("model", model)
            putJsonArray("messages") {
                add(buildJsonObject {
                    put("role", "user")
                    put("content", prompt)
                })
            }
            put("temperature", 0.7)
            put("max_tokens", 1000)
        }

        conn.outputStream.use { 
            it.write(body.toString().toByteArray(Charsets.UTF_8)) 
        }

        val responseCode = conn.responseCode
        val response = if (responseCode == 200) {
            conn.inputStream.use { it.bufferedReader(Charsets.UTF_8).readText() }
        } else {
            val errorBody = conn.errorStream?.use { it.bufferedReader(Charsets.UTF_8).readText() } ?: "未知错误"
            conn.disconnect()
            throw IllegalStateException("API 请求失败 ($responseCode): $errorBody")
        }
        conn.disconnect()

        // 解析 OpenAI 兼容格式响应
        val jsonResp = json.parseToJsonElement(response).jsonObject
        val choices = jsonResp["choices"]?.jsonArray 
            ?: throw IllegalStateException("无效响应：缺少 choices 字段")
        if (choices.isEmpty()) {
            throw IllegalStateException("API 返回空回答")
        }
        val message = choices.first().jsonObject["message"]?.jsonObject
            ?: throw IllegalStateException("无回答内容")
        return message["content"]?.jsonPrimitive?.content 
            ?: throw IllegalStateException("内容为空")
    }

    /** 解析 LLM 返回的 JSON */
    private fun parseResponse(response: String): HealthInsight {
        // 尝试提取 JSON（LLM 可能包裹在 ```json ... ``` 中）
        val jsonStr = response.substringAfter("```json").substringBefore("```").trim()
            .takeIf { it.isNotEmpty() } ?: response.trim()
        
        val obj = json.parseToJsonElement(jsonStr).jsonObject
        return HealthInsight(
            summary = obj["summary"]?.jsonPrimitive?.content ?: "分析完成",
            riskScore = obj["riskScore"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 0.5f,
            findings = obj["findings"]?.jsonArray?.mapNotNull { f ->
                try {
                    val fo = f.jsonObject
                    Finding(
                        category = fo["category"]?.jsonPrimitive?.content ?: "未知",
                        status = fo["status"]?.jsonPrimitive?.content ?: "正常",
                        detail = fo["detail"]?.jsonPrimitive?.content ?: "",
                        severity = fo["severity"]?.jsonPrimitive?.content ?: "low"
                    )
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList(),
            recommendations = obj["recommendations"]?.jsonArray?.mapNotNull { 
                it.jsonPrimitive.content.takeIf { s -> s.isNotBlank() }
            } ?: emptyList(),
            generatedAt = System.currentTimeMillis()
        )
    }

    private fun formatRecords(records: List<VitalRecord>): String {
        return records.sortedByDescending { it.timestampMillis }.take(10).joinToString("\n") { r ->
            "${r.type?.displayName}: ${r.value} ${r.type?.unit ?: ""}"
        }
    }
}
