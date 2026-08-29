package com.silema.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.silema.app.ui.components.ListItemCard
import com.silema.app.ui.components.SectionTitle

@Composable
fun MoreScreen(onNav: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "全部功能",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "监护、设备、报告、家人在底部导航；以下为其他功能入口",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item { SectionTitle("设备与运动") }
        item {
            ListItemCard(
                title = "检测中心",
                subtitle = "摄像头心率 / 蓝牙设备 / 穿戴同步",
                icon = Icons.Filled.Build,
                onClick = { onNav(Routes.DEVICES) }
            )
        }
        item { SectionTitle("健康服务") }
        item {
            ListItemCard(
                title = "AI 健康分析",
                subtitle = "智能风险评估与个性化建议",
                icon = Icons.Filled.AutoAwesome,
                onClick = { onNav(Routes.AI_REPORT) }
            )
        }
        item {
            ListItemCard(
                title = "医疗对接",
                subtitle = "FHIR R4 导出与健康报告",
                icon = Icons.Filled.MedicalServices,
                onClick = { onNav(Routes.MEDICAL) }
            )
        }
        item {
            ListItemCard(
                title = "设置",
                subtitle = "远程同步 / 紧急联系人 / 提醒",
                icon = Icons.Filled.Settings,
                onClick = { onNav(Routes.GUARDIAN) }
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
