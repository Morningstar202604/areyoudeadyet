package com.silema.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.silema.app.data.VitalRecord
import com.silema.app.ui.theme.SilemaTheme
import com.silema.app.util.TtsController

// 设计系统 v2 的预览入口：每个主屏在 SilemaTheme 下渲染，
// 便于在 Android Studio 中直接核对「温暖医疗专业风」的配色、对比度与间距令牌。
// 这些预览不参与 release 产物，仅开发期使用。

@Preview(name = "首页 Dashboard", showBackground = true)
@Composable
private fun PreviewDashboard() {
    SilemaTheme {
        DashboardScreen(
            records = emptyList(),
            tts = TtsController(LocalContext.current),
            onGoSos = {},
            onGoEntry = {},
            onGoDevices = {},
            onGoWorkout = {},
            onGoGuardian = {},
        )
    }
}

@Preview(name = "健康报告 Report", showBackground = true)
@Composable
private fun PreviewReport() {
    SilemaTheme { ReportScreen(emptyList()) }
}

@Preview(name = "家人监护 Family", showBackground = true)
@Composable
private fun PreviewFamily() {
    SilemaTheme { FamilyScreen() }
}

@Preview(name = "守护设置 Guardian", showBackground = true)
@Composable
private fun PreviewGuardian() {
    SilemaTheme { GuardianScreen(emptyList()) }
}

@Preview(name = "医疗对接 Medical", showBackground = true)
@Composable
private fun PreviewMedical() {
    SilemaTheme { MedicalScreen(emptyList()) }
}

@Preview(name = "设备 Devices", showBackground = true)
@Composable
private fun PreviewDevices() {
    SilemaTheme { DevicesScreen(onClose = {}) }
}

@Preview(name = "AI 分析 AiReport", showBackground = true)
@Composable
private fun PreviewAiReport() {
    SilemaTheme { AiReportScreen(emptyList<VitalRecord>()) }
}

