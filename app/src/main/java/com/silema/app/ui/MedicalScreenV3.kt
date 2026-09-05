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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
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
import com.silema.app.ui.components.GradientItem
import com.silema.app.ui.components.MedicationItem
import com.silema.app.ui.theme.AppSpacing
import com.silema.app.ui.theme.BrandGreen
import com.silema.app.ui.theme.LocalSilemaThemeColors
import com.silema.app.ui.theme.cardGradientBlue
import com.silema.app.ui.theme.cardGradientGreen
import com.silema.app.ui.theme.cardGradientOrange
import com.silema.app.ui.theme.cardGradientPurple

/**
 * 医疗/用药屏幕 V3 — 现代健康活力风。
 *
 * 展示用药提醒、就诊记录、医疗档案、健康知识。
 */
@Composable
fun MedicalScreenV3(onClose: () -> Unit = {}) {
    val themeColors = LocalSilemaThemeColors.current
    // 示例用药数据
    val medications =
        listOf(
            MedicationItem("阿司匹林", "100mg", "每天早上 8:00", true),
            MedicationItem("降压药", "5mg", "每天晚上 20:00", false),
            MedicationItem("钙片", "500mg", "每天中午 12:00", true),
        )

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(themeColors.backgroundGradient),
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
                        text = "医疗健康",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "用药提醒、就诊记录、健康知识",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // 2. 今日用药概览
        item {
            GlassCard {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "今日用药",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "2/3 已服用",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BrandGreen,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    // 进度条
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0E0E0)),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth(0.67f)
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(Brush.horizontalGradient(cardGradientGreen())),
                        )
                    }
                }
            }
        }

        // 3. 用药提醒列表
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "用药提醒",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                OutlinedButton(onClick = { /* 添加用药 */ }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("添加", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 4.dp))
                }
            }
            Spacer(modifier = Modifier.height(AppSpacing.sm))
        }

        items(medications) { (name, dosage, time, taken) ->
            MedicationCard(
                name = name,
                dosage = dosage,
                time = time,
                isTaken = taken,
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))
        }

        // 4. 快捷功能
        item {
            Text(
                text = "快捷功能",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))

            val features =
                listOf(
                    GradientItem("就诊记录", "查看历史就诊记录", Icons.Default.LocalHospital, cardGradientBlue()),
                    GradientItem("医疗档案", "管理个人医疗档案", Icons.Default.MedicalServices, cardGradientPurple()),
                    GradientItem("健康知识", "学习健康养生知识", Icons.Default.LocalPharmacy, cardGradientGreen()),
                    GradientItem("复诊提醒", "设置复诊提醒", Icons.Default.Schedule, cardGradientOrange()),
                )

            features.forEach { (name, desc, icon, gradient) ->
                FeatureCard(
                    name = name,
                    description = desc,
                    icon = icon,
                    gradient = gradient,
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))
            }
        }

        // 5. 用药安全提示
        item {
            GradientBanner(
                title = "用药安全提示",
                subtitle = "请遵医嘱按时服药，如有不适请及时就医",
                gradientColors = cardGradientOrange(),
                icon = Icons.Default.Notifications,
            )
        }
    }
}

/**
 * 用药卡片组件。
 */
@Composable
private fun MedicationCard(
    name: String,
    dosage: String,
    time: String,
    isTaken: Boolean,
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
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (isTaken) {
                                Brush.horizontalGradient(cardGradientGreen())
                            } else {
                                Brush.horizontalGradient(cardGradientOrange())
                            },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.LocalPharmacy,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "$dosage · $time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isTaken) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "已服用",
                        style = MaterialTheme.typography.labelMedium,
                        color = BrandGreen,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            } else {
                OutlinedButton(onClick = { /* 标记已服用 */ }) {
                    Text("服用", style = MaterialTheme.typography.labelMedium)
                }
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
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(gradient)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
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
