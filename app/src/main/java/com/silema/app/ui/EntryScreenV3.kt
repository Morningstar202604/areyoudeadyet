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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Thermostat
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silema.app.R
import com.silema.app.ui.components.GlassCard
import com.silema.app.ui.components.GradientBanner
import com.silema.app.ui.components.GradientItem
import com.silema.app.ui.components.MeasureTypeItem
import com.silema.app.ui.theme.AppSpacing
import com.silema.app.ui.theme.DataHeart
import com.silema.app.ui.theme.DataOxygen
import com.silema.app.ui.theme.DataPressure
import com.silema.app.ui.theme.DataTemp
import com.silema.app.ui.theme.LocalSilemaThemeColors
import com.silema.app.ui.theme.cardGradientBlue
import com.silema.app.ui.theme.cardGradientGreen
import com.silema.app.ui.theme.cardGradientOrange
import com.silema.app.ui.theme.cardGradientPurple
import com.silema.app.ui.theme.cardGradientRed

/**
 * 测量入口屏幕 V3 — 现代健康活力风。
 *
 * 展示各种测量方式入口：PPG 相机测量、手动记录、蓝牙设备、Health Connect。
 */
@Composable
fun EntryScreenV3(onClose: () -> Unit = {}) {
    val themeColors = LocalSilemaThemeColors.current
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
            Text(
                text = stringResource(R.string.entry_title_measure),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.entry_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 2. PPG 相机测量（主推）
        item {
            GradientBanner(
                title = stringResource(R.string.entry_ppg_banner_title),
                subtitle = stringResource(R.string.entry_ppg_banner_subtitle),
                gradientColors = cardGradientRed(),
                icon = Icons.Default.CameraAlt,
            )
        }

        // 3. 测量类型选择
        item {
            Text(
                text = stringResource(R.string.entry_measure_type),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))

            val measureTypes =
                listOf(
                    MeasureTypeItem(
                        stringResource(R.string.entry_type_hr_name),
                        stringResource(R.string.entry_type_hr_desc),
                        Icons.Default.Favorite,
                        DataHeart,
                        cardGradientRed(),
                    ),
                    MeasureTypeItem(
                        stringResource(R.string.entry_type_bp_name),
                        stringResource(R.string.entry_type_bp_desc),
                        Icons.Default.MonitorHeart,
                        DataPressure,
                        cardGradientBlue(),
                    ),
                    MeasureTypeItem(
                        stringResource(R.string.entry_type_spo2_name),
                        stringResource(R.string.entry_type_spo2_desc),
                        Icons.Default.HealthAndSafety,
                        DataOxygen,
                        cardGradientGreen(),
                    ),
                    MeasureTypeItem(
                        stringResource(R.string.entry_type_temp_name),
                        stringResource(R.string.entry_type_temp_desc),
                        Icons.Default.Thermostat,
                        DataTemp,
                        cardGradientOrange(),
                    ),
                )

            measureTypes.forEach { (name, desc, icon, color, gradient) ->
                MeasureTypeCard(
                    name = name,
                    description = desc,
                    icon = icon,
                    color = color,
                    gradient = gradient,
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))
            }
        }

        // 4. 数据来源
        item {
            Text(
                text = stringResource(R.string.entry_data_source),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))

            val sources =
                listOf(
                    GradientItem(
                        stringResource(R.string.entry_source_ppg),
                        stringResource(R.string.entry_source_ppg_desc),
                        Icons.Default.CameraAlt,
                        cardGradientRed(),
                    ),
                    GradientItem(
                        stringResource(R.string.entry_source_manual),
                        stringResource(R.string.entry_source_manual_desc),
                        Icons.Default.Edit,
                        cardGradientOrange(),
                    ),
                    GradientItem(
                        stringResource(R.string.entry_source_bluetooth),
                        stringResource(R.string.entry_source_bluetooth_desc),
                        Icons.Default.MonitorHeart,
                        cardGradientBlue(),
                    ),
                    GradientItem(
                        stringResource(R.string.entry_source_health_connect),
                        stringResource(R.string.entry_source_health_connect_desc),
                        Icons.Default.Sync,
                        cardGradientGreen(),
                    ),
                )

            sources.forEach { (name, desc, icon, gradient) ->
                SourceCard(
                    name = name,
                    description = desc,
                    icon = icon,
                    gradient = gradient,
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))
            }
        }

        // 5. 测量提示
        item {
            GradientBanner(
                title = stringResource(R.string.entry_tips_banner),
                subtitle = stringResource(R.string.entry_tips_banner_desc),
                gradientColors = cardGradientPurple(),
                icon = Icons.Default.HealthAndSafety,
            )
        }
    }
}

/**
 * 测量类型卡片组件。
 */
@Composable
private fun MeasureTypeCard(
    name: String,
    description: String,
    icon: ImageVector,
    color: Color,
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
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(gradient)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = { /* 开始测量 */ }) {
                Text(stringResource(R.string.entry_button_start), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/**
 * 数据来源卡片组件。
 */
@Composable
private fun SourceCard(
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
