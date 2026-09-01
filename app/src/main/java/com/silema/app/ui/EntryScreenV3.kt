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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.silema.app.ui.theme.CardGradientPurple
import com.silema.app.ui.theme.CardGradientRed
import com.silema.app.ui.theme.DataHeart
import com.silema.app.ui.theme.DataOxygen
import com.silema.app.ui.theme.DataPressure
import com.silema.app.ui.theme.DataTemp

/**
 * 测量入口屏幕 V3 — 现代健康活力风。
 *
 * 展示各种测量方式入口：PPG 相机测量、手动记录、蓝牙设备、Health Connect。
 */
@Composable
fun EntryScreenV3(onClose: () -> Unit = {}) {
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
                text = "测量数据",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "选择测量方式，记录健康数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 2. PPG 相机测量（主推）
        item {
            GradientBanner(
                title = "PPG 相机测量",
                subtitle = "使用手机摄像头测量心率和血氧",
                gradientColors = CardGradientRed,
                icon = Icons.Default.CameraAlt
            )
        }

        // 3. 测量类型选择
        item {
            Text(
                text = "测量类型",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))

            val measureTypes = listOf(
                Triple("心率", "测量心率（次/分）", Icons.Default.Favorite, DataHeart, CardGradientRed),
                Triple("血压", "记录收缩压/舒张压", Icons.Default.MonitorHeart, DataPressure, CardGradientBlue),
                Triple("血氧", "测量血氧饱和度（%）", Icons.Default.HealthAndSafety, DataOxygen, CardGradientGreen),
                Triple("体温", "记录体温（℃）", Icons.Default.Thermostat, DataTemp, CardGradientOrange)
            )

            measureTypes.forEach { (name, desc, icon, color, gradient) ->
                MeasureTypeCard(
                    name = name,
                    description = desc,
                    icon = icon,
                    color = color,
                    gradient = gradient
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))
            }
        }

        // 4. 数据来源
        item {
            Text(
                text = "数据来源",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))

            val sources = listOf(
                Triple("PPG 相机测量", "使用手机摄像头测量", Icons.Default.CameraAlt, CardGradientRed),
                Triple("手动记录", "手动输入测量数据", Icons.Default.Edit, CardGradientOrange),
                Triple("蓝牙设备", "连接蓝牙健康设备", Icons.Default.MonitorHeart, CardGradientBlue),
                Triple("Health Connect", "从系统健康服务同步", Icons.Default.Sync, CardGradientGreen)
            )

            sources.forEach { (name, desc, icon, gradient) ->
                SourceCard(
                    name = name,
                    description = desc,
                    icon = icon,
                    gradient = gradient
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))
            }
        }

        // 5. 测量提示
        item {
            GradientBanner(
                title = "测量小贴士",
                subtitle = "测量前保持安静 5 分钟，坐姿端正，手臂与心脏同高",
                gradientColors = CardGradientPurple,
                icon = Icons.Default.HealthAndSafety
            )
        }
    }
}

/**
 * 测量类型卡片组件。
 */
@Composable
private fun MeasureTypeCard(
    name: String,
    description: String,
    icon: ImageVector,
    color: Color,
    gradient: List<Color>
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
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = { /* 开始测量 */ }) {
                Text("开始", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/**
 * 数据来源卡片组件。
 */
@Composable
private fun SourceCard(
    name: String,
    description: String,
    icon: ImageVector,
    gradient: List<Color>
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
        }
    }
}
