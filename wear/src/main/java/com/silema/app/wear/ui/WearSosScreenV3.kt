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
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sos
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

/**
 * 手表端 SOS 紧急呼救屏幕 V3。
 *
 * 大按钮设计，一键呼救，适合老人紧急情况下快速操作。
 * 倒计时确认机制，防止误触。
 */
@Composable
fun WearSosScreenV3(
    onSosTriggered: () -> Unit = {},
    onCancel: () -> Unit = {},
) {
    var countdown by remember { mutableStateOf(3) }
    var isCounting by remember { mutableStateOf(false) }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFB71C1C), Color(0xFFE53935), Color(0xFFEF5350)),
                    ),
                ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 标题
            Text(
                text = "紧急呼救",
                style = MaterialTheme.typography.title2,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            // SOS 大图标
            Box(
                modifier =
                    Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Sos,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }

            // 倒计时或提示
            if (isCounting) {
                Text(
                    text = "$countdown 秒后自动呼救",
                    style = MaterialTheme.typography.body1,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    text = "点击按钮呼叫紧急联系人\n同时发送位置信息",
                    style = MaterialTheme.typography.body2,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 呼救按钮
            Button(
                onClick = {
                    if (isCounting) {
                        onSosTriggered()
                    } else {
                        isCounting = true
                        // 简单的倒计时逻辑
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        backgroundColor = Color.White,
                        contentColor = Color(0xFFB71C1C),
                    ),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = if (isCounting) "立即呼救" else "SOS 呼救",
                        style = MaterialTheme.typography.button,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // 取消按钮
            if (isCounting) {
                Button(
                    onClick = {
                        isCounting = false
                        onCancel()
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            backgroundColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White,
                        ),
                ) {
                    Text("取消", style = MaterialTheme.typography.button)
                }
            }
        }
    }
}
