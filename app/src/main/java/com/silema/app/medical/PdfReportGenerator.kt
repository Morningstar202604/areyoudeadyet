package com.silema.app.medical

import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates a plain-text health report suitable for sharing.
 * For full PDF generation, companies can integrate iText or Android PdfDocument.
 */
object PdfReportGenerator {
    
    fun generateWeeklyReport(
        records: List<VitalRecord>,
        patientName: String = "老人",
        reportTitle: String = "周度健康报告"
    ): String {
        val sb = StringBuilder()
        val now = System.currentTimeMillis()
        val weekAgo = now - 7 * 86400000L
        val weekRecords = records.filter { it.timestampMillis >= weekAgo }
        
        sb.appendLine("╔══════════════════════════════════════╗")
        sb.appendLine("║         $reportTitle              ║")
        sb.appendLine("╠══════════════════════════════════════╣")
        sb.appendLine("║ 患者: $patientName")
        sb.appendLine("║ 报告期: ${formatDate(weekAgo)} ~ ${formatDate(now)}")
        sb.appendLine("║ 记录总数: ${weekRecords.size} 条")
        sb.appendLine("╚══════════════════════════════════════╝")
        sb.appendLine()
        
        VitalType.entries.filter { it != VitalType.STRESS }.forEach { type ->
            val typeRecords = weekRecords.filter { it.typeId == type.id }.sortedByDescending { it.timestampMillis }
            if (typeRecords.isNotEmpty()) {
                val values = typeRecords.map { it.value }
                val avg = values.average()
                val min = values.min()
                val max = values.max()
                sb.appendLine("【${type.displayName}】 (${type.unit})")
                sb.appendLine("  测量次数: ${typeRecords.size}")
                sb.appendLine("  平均值: %.1f".format(avg))
                sb.appendLine("  最低值: %.1f".format(min))
                sb.appendLine("  最高值: %.1f".format(max))
                if (typeRecords.size >= 3) {
                    val recent = values.take(3).average()
                    val older = values.takeLast(3).average()
                    val trend = when {
                        recent > older * 1.1 -> "↑ 上升趋势"
                        recent < older * 0.9 -> "↓ 下降趋势"
                        else -> "→ 稳定"
                    }
                    sb.appendLine("  趋势: $trend")
                }
                sb.appendLine()
            }
        }
        
        sb.appendLine("───────────────────────────────────────")
        sb.appendLine("⚠️ 免责声明：本报告基于采集数据自动生成，")
        sb.appendLine("   仅供参考，不构成医疗诊断或治疗建议。")
        sb.appendLine("   如有健康问题请及时就医。")
        sb.appendLine("───────────────────────────────────────")
        sb.appendLine("报告生成时间: ${formatDate(now)}")
        sb.appendLine("导出方: 死了吗？健康监测系统")
        
        return sb.toString()
    }
    
    private fun formatDate(millis: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
    }
}