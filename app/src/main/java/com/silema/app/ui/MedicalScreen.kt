package com.silema.app.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.silema.app.data.VitalRecord
import com.silema.app.medical.FhirExporter
import com.silema.app.medical.PdfReportGenerator
import com.silema.app.ui.components.BigButton
import com.silema.app.ui.components.InfoBar
import com.silema.app.ui.theme.BrandBlue
import com.silema.app.ui.theme.BrandGreen
import com.silema.app.ui.theme.BrandPurple
import com.silema.app.ui.theme.LevelNormal
import java.io.File

@Composable
fun MedicalScreen(records: List<VitalRecord>) {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    var messageOk by remember { mutableStateOf(true) }

    // 将内容写入 cache/shared_exports 并通过 FileProvider 分享（EXTRA_STREAM 需要 Uri）。
    // 返回 true 表示成功拉起了系统分享面板。
    fun shareContent(fileName: String, mimeType: String, content: String, title: String): Boolean {
        return runCatching {
            val dir = File(context.cacheDir, "shared_exports").apply { mkdirs() }
            val file = File(dir, fileName)
            file.writeText(content)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, title))
            true
        }.getOrDefault(false)
    }

    fun shareText(content: String, subject: String): Boolean {
        return runCatching {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content)
                putExtra(Intent.EXTRA_SUBJECT, subject)
            }
            context.startActivity(Intent.createChooser(intent, subject))
            true
        }.getOrDefault(false)
    }

    fun showResult(ok: Boolean, successMsg: String, failMsg: String) {
        message = if (ok) successMsg else failMsg
        messageOk = ok
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("医疗对接", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
            Text(
                "导出标准格式的健康数据，方便与医生沟通",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            InfoBar(
                text = "FHIR R4 是国际通用的医疗数据交换标准，导出的 JSON 可被医院信息系统（HIS/EHR）解析。",
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        // Option 1: FHIR R4 JSON（文件附件分享）
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(44.dp).background(BrandBlue.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.DataObject, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("导出 FHIR R4 JSON", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "标准医疗数据文件，可导入 EHR 系统",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    BigButton(
                        text = "生成并分享 FHIR JSON",
                        container = BrandBlue,
                        icon = Icons.Filled.Share,
                        enabled = records.isNotEmpty(),
                        onClick = {
                            val ok = shareContent(
                                fileName = "fhir_export_${System.currentTimeMillis()}.json",
                                mimeType = "application/json",
                                content = FhirExporter.export(records),
                                title = "健康体征数据 (FHIR R4)"
                            )
                            showResult(ok, "FHIR JSON 已生成，请选择分享方式", "分享失败：设备上没有可用的分享目标")
                        }
                    )
                }
            }
        }

        // Option 2: Text health report
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(44.dp).background(BrandGreen.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Description, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("导出文本健康报告", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "纯文本格式，方便打印或通过微信发送",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    BigButton(
                        text = "生成文本报告",
                        container = BrandGreen,
                        icon = Icons.Filled.Share,
                        enabled = records.isNotEmpty(),
                        onClick = {
                            val ok = shareText(FhirExporter.exportAsTextSummary(records), "健康体征报告")
                            showResult(ok, "文本报告已生成", "分享失败：设备上没有可用的分享目标")
                        }
                    )
                }
            }
        }

        // Option 3: Weekly summary
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(44.dp).background(BrandPurple.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.DateRange, contentDescription = null, tint = BrandPurple, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("导出周度总结", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "包含趋势对比和统计摘要的完整周报",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    BigButton(
                        text = "生成周度报告",
                        container = BrandPurple,
                        icon = Icons.Filled.Share,
                        enabled = records.isNotEmpty(),
                        onClick = {
                            val ok = shareText(PdfReportGenerator.generateWeeklyReport(records), "周度健康报告")
                            showResult(ok, "周度报告已生成", "分享失败：设备上没有可用的分享目标")
                        }
                    )
                }
            }
        }

        message?.let { msg ->
            item {
                val tint = if (messageOk) LevelNormal else MaterialTheme.colorScheme.error
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.08f))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(msg, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "当前共 ${records.size} 条体征记录可供导出",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            InfoBar(
                text = "提示：导出的数据仅供参考，不能替代正规医疗检查。就诊时请携带完整数据记录。",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
