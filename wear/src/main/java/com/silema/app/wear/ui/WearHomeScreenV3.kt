package com.silema.app.wear.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Sos
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.silema.app.data.RiskLevel
import com.silema.app.data.VitalType
import com.silema.app.engine.RiskEngine
import com.silema.app.wear.data.WearStore
import kotlinx.coroutines.flow.collectAsState

/**
 * 手表端首页 v3 — 现代健康活力风（适配圆形/方形手表，包括华为手表）。
 *
 * 设计原则：
 * - 大字号、高对比，适合手表小屏幕和老人使用
 * - 核心数据优先展示（心率/风险等级）
 * - SOS 按钮醒目，一键呼救
 * - 适配圆形屏幕（内容居中，避免边缘裁剪）
 * - 支持华为手表 HarmonyOS / Wear OS 双系统
 *
 * 华为手表适配：
 * - 支持圆形屏幕（Huawei Watch GT 系列）和方形屏幕（Watch D 等）
 * - 使用 Wear OS Compose 组件，兼容华为 Wear OS 手表
 * - HarmonyOS 版本需单独开发（ArkTS），本版本为 Wear OS 版本
 */
@Composable
fun WearHomeScreenV3(
    onGoSos: () -> Unit = {},
    onGoMeasure: () -> Unit = {},
    onGoWorkout: () -> Unit = {},
) {
    val records by WearStore.records.collectAsState(initial = emptyList())
    val assessment = remember(records) { RiskEngine.evaluate(records) }
    val latest =
        remember(records) {
            records.groupBy { it.typeId }.mapValues { (_, list) -> list.maxByOrNull { it.timestampMillis } }
        }

    val heartRate = latest[VitalType.HEART_RATE.id]?.value?.toInt() ?: 72
    val riskColor =
        when (assessment.level) {
            RiskLevel.NORMAL -> Color(0xFF00A86B)
            RiskLevel.WATCH -> Color(0xFF00ACC1)
            RiskLevel.WARNING -> Color(0xFFFF8F00)
            RiskLevel.CRITICAL -> Color(0xFFE53935)
        }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1A1A2E),
                            Color(0xFF16213E),
                            Color(0xFF0F3460),
                        ),
                    ),
                ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // 1. 风险等级指示（顶部小条）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(riskColor),
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = assessment.level.label,
                    style = MaterialTheme.typography.caption2,
                    color = riskColor,
                    fontWeight = FontWeight.Bold,
                )
            }

            // 2. 心率大数字（核心展示）
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFE91E63),
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = heartRate.toString(),
                    style = MaterialTheme.typography.display1,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "次/分",
                    style = MaterialTheme.typography.caption2,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }

            // 3. 血氧/步数 小数据行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MiniDataItem(
                    icon = Icons.Default.HealthAndSafety,
                    value = latest[VitalType.SPO2.id]?.value?.toInt()?.toString() ?: "--",
                    unit = "%",
                    label = "血氧",
                    color = Color(0xFF00BCD4),
                )
                MiniDataItem(
                    icon = Icons.Default.MonitorHeart,
                    value = latest[VitalType.STEPS.id]?.value?.toInt()?.toString() ?: "0",
                    unit = "步",
                    label = "步数",
                    color = Color(0xFF8BC34A),
                )
            }

            // 4. SOS 大按钮
            Card(
                onClick = onGoSos,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                background =
                    Brush.horizontalGradient(
                        listOf(Color(0xFFE53935), Color(0xFFB71C1C)),
                    ),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sos,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "紧急呼救",
                            style = MaterialTheme.typography.button,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 手表端迷你数据项组件。
 */
@Composable
private fun MiniDataItem(
    icon: ImageVector,
    value: String,
    unit: String,
    label: String,
    color: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp),
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.body2,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.caption3,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.caption3,
            color = Color.White.copy(alpha = 0.5f),
        )
    }
}

/**
 * 华为手表适配工具类。
 *
 * 华为手表目前主要有两种系统：
 * 1. Wear OS by Google（部分海外版，如 Watch GT 2 Pro 海外版）
 * 2. HarmonyOS（国内版，如 Watch GT 3/4 系列、Watch D 系列）
 *
 * 本应用为 Wear OS 版本，可在华为 Wear OS 手表上运行。
 * HarmonyOS 版本需使用 ArkTS/ArkUI 单独开发。
 *
 * 屏幕适配：
 * - 圆形屏幕（1.39 英寸，454x454）：Watch GT 系列
 * - 方形屏幕（1.64 英寸，280x456）：Watch Fit 系列
 * - 矩形屏幕（Watch D）：特殊尺寸
 *
 * 使用 Wear OS Compose 的 ScalingLazyColumn 可自动适配各种屏幕形状。
 */
object HuaweiWatchAdapter {
    // 华为手表常见屏幕尺寸（dp）
    const val WATCH_GT_DIAMETER = 390f // 1.39 英寸圆形
    const val WATCH_FIT_WIDTH = 280f // 1.64 英寸方形宽
    const val WATCH_FIT_HEIGHT = 456f // 1.64 英寸方形高

    // 判断是否为圆形屏幕（华为 GT 系列）
    fun isRoundScreen(
        screenWidthDp: Float,
        screenHeightDp: Float,
    ): Boolean = kotlin.math.abs(screenWidthDp - screenHeightDp) < 10f

    // 根据屏幕形状调整内容内边距
    fun getContentPadding(isRound: Boolean): androidx.compose.foundation.layout.PaddingValues =
        if (isRound) {
            androidx.compose.foundation.layout.PaddingValues(
                horizontal = 24.dp,
                vertical = 16.dp,
            )
        } else {
            androidx.compose.foundation.layout.PaddingValues(
                horizontal = 12.dp,
                vertical = 8.dp,
            )
        }

    // 华为手表 BLE 设备过滤（支持华为血压表、心率带等）
    val HUAWEI_BLE_DEVICES =
        listOf(
            "HUAWEI Watch D",
            "HUAWEI Band",
            "HUAWEI Scale",
            "Honor Band",
        )
}
