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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.VideoCall
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
import com.silema.app.ui.theme.BrandWarm
import com.silema.app.ui.theme.CardGradientBlue
import com.silema.app.ui.theme.CardGradientGreen
import com.silema.app.ui.theme.CardGradientOrange
import com.silema.app.ui.theme.CardGradientPurple

/**
 * 家人屏幕 V3 — 现代健康活力风。
 *
 * 展示家人列表、快捷联系入口、家庭健康动态。
 */
@Composable
fun FamilyScreenV3(onClose: () -> Unit = {}) {
    val repository = rememberAppRepository()
    val contacts by repository.contacts.collectAsState(initial = emptyList())

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "家人",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "联系家人，共享健康数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(onClick = { /* 添加家人 */ }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("添加", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }

        // 2. 快捷联系
        item {
            Text(
                text = "快捷联系",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))

            GlassCard {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    QuickContactItem(
                        icon = Icons.Default.Phone,
                        label = "语音通话",
                        color = BrandGreen,
                        gradient = CardGradientGreen,
                    )
                    QuickContactItem(
                        icon = Icons.Default.VideoCall,
                        label = "视频通话",
                        color = BrandBlue,
                        gradient = CardGradientBlue,
                    )
                    QuickContactItem(
                        icon = Icons.Default.Message,
                        label = "发送消息",
                        color = BrandWarm,
                        gradient = CardGradientOrange,
                    )
                }
            }
        }

        // 3. 家人列表
        item {
            Text(
                text = "家人列表",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))
        }

        if (contacts.isEmpty()) {
            item {
                GlassCard {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(56.dp),
                        )
                        Text(
                            text = "暂无家人",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "点击右上角添加家人，方便随时联系",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(contacts) { contact ->
                FamilyMemberCard(contact = contact)
                Spacer(modifier = Modifier.height(AppSpacing.sm))
            }
        }

        // 4. 家庭健康共享
        item {
            GradientBanner(
                title = "家庭健康共享",
                subtitle = "邀请家人加入，共享健康数据，一起守护老人健康",
                gradientColors = CardGradientPurple,
                icon = Icons.Default.Favorite,
            )
        }
    }
}

/**
 * 快捷联系项组件。
 */
@Composable
private fun QuickContactItem(
    icon: ImageVector,
    label: String,
    color: Color,
    gradient: List<Color>,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(gradient)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * 家人成员卡片组件。
 */
@Composable
private fun FamilyMemberCard(contact: Contact) {
    val gradients = listOf(CardGradientBlue, CardGradientGreen, CardGradientOrange, CardGradientPurple)
    val gradient = gradients[contact.name.length % gradients.size]

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
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(gradient)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = contact.name.first().toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = contact.relation.ifEmpty { "家人" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = contact.phone,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { /* 拨打电话 */ }) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                OutlinedButton(onClick = { /* 发送消息 */ }) {
                    Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
