package com.silema.app.medical

import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * FHIR R4 Bundle exporter for health records.
 * Produces standard JSON that any EHR/HIS system can consume.
 * Reference: https://www.hl7.org/fhir/STU4/
 */
object FhirExporter {
    
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    
    fun export(records: List<VitalRecord>, patientName: String = "老人"): String {
        val bundle = FhirBundle(
            resourceType = "Bundle",
            id = UUID.randomUUID().toString(),
            type = "collection",
            timestamp = formatTime(System.currentTimeMillis()),
            entry = records.map { recordToObservation(it) }
        )
        return json.encodeToString(bundle)
    }
    
    fun exportAsTextSummary(records: List<VitalRecord>, patientName: String = "老人"): String {
        val sb = StringBuilder()
        sb.appendLine("═══════════════════════════════════")
        sb.appendLine("  健康体征报告 (FHIR R4 格式)")
        sb.appendLine("═══════════════════════════════════")
        sb.appendLine("患者: $patientName")
        sb.appendLine("记录数: ${records.size}")
        sb.appendLine("导出时间: ${formatTime(System.currentTimeMillis())}")
        sb.appendLine("───────────────────────────────────")
        
        records.groupBy { it.typeId }.forEach { (typeId, typeRecords) ->
            val type = VitalType.fromId(typeId)
            sb.appendLine("【${type?.displayName ?: typeId}】")
            typeRecords.takeLast(5).forEach { r ->
                sb.appendLine("  ${formatTime(r.timestampMillis)}  ${r.value} ${type?.unit ?: ""}  [${r.source}]")
            }
            sb.appendLine()
        }
        
        sb.appendLine("═══════════════════════════════════")
        sb.appendLine("⚠️ 本报告仅供参考，不构成医疗诊断")
        sb.appendLine("═══════════════════════════════════")
        return sb.toString()
    }
    
    private fun recordToObservation(record: VitalRecord): FhirEntry {
        val type = VitalType.fromId(record.typeId)
        return FhirEntry(
            fullUrl = "Observation/${UUID.randomUUID()}",
            resource = FhirObservation(
                resourceType = "Observation",
                id = UUID.randomUUID().toString(),
                status = "final",
                code = FhirCodeableConcept(
                    coding = listOf(FhirCoding(
                        system = "http://loinc.org",
                        code = loincCode(record.typeId),
                        display = type?.displayName ?: record.typeId
                    ))
                ),
                valueQuantity = FhirQuantity(
                    value = record.value,
                    unit = type?.unit ?: "",
                    system = "http://unitsofmeasure.org"
                ),
                effectiveDateTime = formatTime(record.timestampMillis)
            )
        )
    }
    
    private fun loincCode(typeId: String): String = when (typeId) {
        "heart_rate" -> "8867-4"
        "systolic" -> "8480-6"
        "diastolic" -> "8462-4"
        "spo2" -> "2708-6"
        "temperature" -> "8310-5"
        "steps" -> "41901-0"
        else -> "unknown"
    }
    
    private fun formatTime(millis: Long): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(Date(millis))
    }
}

@Serializable
data class FhirBundle(
    val resourceType: String,
    val id: String,
    val type: String,
    val timestamp: String,
    val entry: List<FhirEntry>
)

@Serializable
data class FhirEntry(
    val fullUrl: String,
    val resource: FhirObservation
)

@Serializable
data class FhirObservation(
    val resourceType: String,
    val id: String,
    val status: String,
    val code: FhirCodeableConcept,
    val valueQuantity: FhirQuantity,
    val effectiveDateTime: String
)

@Serializable
data class FhirCodeableConcept(
    val coding: List<FhirCoding>
)

@Serializable
data class FhirCoding(
    val system: String,
    val code: String,
    val display: String
)

@Serializable
data class FhirQuantity(
    val value: Double,
    val unit: String,
    val system: String
)