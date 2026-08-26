package com.silema.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.silema.app.data.Contact
import com.silema.app.data.VitalRecord
import com.silema.app.engine.RiskEngine
import com.silema.app.sos.Emergency

private val SosRed = Color(0xFF7F0000)

@Composable
fun SosScreen(records: List<VitalRecord>, contacts: List<Contact>, onClose: () -> Unit) {
    val context = LocalContext.current
    val assessment = remember(records) { RiskEngine.evaluate(records) }
    val primaryPhone = Emergency.primaryContactPhone(contacts)
    val summary = remember(records) { Emergency.statusSummary(records) }

    var pendingCallNumber by remember { mutableStateOf<String?>(null) }
    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val number = pendingCallNumber
        if (granted && number != null) {
            runCatching { context.startActivity(Emergency.callIntent(number)) }
        } else if (number != null) {
            context.startActivity(Emergency.dialIntent(number))
        }
        pendingCallNumber = null
    }

    fun callContact(number: String) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            runCatching { context.startActivity(Emergency.callIntent(number)) }
        } else {
            pendingCallNumber = number
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(SosRed)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "紧急呼救",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )
            Text(
                text = "当前评估：${assessment.level.label}" +
                    (assessment.alerts.firstOrNull()?.let { " ｜ ${it.metric} ${it.measured}" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFFCDD2),
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(Modifier.height(28.dp))

            // 1. 拨打 120 —— 最优先，永远可用
            SosBigButton(text = "拨打 120 急救电话", container = Color.White, contentColor = SosRed) {
                context.startActivity(Emergency.dialIntent(Emergency.MEDICAL_NUMBER))
            }
            Spacer(Modifier.height(14.dp))

            // 2. 打给紧急联系人
            if (primaryPhone != null) {
                val name = contacts.first().name
                SosBigButton(text = "打给家人：$name（$primaryPhone）", container = Color(0xFFD32F2F), contentColor = Color.White) {
                    callContact(primaryPhone)
                }
            } else {
                Text(
                    text = "还没有设置家人电话。到「守护」页添加后，这里可以一键拨打。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFFCDD2)
                )
            }
            Spacer(Modifier.height(14.dp))

            // 3. 发送体征短信给家人（打开短信应用，由老人确认发送，避免误发）
            if (primaryPhone != null) {
                SosBigButton(text = "把我的情况短信发给家人", container = Color(0xFFD32F2F), contentColor = Color.White) {
                    runCatching {
                        context.startActivity(Emergency.smsIntent(primaryPhone, summary))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "短信内容会自动带上最近的测量数据和风险等级。\n打电话时说清楚：你是谁、在哪里、发生了什么。",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFFCDD2)
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onClose,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().height(68.dp)
            ) {
                Text("取消，返回首页", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SosBigButton(
    text: String,
    container: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = contentColor),
            modifier = Modifier.fillMaxWidth().height(88.dp)
        ) {
            Text(text, style = MaterialTheme.typography.titleLarge)
        }
    }
}
