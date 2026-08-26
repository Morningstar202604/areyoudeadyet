package com.silema.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silema.app.data.RiskLevel
import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalType
import com.silema.app.engine.RiskEngine
import com.silema.app.remote.FamilyMember
import com.silema.app.remote.RemoteSyncProvider
import com.silema.app.ui.components.EmptyState
import com.silema.app.ui.components.InfoBar
import com.silema.app.ui.components.LevelBadge
import com.silema.app.ui.components.SectionTitle
import com.silema.app.ui.theme.riskColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 家人卡片展示模型：远程成员 + 拉取到的最近 24h 体征。 */
private data class MemberCardData(
    val member: FamilyMember,
    val records: List<VitalRecord>,
    val overallLevel: RiskLevel?
)

@Composable
fun FamilyScreen() {
    val context = LocalContext.current

    var loading by remember { mutableStateOf(true) }
    var remoteAvailable by remember { mutableStateOf(false) }
    var members by remember { mutableStateOf<List<MemberCardData>>(emptyList()) }
    var showAddHint by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<MemberCardData?>(null) }

    LaunchedEffect(Unit) {
        val sync = RemoteSyncProvider.get(context)
        val available = runCatching { sync.isAvailable() }.getOrDefault(false)
        remoteAvailable = available
        if (available) {
            sync.getFamilyMembers()
                .onSuccess { list ->
                    val dayAgo = System.currentTimeMillis() - 24 * 3600_000L
                    members = list.map { m ->
                        val recs: List<VitalRecord> = runCatching {
                            sync.getFamilyVitals(m.id, dayAgo).getOrThrow()
                        }.getOrElse { emptyList() }
                        MemberCardData(
                            member = m,
                            records = recs.sortedByDescending { it.timestampMillis },
                            overallLevel = if (recs.isEmpty()) null else RiskEngine.evaluate(recs).level
                        )
                    }
                }
                .onFailure { members = emptyList() }
        } else {
            members = emptyList()
        }
        loading = false
    }

    if (showAddHint) {
        AlertDialog(
            onDismissRequest = { showAddHint = false },
            title = { Text("添加家人") },
            text = {
                Text("绑定家人的账号需要在部署时配置远程同步服务与账号体系（见项目文档 docs/remote-setup.md）。当前构建未连接后端，暂不可添加。")
            },
            confirmButton = {
                TextButton(onClick = { showAddHint = false }) { Text("知道了") }
            }
        )
    }

    detail?.let { d ->
        MemberDetailDialog(data = d, onDismiss = { detail = null })
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("远程监护", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        "查看家人的健康数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (remoteAvailable) {
                    FilledIconButton(onClick = { showAddHint = true }, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = "添加家人")
                    }
                }
            }
        }

        if (loading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (!remoteAvailable) {
            item {
                EmptyState(
                    icon = Icons.Filled.CloudOff,
                    title = "远程监护未启用",
                    message = "本页通过企业部署的远程同步服务获取家人数据。\n未配置后端时不可用，详见 docs/remote-setup.md"
                )
            }
        } else if (members.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.People,
                    title = "还没有可查看的家人",
                    message = "请先在对方设备上完成账号绑定与数据授权"
                )
            }
        } else {
            items(members, key = { it.member.id }) { data ->
                MemberCard(data = data, onClick = { detail = data })
            }
            item {
                InfoBar(
                    text = "家人数据需对方在本机授权共享后才会展示；仅显示最近 24 小时内的测量。",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun MemberCard(data: MemberCardData, onClick: () -> Unit) {
    val levelColor = data.overallLevel?.let { riskColor(it) } ?: MaterialTheme.colorScheme.outline
    val vitals = data.records.groupBy { it.typeId }
        .mapValues { (_, v) -> v.maxByOrNull { it.timestampMillis }?.value }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(levelColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = levelColor, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(data.member.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        data.member.relationship,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (data.overallLevel != null) LevelBadge(level = data.overallLevel)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MiniVitalChip(
                    label = "心率",
                    value = vitals[VitalType.HEART_RATE.id]?.let { "${it.toInt()}" } ?: "--",
                    color = vitals[VitalType.HEART_RATE.id]
                        ?.let { riskColor(RiskEngine.metricLevel(VitalType.HEART_RATE, it)) }
                        ?: MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f)
                )
                MiniVitalChip(
                    label = "血氧",
                    value = vitals[VitalType.SPO2.id]?.let { "${it.toInt()}%" } ?: "--",
                    color = vitals[VitalType.SPO2.id]
                        ?.let { riskColor(RiskEngine.metricLevel(VitalType.SPO2, it)) }
                        ?: MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f)
                )
                MiniVitalChip(
                    label = "高压",
                    value = vitals[VitalType.SYSTOLIC.id]?.let { "${it.toInt()}" } ?: "--",
                    color = vitals[VitalType.SYSTOLIC.id]
                        ?.let { riskColor(RiskEngine.metricLevel(VitalType.SYSTOLIC, it)) }
                        ?: MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (data.records.isEmpty()) "最近 24 小时无数据"
                else "最后更新：${SimpleDateFormat("MM-dd HH:mm", Locale.US).format(Date(data.records.first().timestampMillis))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MiniVitalChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            Text(value, style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MemberDetailDialog(data: MemberCardData, onDismiss: () -> Unit) {
    val latestByType = data.records.groupBy { it.typeId }
        .mapValues { (_, v) -> v.maxByOrNull { it.timestampMillis } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(data.member.name, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.width(8.dp))
                data.overallLevel?.let { LevelBadge(level = it) }
            }
        },
        text = {
            Column {
                Text(
                    data.member.relationship,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                SectionTitle("最新体征")
                listOf(
                    VitalType.HEART_RATE to "心率",
                    VitalType.SPO2 to "血氧",
                    VitalType.SYSTOLIC to "收缩压",
                    VitalType.DIASTOLIC to "舒张压",
                    VitalType.TEMPERATURE to "体温"
                ).forEach { (type, label) ->
                    val record = latestByType[type.id]
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            record?.let {
                                "${RiskEngine.metricLevel(type, it.value).label} · ${it.value.toInt()} ${type.unit}"
                            } ?: "暂无数据",
                            style = MaterialTheme.typography.bodyMedium,
                            color = record?.let { riskColor(RiskEngine.metricLevel(type, it.value)) }
                                ?: MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val alerts = data.records.takeIf { it.isNotEmpty() }
                    ?.let { RiskEngine.evaluate(it).alerts }
                    .orEmpty()
                if (alerts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SectionTitle("预警信息")
                    alerts.take(3).forEach { alert ->
                        Text(
                            "· ${alert.level.label}：${alert.problem}",
                            style = MaterialTheme.typography.bodySmall,
                            color = riskColor(alert.level),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}
