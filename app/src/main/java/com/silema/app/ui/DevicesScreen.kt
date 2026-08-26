package com.silema.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.silema.app.ble.BleVitals
import com.silema.app.ppg.PpgMeasureSection
import com.silema.app.store.AppRepository
import com.silema.app.ui.components.SectionTitle

/**
 * 检测中心：真实数据采集入口 ——
 *  1) 摄像头 PPG 心率实测（无需任何外设）
 *  2) 蓝牙 BLE 标准协议设备直连（心率带/电子血压计/血氧仪）
 */
@Composable
fun DevicesScreen(onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("检测中心", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "关闭")
            }
        }
        Spacer(Modifier.height(8.dp))

        SectionTitle("方式一：手机实测（不需要任何外设）")
        PpgMeasureSection()

        SectionTitle("方式二：蓝牙标准设备直连")
        BleSection()

        SectionTitle("关于华为 / 小米手环手表")
        EmptyHint(
            text = "华为、小米等品牌的手环手表使用私有蓝牙协议，无法被第三方应用直连；" +
                "它们的正确接入方式是：穿戴设备 → 厂商运动健康App → Health Connect → 本应用。" +
                "到「守护」页点「同步最近24小时数据」即可拉取。"
        )
        Spacer(Modifier.height(24.dp))
    }
}

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
        EmptyHint(text = "此设备不支持 BLE 蓝牙，无法直连外设。仍可使用摄像头实测与手动录入。")
        return
    }

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
                    // 启动失败信息会体现在 connectionState / 扫描状态上
                    BleVitals.stopScan(context)
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
        modifier = Modifier.fillMaxWidth().height(68.dp)
    ) {
        Text(if (scanning) "停止扫描" else "扫描附近的蓝牙测量设备", style = MaterialTheme.typography.titleMedium)
    }

    Spacer(Modifier.height(10.dp))
    Text(
        text = connState,
        style = MaterialTheme.typography.bodyMedium,
        color = if (connState.startsWith("收到")) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant
    )

    if (found.isEmpty() && scanning) {
        Spacer(Modifier.height(6.dp))
        EmptyHint(text = "正在搜索… 请确认设备已开机并处于配对/广播模式（多数心率带长按按键至指示灯快闪）。")
    }
    found.forEach { device ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text("${device.name}（${device.kind}）", style = MaterialTheme.typography.bodyLarge)
                Text(
                    device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { BleVitals.connect(context, device.address) },
                shape = RoundedCornerShape(12.dp),
                enabled = !scanning,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                modifier = Modifier.height(52.dp)
            ) {
                Text("连接", style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    if (live.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text("实时读数：", style = MaterialTheme.typography.titleSmall)
        Text(
            text = live.entries.joinToString("   ") { "${it.key} ${formatLive(it.value)}" },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(6.dp))
        EmptyHint(text = "每次设备推送的测量都会自动存入记录（来源标记为蓝牙），首页风险随之更新。")
    }

    if (connState != "未连接") {
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { BleVitals.disconnect() },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.fillMaxWidth().height(60.dp)
        ) {
            Text("断开连接")
        }
    }

    Spacer(Modifier.height(6.dp))
    Text(
        text = "兼容说明：支持 Bluetooth SIG 标准协议的设备均可直连（如 Polar 心率带、iHealth/欧姆龙部分血压计、标准血氧仪）。" +
            "共 ${AppRepository.records.value.size} 条本地记录。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun formatLive(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)
