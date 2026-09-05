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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.silema.app.R
import com.silema.app.data.RiskLevel
import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalType
import com.silema.app.ui.components.DataTile
import com.silema.app.ui.components.GlassCard
import com.silema.app.ui.components.GradientBanner
import com.silema.app.ui.components.QuickActionButton
import com.silema.app.ui.theme.AppShapes
import com.silema.app.ui.theme.AppSpacing
import com.silema.app.ui.theme.BrandBlue
import com.silema.app.ui.theme.BrandGreen
import com.silema.app.ui.theme.BrandPurple
import com.silema.app.ui.theme.BrandWarm
import com.silema.app.ui.theme.DataHeart
import com.silema.app.ui.theme.DataOxygen
import com.silema.app.ui.theme.DataPressure
import com.silema.app.ui.theme.DataSleep
import com.silema.app.ui.theme.DataSteps
import com.silema.app.ui.theme.LocalSilemaThemeColors
import com.silema.app.ui.theme.cardGradientGreen
import com.silema.app.ui.theme.cardGradientOrange
import com.silema.app.ui.theme.cardGradientRed
import com.silema.app.ui.theme.riskColor
import com.silema.app.ui.theme.sosGradient
import com.silema.app.util.TtsController
import com.silema.app.vm.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreenV3(
    records: List<VitalRecord>,
    tts: TtsController,
    viewModel: DashboardViewModel,
    onGoSos: () -> Unit,
    onGoEntry: () -> Unit,
    onGoDevices: () -> Unit,
    onGoWorkout: () -> Unit,
    onGoGuardian: () -> Unit,
    onGoFamily: () -> Unit = {},
    onGoAi: () -> Unit = {},
    onGoMedical: () -> Unit = {},
) {
    val themeColors = LocalSilemaThemeColors.current
    val assessment by viewModel.assessment.collectAsState()
    val latest =
        remember(records) {
            records.groupBy { it.typeId }.mapValues { (_, list) -> list.maxByOrNull { it.timestampMillis } }
        }

    val dateText = remember { SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(Date()) }

    val context = LocalContext.current
    val greeting =
        remember {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            when {
                hour < 6 -> context.getString(R.string.dashboard_greeting_late_night)
                hour < 9 -> context.getString(R.string.dashboard_greeting_morning)
                hour < 12 -> context.getString(R.string.dashboard_greeting_morning_early)
                hour < 14 -> context.getString(R.string.dashboard_greeting_noon)
                hour < 18 -> context.getString(R.string.dashboard_greeting_evening)
                else -> context.getString(R.string.dashboard_greeting_night)
            }
        }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(themeColors.backgroundGradient))
                .padding(horizontal = AppSpacing.screenPad),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                top = AppSpacing.xxl,
                bottom = 100.dp,
            ),
    ) {
        // 1. 顶部问候 + 日期
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(cardGradientGreen())),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = stringResource(R.string.dashboard_content_description_health_safety),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        // 2. 风险评估渐变横幅
        item {
            val riskGradient =
                when (assessment.level) {
                    RiskLevel.NORMAL -> cardGradientGreen()
                    RiskLevel.WATCH -> listOf(Color(0xFF00ACC1), Color(0xFF4DD0E1))
                    RiskLevel.WARNING -> cardGradientOrange()
                    RiskLevel.CRITICAL -> cardGradientRed()
                }

            GradientBanner(
                title = stringResource(R.string.dashboard_health_status, assessment.level.label),
                subtitle =
                    assessment.alerts.firstOrNull()?.problem
                        ?: stringResource(R.string.dashboard_all_normal_continue),
                gradientColors = riskGradient,
                icon = Icons.Default.MonitorHeart,
            )
        }

        // 3. 核心数据网格（2x3）
        item {
            Text(
                text = stringResource(R.string.dashboard_core_data),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))

            val dataTiles =
                listOf(
                    Triple(
                        Icons.Default.Favorite,
                        DataHeart,
                        Triple(
                            latest[VitalType.HEART_RATE.id]?.value?.toInt()?.toString() ?: "--",
                            stringResource(R.string.unit_bpm),
                            stringResource(R.string.dashboard_heart_rate),
                        ),
                    ),
                    Triple(
                        Icons.Default.MonitorHeart,
                        DataPressure,
                        Triple(
                            latest[VitalType.SYSTOLIC.id]?.value?.toInt()?.toString() ?: "--",
                            stringResource(R.string.unit_mmhg),
                            stringResource(R.string.dashboard_blood_pressure),
                        ),
                    ),
                    Triple(
                        Icons.Default.HealthAndSafety,
                        DataOxygen,
                        Triple(
                            latest[VitalType.SPO2.id]?.value?.toInt()?.toString() ?: "--",
                            stringResource(R.string.unit_percent),
                            stringResource(R.string.dashboard_blood_oxygen),
                        ),
                    ),
                    Triple(
                        Icons.Default.DirectionsWalk,
                        DataSteps,
                        Triple(
                            latest[VitalType.STEPS.id]?.value?.toInt()?.toString() ?: "0",
                            stringResource(R.string.dashboard_unit_steps),
                            stringResource(R.string.dashboard_steps),
                        ),
                    ),
                    Triple(
                        Icons.Default.NightsStay,
                        DataSleep,
                        Triple(
                            latest[VitalType.SLEEP.id]?.value?.toInt()?.toString() ?: "--",
                            stringResource(R.string.dashboard_unit_hours),
                            stringResource(R.string.dashboard_sleep),
                        ),
                    ),
                    Triple(
                        Icons.Default.LocalFireDepartment,
                        BrandWarm,
                        Triple(
                            latest[VitalType.TEMPERATURE.id]?.value?.toString() ?: "--",
                            "℃",
                            stringResource(R.string.dashboard_temperature),
                        ),
                    ),
                )

            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                dataTiles.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    ) {
                        row.forEach { (icon, color, data) ->
                            DataTile(
                                icon = icon,
                                iconTint = color,
                                value = data.first,
                                unit = data.second,
                                label = data.third,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 4. 快速操作区
        item {
            Text(
                text = stringResource(R.string.dashboard_quick_actions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))

            GlassCard {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(AppSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        QuickActionButton(
                            stringResource(R.string.action_measure_hr),
                            Icons.Default.Favorite,
                            DataHeart,
                            onGoEntry,
                        )
                        QuickActionButton(
                            stringResource(R.string.action_devices),
                            Icons.Default.MonitorHeart,
                            BrandBlue,
                            onGoDevices,
                        )
                        QuickActionButton(
                            stringResource(R.string.action_workout),
                            Icons.Default.DirectionsWalk,
                            DataSteps,
                            onGoWorkout,
                        )
                        QuickActionButton(
                            stringResource(R.string.action_family),
                            Icons.Default.Phone,
                            BrandPurple,
                            onGoFamily,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        QuickActionButton(
                            stringResource(R.string.action_report),
                            Icons.Default.DateRange,
                            BrandGreen,
                            onGoMedical,
                        )
                        QuickActionButton(
                            stringResource(R.string.action_ai_analysis),
                            Icons.Default.TrendingUp,
                            BrandPurple,
                            onGoAi,
                        )
                        QuickActionButton(
                            stringResource(R.string.action_guardian),
                            Icons.Default.HealthAndSafety,
                            BrandBlue,
                            onGoGuardian,
                        )
                        QuickActionButton("SOS", Icons.Default.Sos, Color(0xFFE53935), onGoSos)
                    }
                }
            }
        }

        // 5. SOS 大按钮
        item {
            GradientBanner(
                title = stringResource(R.string.dashboard_emergency_sos),
                subtitle = stringResource(R.string.dashboard_emergency_subtitle_full),
                gradientColors = sosGradient(),
                icon = Icons.Default.Sos,
                onClick = onGoSos,
            )
        }

        // 6. 最近记录或空状态
        if (records.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.dashboard_recent_records),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))

                val recentRecords =
                    records
                        .sortedByDescending { it.timestampMillis }
                        .take(5)

                GlassCard {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(AppSpacing.md),
                    ) {
                        recentRecords.forEach { record ->
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = AppSpacing.sm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(36.dp)
                                                .clip(AppShapes.small)
                                                .background(riskColor(assessment.level).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = riskColor(assessment.level),
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = record.type?.displayName ?: record.typeId,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Text(
                                            text =
                                                SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
                                                    .format(Date(record.timestampMillis)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Text(
                                    text = "${record.value.toInt()} ${record.type?.unit ?: ""}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // 空状态
            item {
                GlassCard {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(AppSpacing.xxl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonitorHeart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp),
                        )
                        Text(
                            text = stringResource(R.string.dashboard_no_data),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.dashboard_no_data_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
