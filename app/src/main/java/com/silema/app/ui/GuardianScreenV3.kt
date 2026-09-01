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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silema.app.data.Contact
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
import com.silema.app.ui.theme.CardGradientRed
import com.silema.app.ui.theme.SosGradient

/**
 * 守护屏幕 V3 — 现代健康活力风。
 *
 * 展示紧急联系人、守护功能开关、SOS 设置。
 */
@Composable
fun GuardianScreenV3(
    contacts: List<Contact>,
    onAddContact: () -> Unit = {},
    onClose: () -> Unit = {}
) {
    val repository = rememberAppRepository()
    val savedContacts by repository.contacts.collectAsState(initial = emptyList())
    val allContacts = contacts.ifEmpty { savedContacts }

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
                text = "守护设置",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "设置紧急联系人和守护功能，保障老人安全",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 2. SOS 紧急呼救设置
        item {
            GradientBanner(
                title = "SOS 紧急呼救",
                subtitle = "一键呼叫紧急联系人，同时发送位置信息",
                gradientColors = SosGradient,
                icon = Icons.Default.Phone
            )
        }

        // 3. 守护功能开关
        item {
            Text(
                text = "守护功能",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))

            val features = listOf(
                Triple("异常心率提醒", "心率异常时自动通知家人", Icons.Default.Notifications, BrandWarm),
                Triple("位置共享", "实时共享位置给紧急联系人", Icons.Default.LocationOn, BrandBlue),
                Triple("跌倒检测", "检测到跌倒时自动呼救", Icons.Default.HealthAndSafety, BrandPurple),
                Triple("用药提醒", "定时提醒老人服药", Icons.Default.Notifications, BrandGreen)
            )

            features.forEach { (name, desc, icon, color) ->
                FeatureCard(
                    name = name,
                    description = desc,
                    icon = icon,
                    color = color,
                    isEnabled = true
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))
            }
        }

        // 4. 紧急联系人列表
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "紧急联系人",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                OutlinedButton(onClick = onAddContact) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("添加", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 4.dp))
                }
            }
            Spacer(modifier = Modifier.height(AppSpacing.sm))

            if (allContacts.isEmpty()) {
                GlassCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "暂无紧急联系人",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "点击右上角添加紧急联系人",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                allContacts.forEach { contact ->
                    ContactCard(contact = contact)
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                }
            }
        }

        // 5. 通知方式
        item {
            Text(
                text = "通知方式",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))

            val notifyMethods = listOf(
                Triple("电话通知", "紧急情况直接拨打电话", Icons.Default.Phone, CardGradientRed),
                Triple("短信通知", "发送短信通知紧急联系人", Icons.Default.Sms, CardGradientBlue),
                Triple("APP 推送", "通过 APP 推送通知", Icons.Default.Notifications, CardGradientGreen)
            )

            notifyMethods.forEach { (name, desc, icon, gradient) ->
                NotifyMethodCard(
                    name = name,
                    description = desc,
                    icon = icon,
                    gradient = gradient,
                    isEnabled = true
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))
            }
        }
    }
}

/**
 * 功能卡片组件。
 */
@Composable
private fun FeatureCard(
    name: String,
    description: String,
    icon: ImageVector,
    color: Color,
    isEnabled: Boolean
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
            androidx.compose.material3.Switch(
                checked = isEnabled,
                onCheckedChange = { /* 切换开关 */ }
            )
        }
    }
}

/**
 * 联系人卡片组件。
 */
@Composable
private fun ContactCard(contact: Contact) {
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
                    .background(Brush.horizontalGradient(CardGradientBlue)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.first().toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = contact.relation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = { /* 拨打电话 */ }) {
                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/**
 * 通知方式卡片组件。
 */
@Composable
private fun NotifyMethodCard(
    name: String,
    description: String,
    icon: ImageVector,
    gradient: List<Color>,
    isEnabled: Boolean
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
            androidx.compose.material3.Switch(
                checked = isEnabled,
                onCheckedChange = { /* 切换开关 */ }
            )
        }
    }
}
