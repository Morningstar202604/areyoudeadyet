package com.silema.app.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import com.silema.app.data.VitalType
import com.silema.app.wear.data.WearStore

/**
 * 手表端运动屏幕 V3。
 *
 * 展示运动数据（步数、心率、卡路里、时长），支持开始/暂停运动。
 */
@Composable
fun WearWorkoutScreenV3(
    onStart: () -> Unit = {},
    onPause: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    var isRunning by remember { mutableStateOf(false) }
    val records by WearStore.records.collectAsState(initial = emptyList())
    val steps = records
        .filter { it.typeId == VitalType.STEPS.id }
        .maxByOrNull { it.timestampMillis }?.value?.toInt() ?: 0
    val heartRate = records
        .filter { it.typeId == VitalType.HEART_RATE.id }
        .maxByOrNull { it.timestampMillis }?.value?.toInt() ?: 0

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1A237E), Color(0xFF283593), Color(0xFF3949AB)),
                    ),
                ),
        contentAlignment = Alignment.Center,
    ) {
        ScalingLazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 32.dp),
        ) {
            item {
                // 标题
                Text(
                    text = "运动监测",
                    style = MaterialTheme.typography.title2,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }

            item {
                // 运动状态
                Text(
                    text = if (isRunning) "运动中" else "准备开始",
                    style = MaterialTheme.typography.body2,
                    color = if (isRunning) Color(0xFF69F0AE) else Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium,
                )
            }

            item {
                // 数据网格 2x2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    WorkoutDataItem(
                        icon = Icons.Default.DirectionsWalk,
                        value = steps.toString(),
                        unit = "步",
                        label = "步数",
                        color = Color(0xFF69F0AE),
                        modifier = Modifier.weight(1f),
                    )
                    WorkoutDataItem(
                        icon = Icons.Default.Favorite,
                        value = heartRate.toString(),
                        unit = "bpm",
                        label = "心率",
                        color = Color(0xFFFF80AB),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    WorkoutDataItem(
                        icon = Icons.Default.LocalFireDepartment,
                        value = "45",
                        unit = "kcal",
                        label = "卡路里",
                        color = Color(0xFFFFAB40),
                        modifier = Modifier.weight(1f),
                    )
                    WorkoutDataItem(
                        icon = Icons.Default.Timer,
                        value = "12:30",
                        unit = "",
                        label = "时长",
                        color = Color(0xFF80D8FF),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                // 开始/暂停按钮
                Button(
                    onClick = {
                        isRunning = !isRunning
                        if (isRunning) onStart() else onPause()
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            backgroundColor = if (isRunning) Color(0xFFFF5252) else Color(0xFF69F0AE),
                            contentColor = Color(0xFF1A237E),
                        ),
                ) {
                    Text(
                        text = if (isRunning) "暂停" else "开始运动",
                        style = MaterialTheme.typography.button,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/**
 * 运动数据项组件。
 */
@Composable
private fun WorkoutDataItem(
    icon: ImageVector,
    value: String,
    unit: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .padding(8.dp),
        contentAlignment = Alignment.Center,
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
                    style = MaterialTheme.typography.body1,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                if (unit.isNotEmpty()) {
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.caption3,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.caption3,
                color = Color.White.copy(alpha = 0.5f),
            )
        }
    }
}
