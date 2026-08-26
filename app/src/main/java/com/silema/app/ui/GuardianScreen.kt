package com.silema.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.health.connect.client.PermissionController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.rememberCoroutineScope
import com.silema.app.data.Contact
import com.silema.app.hc.HealthConnectManager
import com.silema.app.store.AppRepository
import com.silema.app.ui.components.BigButton
import com.silema.app.ui.components.SectionTitle
import kotlinx.coroutines.launch

@Composable
fun GuardianScreen(records: List<com.silema.app.data.VitalRecord>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactMsg by remember { mutableStateOf<String?>(null) }

    var hcMessage by remember { mutableStateOf<String?>(null) }
    var hcBusy by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    if (confirmClear) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("确认清空全部数据？") },
            text = { Text("将删除所有体征记录、紧急联系人和设置，无法恢复。演示数据也会一并清除。") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    AppRepository.clearAll()
                    contactMsg = "已清空全部数据"
                    confirmClear = false
                }) { Text("全部删除") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmClear = false }) { Text("取消") }
            }
        )
    }

    suspend fun doSync() {
        hcBusy = true
        hcMessage = try {
            val pulled = HealthConnectManager.pullLast24h(context)
            if (pulled.isEmpty()) {
                "穿戴设备最近 24 小时没有新数据（确认手表已连接且华为运动健康已上传）"
            } else {
                val added = AppRepository.mergeHealthConnect(pulled)
                if (added == 0) "同步完成：拉到 ${pulled.size} 条，但都已有记录，无新增"
                else "同步完成：新增 $added 条数据，首页风险已重新评估"
            }
        } catch (e: Exception) {
            "同步失败：${e.message ?: e.javaClass.simpleName}。可检查 Health Connect 权限后重试"
        } finally {
            hcBusy = false
        }
    }

    // Health Connect 授权流程：先请求缺失权限，授权回调后再真正拉数据
    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.isNotEmpty()) {
            scope.launch { doSync() }
        } else {
            hcMessage = "未授予任何读取权限，无法同步"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("守护与设置", style = MaterialTheme.typography.headlineSmall)

        // ---------- 紧急联系人 ----------
        SectionTitle("紧急联系人（SOS 时拨打/发短信的对象）")
        val contacts by AppRepository.contacts.collectAsState()
        if (contacts.isEmpty()) {
            EmptyHint(text = "还没有添加家人电话。强烈建议至少添加 1 个，SOS 才能一键联系家人。")
        } else {
            contacts.forEach { c -> ContactRow(c) ; Spacer(Modifier.height(6.dp)) }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = contactName,
            onValueChange = { contactName = it },
            label = { Text("称呼（如：大女儿）") },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = contactPhone,
            onValueChange = { contactPhone = it.filter { ch -> ch.isDigit() || ch == '+' || ch == '-' } },
            label = { Text("手机号") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                val name = contactName.trim()
                val phone = contactPhone.trim()
                when {
                    name.isEmpty() -> contactMsg = "请填写称呼"
                    phone.length < 5 -> contactMsg = "手机号看起来不对，请检查"
                    else -> {
                        AppRepository.addContact(Contact(name, phone))
                        contactName = ""
                        contactPhone = ""
                        contactMsg = "已添加 $name"
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
            Text("添加联系人", style = MaterialTheme.typography.titleMedium)
        }
        contactMsg?.let {
            Spacer(Modifier.height(8.dp))
            EmptyHint(text = it)
        }

        // ---------- Health Connect ----------
        SectionTitle("穿戴设备数据（Health Connect）")
        val available = remember { HealthConnectManager.isAvailable(context) }
        Text(
            text = if (available) {
                "状态：可用。支持读取华为运动健康、小米运动健康等写入的心率、血氧、血压、步数。"
            } else {
                "状态：${HealthConnectManager.unavailableReason(context)}。低版本安卓需先安装 Health Connect 应用。"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        BigButton(
            text = if (hcBusy) "正在同步…" else "同步最近 24 小时数据",
            container = if (available) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (available) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            enabled = available && !hcBusy,
            onClick = {
                scope.launch {
                    val granted = runCatching { HealthConnectManager.grantedPermissions(context) }
                        .getOrDefault(emptySet())
                    val missing = HealthConnectManager.READ_PERMISSIONS - granted
                    if (missing.isEmpty()) doSync() else permissionLauncher.launch(HealthConnectManager.READ_PERMISSIONS)
                }
            }
        )
        hcMessage?.let {
            Spacer(Modifier.height(8.dp))
            EmptyHint(text = it)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.padding(start = 4.dp))
            Text(
                text = "当前本地共 ${records.size} 条记录",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ---------- 规则透明化 ----------
        SectionTitle("本应用的预警标准（全部公开，不搞玄学）")
        val rules = listOf(
            "血压 ≥180 或低压 ≥110 → 危险（高血压危象，立即送医）；160-179 / 100-109 → 警告；140-159 / 90-99 → 注意；高压 <90 或低压 <55 → 危险（休克风险）",
            "心率 ≥150 或 ≤45 → 危险；121-149 或 46-49 → 警告；100-120 → 注意",
            "血氧 <90% → 危险（呼吸衰竭水平）；90-93% → 警告；94-95% → 注意",
            "体温 ≥39.5℃ 或 ≤35℃ → 危险；38.5-39.4℃ → 警告；37.3-38.4℃ → 注意",
            "组合规则：低压+心跳快=休克代偿、缺氧+心跳快=呼吸循环衰竭、发热+心跳过快=重症感染信号 —— 单看正常、合起来危险的情况单独升级处理",
            "连续 3 次同方向超标自动升一级，防止\"再观察观察\"拖出大事"
        )
        rules.forEachIndexed { i, r ->
            Text(
                text = "${i + 1}. $r",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        SectionTitle("关于与免责声明")
        Text(
            text = "「死了吗？」v0.2.0 · 健康监测 · 生命管理\n" +
                "本应用基于公开医学共识的阈值规则做风险提示，目的是\"宁可多提醒，绝不装没事\"。" +
                "它不能替代医生的诊断，也不能替代正规医疗设备。遇到紧急情况永远优先拨打 120。\n" +
                "所有数据只保存在手机本地，不联网上传。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))
        BigButton(
            text = "清空全部数据（重置应用）",
            container = com.silema.app.ui.theme.LevelCritical,
            onClick = { confirmClear = true }
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ContactRow(contact: Contact) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.weight(1f)) {
            Text(contact.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                contact.phone,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FilledIconButton(
            onClick = { AppRepository.removeContact(contact.phone) },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.height(48.dp).width(48.dp)
        ) {
            Icon(Icons.Filled.Delete, contentDescription = "删除联系人")
        }
    }
}
