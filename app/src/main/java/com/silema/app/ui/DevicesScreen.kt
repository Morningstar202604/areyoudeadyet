package com.silema.app.ui

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.silema.app.ble.BleVitals
import com.silema.app.ppg.PpgMeasureSection
import com.silema.app.store.AppRepository
import com.silema.app.ui.components.GradientCard
import com.silema.app.ui.components.ListItemCard
import com.silema.app.ui.components.SectionTitle
import com.silema.app.ui.theme.BrandBlue
import com.silema.app.ui.theme.BrandGreen
import com.silema.app.ui.theme.BrandPurple
import com.silema.app.ui.theme.BrandWarm
import com.silema.app.ui.theme.CardGradientBlue
import com.silema.app.ui.theme.CardGradientGreen
import com.silema.app.ui.theme.CardGradientOrange
import com.silema.app.ui.theme.CardGradientPurple
import com.silema.app.ui.theme.LevelNormal

@Composable
fun DevicesScreen(onClose: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("检测中心", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "关闭")
                }
            }
        }

        // ═══ Section 1: Camera PPG ═══
        item {
            SectionTitle("方式一：手机实测（不需要任何外设）")
        }
        item {
            GradientCard(gradientColors = CardGradientOrange) {
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
                            Icons.Filled.Favorite, contentDescription = null,
                            tint = Color.White, modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("摄像头心率检测", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(
                            "使用手机摄像头和闪光灯实时测量心率",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
        item {
            PpgMeasureSection()
        }

        // ═══ Section 2: BLE Devices ═══
        item {
            SectionTitle("方式二：蓝牙标准设备直连")
        }
        item {
            BleSection()
        }

        // ═══ Section 3: Wearable Sync ═══
        item {
            SectionTitle("穿戴设备同步")
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
                        Text("Health Connect 集成", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(
                            "华为、小米等品牌手环手表需通过 Health Connect 接入",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
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
                    Text(
                        text = "接入路径：穿戴设备 → 厂商运动健康 App → Health Connect → 本应用",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "共 ${AppRepository.records.value.size} 条本地记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "请到「守护」页点「同步最近 24 小时数据」拉取穿戴设备数据。",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandBlue
                    )
                }
            }
        }

        // ═══ Section 4: IoT Device Catalog ═══
        item {
            SectionTitle("支持的设备类型")
        }
        val deviceTypes = listOf(
            DeviceTypeItem("心率带", "Bluetooth SIG 标准协议", Icons.Filled.Favorite, BrandWarm),
            DeviceTypeItem("电子血压计", "蓝牙标准血压协议", Icons.Filled.Phone, BrandBlue),
            DeviceTypeItem("脉搏血氧仪", "蓝牙标准血氧协议", Icons.Filled.Info, BrandGreen),
            DeviceTypeItem("华为手环/手表", "通过 Health Connect", Icons.Filled.DateRange, BrandPurple),
            DeviceTypeItem("小米手环/手表", "通过 Health Connect", Icons.Filled.DateRange, BrandPurple),
            DeviceTypeItem("智能体脂秤", "蓝牙标准体重协议", Icons.Filled.Home, BrandWarm)
        )
        items(deviceTypes.chunked(2)) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { item ->
                    DeviceTypeCard(item, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun DeviceTypeCard(item: DeviceTypeItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(item.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, contentDescription = null, tint = item.color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(item.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private data class DeviceTypeItem(val name: String, val desc: String, val icon: ImageVector, val color: Color)

@Composable
private fun BleSection() {
    val context = LocalContext.current

    val neededPermissions: Array<String> = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    fun allGranted(): Boolean = neededPermissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    var granted by remember { mutableStateOf(allGranted()) }
    var pendingScan by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        granted = allGranted()
        if (granted && pendingScan) {
            pendingScan = false
            BleVitals.startScan(context)
        }
    }

    val scanning by BleVitals.scanning.collectAsState()
    val found by BleVitals.found.collectAsState()
    val connState by BleVitals.connectionState.collectAsState()
    val live by BleVitals.liveReadings.collectAsState()

    val hasBle = remember { BleVitals.hasBluetooth(context) }

    if (!hasBle) {
        InfoBar(
            text = "此设备不支持 BLE 蓝牙，无法直连外设。仍可使用摄像头实测与手动录入。",
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Button(
                onClick = {
                    if (scanning) {
                        BleVitals.stopScan(context)
                    } else if (!granted) {
                        pendingScan = true
                        permissionLauncher.launch(neededPermissions)
                    } else {
                        val msg = BleVitals.startScan(context)
                        if (msg != "扫描中… 请让设备进入配对/广播模式") {
                            BleVitals.stopScan(context)
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (scanning) MaterialTheme.colorScheme.error else BrandBlue
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    if (scanning) Icons.Filled.Close else Icons.Filled.Build,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (scanning) "停止扫描" else "扫描蓝牙测量设备",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = connState,
                style = MaterialTheme.typography.bodyMedium,
                color = if (connState.startsWith("收到")) BrandBlue else MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (found.isEmpty() && scanning) {
                Spacer(modifier = Modifier.height(8.dp))
                InfoBar(
                    text = "正在搜索… 请确认设备已开机并处于配对模式。",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            found.forEach { device ->
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${device.name}（${device.kind}）", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                device.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { BleVitals.connect(context, device.address) },
                            shape = RoundedCornerShape(12.dp),
                            enabled = !scanning,
                            contentPadding = ButtonDefaults.TextButtonContentPadding,
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("连接", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            if (live.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text("实时读数：", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = live.entries.joinToString("   ") { "${it.key} ${formatLive(it.value)}" },
                    style = MaterialTheme.typography.headlineSmall,
                    color = BrandBlue
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "每次设备推送的测量都会自动存入记录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (connState != "未连接") {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { BleVitals.disconnect() },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("断开连接")
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun InfoBar(
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = contentColor)
    }
}

private fun formatLive(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)
