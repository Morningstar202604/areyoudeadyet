package com.silema.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silema.app.ai.AiAnalyzerProvider
import com.silema.app.ai.Finding
import com.silema.app.ai.HealthInsight
import com.silema.app.data.VitalRecord
import com.silema.app.ui.components.*
import com.silema.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AiReportScreen(records: List<VitalRecord>) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    var insight by remember { mutableStateOf<HealthInsight?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastGeneratedAt by remember { mutableStateOf<Long?>(null) }

    fun analyze() {
        isLoading = true
        error = null
        scope.launch {
            try {
                val analyzer = AiAnalyzerProvider.get(context)
                val result = analyzer.analyze(records, com.silema.app.ai.AnalysisContext())
                result.onSuccess { healthInsight ->
                    insight = healthInsight
                    lastGeneratedAt = healthInsight.generatedAt
                }
                result.onFailure { e ->
                    // 如果是 API Key 未配置或网络错误，自动降级到本地模式
                    if (e.message?.contains("API Key") == true || e is java.net.UnknownHostException || e is java.net.SocketTimeoutException) {
                        val localAnalyzer = com.silema.app.ai.LocalAiAnalyzer()
                        val fallback = localAnalyzer.analyze(records, com.silema.app.ai.AnalysisContext())
                        fallback.onSuccess { 
                            insight = it
                            error = "云端 API 不可用，已使用本地规则引擎"
                        }
                        fallback.onFailure { fallbackError ->
                            error = "分析失败：${fallbackError.message ?: fallbackError.javaClass.simpleName}"
                        }
                    } else {
                        error = "分析失败：${e.message ?: e.javaClass.simpleName}"
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                error = "分析失败：${e.message ?: e.javaClass.simpleName}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(records.size) {
        if (records.isNotEmpty() && insight == null && !isLoading) {
            analyze()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "AI 健康分析",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "基于体征数据的智能分析与建议",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isLoading) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "正在分析您的健康数据…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "这可能需要几秒钟",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        error?.let { errorMsg ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = LevelCritical.copy(alpha = 0.08f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Error,
                                contentDescription = null,
                                tint = LevelCritical,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "分析出错",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = LevelCritical
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorMsg, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { analyze() }) {
                            Text("重试")
                        }
                    }
                }
            }
        }

        insight?.let { data ->
            // Risk score gauge
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            "综合风险评分",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        val progress = data.riskScore
                        val ringColor = when {
                            progress >= 0.7f -> LevelCritical
                            progress >= 0.4f -> LevelWarning
                            progress >= 0.2f -> LevelWatch
                            else -> LevelNormal
                        }
                        val sizeDp = 160
                        Box(contentAlignment = Alignment.Center) {
                            ProgressRing(
                                progress = progress,
                                color = ringColor,
                                trackColor = ringColor.copy(alpha = 0.12f),
                                sizeDp = sizeDp
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${(progress * 100).toInt()}",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = ringColor,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "/ 100",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            data.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // Findings
            if (data.findings.isNotEmpty()) {
                item {
                    SectionTitle("检查发现")
                }
                items(data.findings, key = { "${it.category}_${it.detail}" }) { finding ->
                    FindingCard(finding)
                }
            }

            // Recommendations
            if (data.recommendations.isNotEmpty()) {
                item {
                    SectionTitle("健康建议")
                }
                items(data.recommendations, key = { it }) { recommendation ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = BrandGreen.copy(alpha = 0.08f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                recommendation,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Timestamp
            item {
                lastGeneratedAt?.let { ts ->
                    Text(
                        "分析时间：${com.silema.app.engine.RiskEngine.clockText(ts)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        if (records.isEmpty() && !isLoading) {
            item {
                EmptyState(
                    icon = Icons.Filled.Info,
                    title = "暂无数据",
                    message = "请先在「录入」页添加体征数据，AI 分析需要至少几条记录"
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun FindingCard(finding: Finding) {
    val (color, bgColor) = when (finding.severity) {
        "high" -> LevelCritical to LevelCritical.copy(alpha = 0.08f)
        "medium" -> LevelWarning to LevelWarning.copy(alpha = 0.08f)
        else -> LevelNormal to LevelNormal.copy(alpha = 0.08f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        finding.category,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        finding.status,
                        style = MaterialTheme.typography.labelMedium,
                        color = color,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    finding.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
