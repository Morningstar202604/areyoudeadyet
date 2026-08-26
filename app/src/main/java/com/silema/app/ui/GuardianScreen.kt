package com.silema.app.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import com.silema.app.data.Contact
import com.silema.app.data.VitalRecord
import com.silema.app.hc.HealthConnectManager
import com.silema.app.store.AppRepository
import com.silema.app.ui.components.BigButton
import com.silema.app.ui.components.EmptyState
import com.silema.app.ui.components.GradientCard
import com.silema.app.ui.components.InfoBar
import com.silema.app.ui.components.ListItemCard
import com.silema.app.ui.components.SectionTitle
import com.silema.app.ui.theme.BrandBlue
import com.silema.app.ui.theme.BrandGreen
import com.silema.app.ui.theme.BrandSoftRed
import com.silema.app.ui.theme.BrandWarm
import com.silema.app.ui.theme.CardGradientBlue
import com.silema.app.ui.theme.CardGradientGreen
import com.silema.app.ui.theme.CardGradientOrange
import com.silema.app.ui.theme.CardGradientPurple
import com.silema.app.ui.theme.CardGradientRed
import com.silema.app.ui.theme.LevelCritical
import com.silema.app.ui.theme.LevelNormal
import kotlinx.coroutines.launch

@Composable
fun GuardianScreen(records: List<VitalRecord>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val contacts by AppRepository.contacts.collectAsState()

    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var feedbackMsg by remember { mutableStateOf<String?>(null) }

    var hcMessage by remember { mutableStateOf<String?>(null) }
    var hcBusy by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("确认清空全部数据？") },
            text = { Text("将删除所有体征记录、紧急联系人和设置，无法恢复。演示数据也会一并清除。") },
            confirmButton = {
                TextButton(onClick = {
                    AppRepository.clearAll()
                    feedbackMsg = "已清空全部数据"
                    confirmClear = false
                }) { Text("全部删除") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("取消") }
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

    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.isNotEmpty()) {
            scope.launch { doSync() }
        } else {
            hcMessage = "未授予任何读取权限，无法同步"
        }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    fun requestNotifIfNeeded(onOk: () -> Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            onOk()
            notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else onOk()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("守护与设置", style = MaterialTheme.typography.headlineSmall)
        }

        // ═══ Section 1: 远程同步 ═══
        item {
            SectionTitle("远程同步")
        }
        item {
            GradientCard(gradientColors = CardGradientBlue) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Refresh, contentDescription = null,
                            tint = Color.White, modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("数据同步", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(
                            "本地共 ${records.size} 条记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // ═══ Section 2: 紧急联系人 ═══
        item {
            SectionTitle("紧急联系人（SOS 时拨打/发短信的对象）")
        }
        val contactsList = contacts // already collected at top of function
        if (contactsList.isEmpty()) {
            item {
                InfoBar(
                    text = "建议至少添加 1 位家人，SOS 时才能一键联系。",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        } else {
            items(contactsList) { contact ->
                ContactCard(contact)
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = { Text("称呼（如：大女儿）") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it.filter { ch -> ch.isDigit() || ch == '+' || ch == '-' } },
                        label = { Text("手机号") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    BigButton(
                        text = "添加联系人",
                        container = BrandBlue,
                        onClick = {
                            val name = contactName.trim()
                            val phone = contactPhone.trim()
                            when {
                                name.isEmpty() -> feedbackMsg = "请填写称呼"
                                phone.length < 5 -> feedbackMsg = "手机号看起来不对，请检查"
                                else -> {
                                    AppRepository.addContact(Contact(name, phone))
                                    contactName = ""
                                    contactPhone = ""
                                    feedbackMsg = "已添加 $name"
                                }
                            }
                        }
                    )
                }
            }
        }

        feedbackMsg?.let { msg ->
            item {
                InfoBar(
                    text = msg,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        // ═══ Section 3: Health Connect ═══
        item {
            SectionTitle("穿戴设备数据（Health Connect）")
        }
        item {
            val available = remember { HealthConnectManager.isAvailable(context) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (available) LevelNormal.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (available) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = null,
                                tint = if (available) LevelNormal else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (available) "状态：可用" else "状态：不可用",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (available) "支持读取华为运动健康、小米运动健康等写入的心率、血氧、血压、步数。"
                        else "低版本安卓需先安装 Health Connect 应用。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    BigButton(
                        text = if (hcBusy) "正在同步…" else "同步最近 24 小时数据",
                        container = if (available) BrandWarm else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (available) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
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
                }
            }
        }

        hcMessage?.let { msg ->
            item {
                InfoBar(
                    text = msg,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        // ═══ Section 4: 提醒设置 ═══
        item {
            SectionTitle("提醒设置")
        }
        item {
            var remMeasure by remember { mutableStateOf(AppRepository.measureReminderOn) }
            var remHour by remember { mutableStateOf(AppRepository.measureReminderHour.toString()) }
            var remMin by remember { mutableStateOf(AppRepository.measureReminderMinute.toString()) }
            var remSed by remember { mutableStateOf(AppRepository.sedentaryReminderOn) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ListItemCard(
                        title = "每日测量提醒",
                        subtitle = if (remMeasure) "已开启" else "已关闭",
                        icon = Icons.Filled.DateRange,
                        trailing = {
                            Switch(
                                checked = remMeasure,
                                onCheckedChange = { on ->
                                    requestNotifIfNeeded {
                                        AppRepository.measureReminderOn = on
                                        remMeasure = on
                                        com.silema.app.work.Reminders.syncMeasurement(context)
                                        feedbackMsg = if (on) "测量提醒已开启" else "测量提醒已关闭"
                                    }
                                }
                            )
                        }
                    )

                    if (remMeasure) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = remHour,
                                onValueChange = { remHour = it.filter { c -> c.isDigit() }.take(2) },
                                label = { Text("时") },
                                singleLine = true,
                                modifier = Modifier.width(90.dp)
                            )
                            Text(":", style = MaterialTheme.typography.titleLarge)
                            OutlinedTextField(
                                value = remMin,
                                onValueChange = { remMin = it.filter { c -> c.isDigit() }.take(2) },
                                label = { Text("分") },
                                singleLine = true,
                                modifier = Modifier.width(90.dp)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                onClick = {
                                    val h = remHour.toIntOrNull()?.coerceIn(0, 23)
                                    val m = remMin.toIntOrNull()?.coerceIn(0, 59)
                                    if (h == null || m == null) feedbackMsg = "时间格式不对"
                                    else {
                                        AppRepository.measureReminderHour = h
                                        AppRepository.measureReminderMinute = m
                                        com.silema.app.work.Reminders.syncMeasurement(context)
                                        feedbackMsg = "提醒时间已设为 %02d:%02d".format(h, m)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(52.dp)
                            ) { Text("保存") }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ListItemCard(
                        title = "久坐提醒",
                        subtitle = "9:00-21:00 每小时提醒",
                        icon = Icons.Filled.Warning,
                        trailing = {
                            Switch(
                                checked = remSed,
                                onCheckedChange = { on ->
                                    requestNotifIfNeeded {
                                        AppRepository.sedentaryReminderOn = on
                                        remSed = on
                                        com.silema.app.work.Reminders.syncSedentary(context)
                                    }
                                }
                            )
                        }
                    )
                }
            }
        }

        // ═══ Section 5: 数据管理 ═══
        item {
            SectionTitle("数据管理")
        }
        item {
            ListItemCard(
                title = "导出健康数据",
                subtitle = "将所有记录导出为 JSON 文件",
                icon = Icons.Filled.DateRange,
                onClick = { feedbackMsg = "导出功能开发中…" }
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = LevelCritical.copy(alpha = 0.06f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(LevelCritical.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = LevelCritical, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("清空全部数据", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = LevelCritical)
                        Text("重置应用，无法恢复", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { confirmClear = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LevelCritical),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("清空", color = Color.White)
                    }
                }
            }
        }

        // ═══ Section 6: 关于 ═══
        item {
            SectionTitle("关于")
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "「死了吗？」v0.2.0",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "健康监测 · 生命管理",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ListItemCard(
                        title = "隐私声明",
                        subtitle = "所有数据只保存在手机本地，不联网上传",
                        icon = Icons.Filled.Info
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ListItemCard(
                        title = "免责声明",
                        subtitle = "不能替代医生诊断，遇到紧急情况请拨打 120",
                        icon = Icons.Filled.Warning
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ContactCard(contact: Contact) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Favorite, contentDescription = null,
                    tint = BrandBlue, modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
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
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "删除联系人", modifier = Modifier.size(20.dp))
            }
        }
    }
}
