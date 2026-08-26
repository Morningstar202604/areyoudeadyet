package com.silema.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalType
import com.silema.app.engine.RiskEngine
import com.silema.app.ui.components.*
import com.silema.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

data class FamilyMember(
    val name: String,
    val relationship: String,
    val lastOnlineMillis: Long,
    val latestVitals: Map<String, Double>,
    val overallLevel: com.silema.app.data.RiskLevel
)

@Composable
fun FamilyScreen(records: List<VitalRecord>) {
    var showDetail by remember { mutableStateOf<FamilyMember?>(null) }

    val familyMembers = remember(records) {
        buildDemoFamilyMembers(records)
    }

    if (showDetail != null) {
        FamilyDetailDialog(
            member = showDetail!!,
            records = records,
            onDismiss = { showDetail = null }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "远程监护",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "查看家人的健康数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledIconButton(
                    onClick = { },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "添加家人")
                }
            }
        }

        if (familyMembers.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.People,
                    title = "还没有添加家人",
                    message = "添加家人后，可以远程查看他们的健康数据和风险预警"
                )
            }
        } else {
            items(familyMembers) { member ->
                FamilyMemberCard(
                    member = member,
                    onClick = { showDetail = member }
                )
            }
        }

        item {
            InfoBar(
                text = "家人数据需要在对方手机上授权共享后才能查看。共享的数据会每小时自动同步一次。",
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun FamilyMemberCard(member: FamilyMember, onClick: () -> Unit) {
    val levelColor = riskColor(member.overallLevel)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(levelColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = levelColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = member.relationship,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LevelBadge(level = member.overallLevel)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val hr = member.latestVitals[VitalType.HEART_RATE.id]
                val spo2 = member.latestVitals[VitalType.SPO2.id]
                val sys = member.latestVitals[VitalType.SYSTOLIC.id]

                MiniVitalChip(
                    label = "心率",
                    value = hr?.let { "${it.toInt()}" } ?: "--",
                    color = hr?.let { riskColor(RiskEngine.metricLevel(VitalType.HEART_RATE, it)) }
                        ?: MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f)
                )
                MiniVitalChip(
                    label = "血氧",
                    value = spo2?.let { "${it.toInt()}%" } ?: "--",
                    color = spo2?.let { riskColor(RiskEngine.metricLevel(VitalType.SPO2, it)) }
                        ?: MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f)
                )
                MiniVitalChip(
                    label = "高压",
                    value = sys?.let { "${it.toInt()}" } ?: "--",
                    color = sys?.let { riskColor(RiskEngine.metricLevel(VitalType.SYSTOLIC, it)) }
                        ?: MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "最后在线：${RiskEngine.relativeTime(member.lastOnlineMillis, System.currentTimeMillis())}",
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
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FamilyDetailDialog(
    member: FamilyMember,
    records: List<VitalRecord>,
    onDismiss: () -> Unit
) {
    val recentRecords = remember(member, records) {
        records.sortedByDescending { it.timestampMillis }.take(20)
    }
    val assessment = remember(records) { RiskEngine.evaluate(records) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(member.name, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.width(8.dp))
                LevelBadge(level = member.overallLevel)
            }
        },
        text = {
            Column {
                Text(
                    "${member.relationship} · ${RiskEngine.relativeTime(member.lastOnlineMillis, System.currentTimeMillis())}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                SectionTitle("最新体征")
                val latestByType = recentRecords.groupBy { it.typeId }
                    .mapValues { (_, v) -> v.maxByOrNull { it.timestampMillis } }

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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
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

                if (assessment.alerts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SectionTitle("预警信息")
                    assessment.alerts.take(3).forEach { alert ->
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

private fun buildDemoFamilyMembers(records: List<VitalRecord>): List<FamilyMember> {
    val now = System.currentTimeMillis()
    val members = mutableListOf<FamilyMember>()

    val latestByType = records.groupBy { it.typeId }
        .mapValues { (_, v) -> v.maxByOrNull { it.timestampMillis } }

    members.add(
        FamilyMember(
            name = "爸爸",
            relationship = "父亲",
            lastOnlineMillis = now - 2 * 3600_000,
            latestVitals = mapOf(
                VitalType.HEART_RATE.id to (latestByType[VitalType.HEART_RATE.id]?.value ?: 72.0),
                VitalType.SPO2.id to (latestByType[VitalType.SPO2.id]?.value ?: 97.0),
                VitalType.SYSTOLIC.id to (latestByType[VitalType.SYSTOLIC.id]?.value ?: 126.0)
            ),
            overallLevel = com.silema.app.data.RiskLevel.NORMAL
        )
    )

    members.add(
        FamilyMember(
            name = "妈妈",
            relationship = "母亲",
            lastOnlineMillis = now - 5 * 3600_000,
            latestVitals = mapOf(
                VitalType.HEART_RATE.id to 68.0,
                VitalType.SPO2.id to 98.0,
                VitalType.SYSTOLIC.id to 122.0
            ),
            overallLevel = com.silema.app.data.RiskLevel.NORMAL
        )
    )

    return members
}
