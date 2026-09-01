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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silema.app.ui.components.GlassCard
import com.silema.app.ui.components.GradientBanner
import com.silema.app.ui.components.GradientItem
import com.silema.app.ui.theme.AppSpacing
import com.silema.app.ui.theme.CardGradientBlue
import com.silema.app.ui.theme.CardGradientGreen
import com.silema.app.ui.theme.CardGradientOrange
import com.silema.app.ui.theme.CardGradientPurple
import com.silema.app.ui.theme.CardGradientRed

/**
 * 更多功能屏幕 V3 — 现代健康活力风。
 *
 * 展示设置、数据管理、关于等功能入口。
 */
@Composable
fun MoreScreenV3(onClose: () -> Unit = {}) {
    var darkMode by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf(true) }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFF1F8E9), Color(0xFFE8F5E9), Color(0xFFFFFFFF)),
                    ),
                ).padding(horizontal = AppSpacing.screenPad),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
        contentPadding =
            androidx.compose.foundation.layout
                .PaddingValues(top = AppSpacing.xxl, bottom = 100.dp),
    ) {
        // 1. 标题
        item {
            Text(
                text = "更多功能",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "设置、数据管理、关于应用",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 2. 数据管理
        item {
            Text(
                text = "数据管理",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))

            val dataItems =
                listOf(
                    GradientItem("导出数据", "导出健康数据为 JSON/PDF", Icons.Default.CloudDownload, CardGradientGreen),
                    GradientItem("导入数据", "从备份文件导入健康数据", Icons.Default.CloudUpload, CardGradientBlue),
                )

            dataItems.forEach { (name, desc, icon, gradient) ->
                SettingItem(
                    name = name,
                    description = desc,
                    icon = icon,
                    gradient = gradient,
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))
            }
        }

        // 3. 个性化设置
        item {
            Text(
                text = "个性化",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))

            GlassCard {
                Column {
                    SwitchSettingItem(
                        name = "深色模式",
                        description = "切换深色/浅色主题",
                        icon = Icons.Default.DarkMode,
                        gradient = CardGradientPurple,
                        checked = darkMode,
                        onCheckedChange = { darkMode = it },
                    )
                    SwitchSettingItem(
                        name = "消息通知",
                        description = "接收健康提醒和通知",
                        icon = Icons.Default.Notifications,
                        gradient = CardGradientOrange,
                        checked = notifications,
                        onCheckedChange = { notifications = it },
                    )
                    SettingItem(
                        name = "主题配色",
                        description = "选择喜欢的主题配色方案",
                        icon = Icons.Default.Palette,
                        gradient = CardGradientGreen,
                    )
                    SettingItem(
                        name = "语言设置",
                        description = "选择应用显示语言",
                        icon = Icons.Default.Language,
                        gradient = CardGradientBlue,
                    )
                }
            }
        }

        // 4. 安全与隐私
        item {
            Text(
                text = "安全与隐私",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))

            val securityItems =
                listOf(
                    GradientItem("隐私设置", "管理数据收集和使用权限", Icons.Default.Security, CardGradientRed),
                    GradientItem("应用锁", "设置应用启动密码保护", Icons.Default.Lock, CardGradientOrange),
                )

            securityItems.forEach { (name, desc, icon, gradient) ->
                SettingItem(
                    name = name,
                    description = desc,
                    icon = icon,
                    gradient = gradient,
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))
            }
        }

        // 5. 关于
        item {
            Text(
                text = "关于",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))

            val aboutItems =
                listOf(
                    GradientItem("关于应用", "Silema · Are You Dead Yet? v3.0", Icons.Default.Info, CardGradientBlue),
                    GradientItem("给我们评分", "在应用商店评价我们", Icons.Default.Star, CardGradientOrange),
                )

            aboutItems.forEach { (name, desc, icon, gradient) ->
                SettingItem(
                    name = name,
                    description = desc,
                    icon = icon,
                    gradient = gradient,
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))
            }
        }

        // 6. 退出登录
        item {
            GradientBanner(
                title = "退出登录",
                subtitle = "退出当前账号，清除本地缓存",
                gradientColors = listOf(Color(0xFFE53935), Color(0xFFB71C1C)),
                icon = Icons.Default.ExitToApp,
            )
        }
    }
}

/**
 * 设置项组件。
 */
@Composable
private fun SettingItem(
    name: String,
    description: String,
    icon: ImageVector,
    gradient: List<Color>,
) {
    GlassCard {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(gradient)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * 带开关的设置项组件。
 */
@Composable
private fun SwitchSettingItem(
    name: String,
    description: String,
    icon: ImageVector,
    gradient: List<Color>,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(gradient)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
