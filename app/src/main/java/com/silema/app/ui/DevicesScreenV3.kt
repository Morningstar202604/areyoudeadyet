package com.silema.app.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalType
import com.silema.app.store.rememberAppRepository
import com.silema.app.ui.components.GlassCard
import com.silema.app.ui.components.GradientBanner
import com.silema.app.ui.theme.AppSpacing
import com.silema.app.ui.theme.BrandBlue
import com.silema.app.ui.theme.BrandGreen
import com.silema.app.ui.theme.BrandPurple
import com.silema.app.ui.theme.BrandWarm
import com.silema.app.ui.theme.CardGradientBlue
import com.silema.app.ui.theme.CardGradientGreen
import com.silema.app.ui.theme.CardGradientOrange
import com.silema.app.ui.theme.DataHeart
import com.silema.app.ui.theme.DataOxygen
import com.silema.app.ui.theme.DataPressure

/**
 * 设备屏幕 V3 — 现代健康活力风。
 *
 * 展示已连接的设备、最近测量数据、设备管理入口。
 */
@Composable
fun DevicesScreenV3(onClose: () -> Unit = {}) {
    val repository = rememberAppRepository()
    val records by repository.records.collectAsState(initial = emptyList())

    val latest = remember(records) {
        records.groupBy { it.typeId }.mapValues { (_, list) -> list.maxByOrNull { it.timestampMillis } }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF1F8E9), Color(0xFFE8F5E9), Color(0xFFFFFFFF))
                )
            )
            .padding(horizontal = AppSpacing.screenPad),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = AppSpacing.xxl, bottom = 100.dp)
    ) {
        // 1. 标题
        item {
            Text(
                text = "设备管理",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "连接健康设备，自动同步测量数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 2. 最近测量数据
        item {
            Text(
                text = "最近测量",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))

            val dataItems = listOf(
                Triple(Icons.Default.Favorite, DataHeart, Triple(latest[VitalType.HEART_RATE.id]?.value?.toInt()?.toString() ?: "--", "次/分", "心率")),
                Triple(Icons.Default.MonitorHeart, DataPressure, Triple(latest[VitalType.SYSTOLIC.id]?.value?.toInt()?.toString() ?: "--", "mmHg", "血压")),
                Triple(Icons.Default.HealthAndSafety, DataOxygen, Triple(latest[VitalType.SPO2.id]?.value?.toInt()?.toString() ?: "--", "%", "血氧")),
                Triple(Icons.Default.DirectionsWalk, BrandGreen, Triple(latest[VitalType.STEPS.id]?.value?.toInt()?.toString() ?: "0", "步", "步数"))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                dataItems.take(2).forEach { (icon, color, data) ->
                    MiniDataCard(
                        icon = icon,
                        color = color,
                        value = data.first,
                        unit = data.second,
                        label = data.third,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                dataItems.drop(2).forEach { (icon, color, data) ->
                    MiniDataCard(
                        icon = icon,
                        color = color,
                        value = data.first,
                        unit = data.second,
                        label = data.third,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. 已连接设备
        item {
            Text(
                text = "已连接设备",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))

            val connectedDevices = listOf(
                Triple("PPG 相机测量", "手机摄像头", Icons.Default.CameraAlt, CardGradientOrange),
                Triple("Health Connect", "系统健康服务", Icons.Default.Sync, CardGradientBlue),
                Triple("手动记录", "手动输入数据", Icons.Default.Edit, CardGradientGreen)
            )

            connectedDevices.forEach { (name, desc, icon, gradient) ->
                DeviceCard(
                    name = name,
                    description = desc,
                    icon = icon,
                    gradient = gradient,
                    isConnected = true
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))
            }
        }

        // 4. 可连接设备
        item {
            Text(
                text = "可连接设备",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))

            val availableDevices = listOf(
                Triple("蓝牙心率带", "BLE 心率监测", Icons.Default.Bluetooth, BrandBlue),
                Triple("蓝牙血压计", "BLE 血压监测", Icons.Default.MonitorHeart, BrandPurple),
                Triple("蓝牙血氧仪", "BLE 血氧监测", Icons.Default.HealthAndSafety, BrandWarm)
            )

            availableDevices.forEach { (name, desc, icon, color) ->
                AvailableDeviceCard(
                    name = name,
                    description = desc,
                    icon = icon,
                    color = color
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))
            }
        }
    }
}

/**
 * 迷你数据卡片组件。
 */
@Composable
private fun MiniDataCard(
    icon: ImageVector,
    color: Color,
    value: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 已连接设备卡片组件。
 */
@Composable
private fun DeviceCard(
    name: String,
    description: String,
    icon: ImageVector,
    gradient: List<Color>,
    isConnected: Boolean
) {
    GlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isConnected) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "已连接",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandGreen,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * 可连接设备卡片组件。
 */
@Composable
private fun AvailableDeviceCard(
    name: String,
    description: String,
    icon: ImageVector,
    color: Color
) {
    GlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = { /* 扫描连接 */ }) {
                Text("连接", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
