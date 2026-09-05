package com.silema.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.silema.app.data.Workout
import com.silema.app.ui.components.GlassCard
import com.silema.app.ui.theme.AppSpacing
import com.silema.app.ui.theme.BrandGreen
import com.silema.app.ui.theme.CardGradientGreen
import com.silema.app.ui.theme.CardGradientOrange
import com.silema.app.ui.theme.CardGradientPurple
import com.silema.app.ui.theme.DataHeart
import com.silema.app.ui.theme.DataSteps
import com.silema.app.ui.theme.LocalSilemaThemeColors
import kotlinx.coroutines.delay

@Composable
fun WorkoutScreenV3(
    workouts: List<Workout> = emptyList(),
    onStartWorkout: () -> Unit = {},
    onStopWorkout: () -> Unit = {},
) {
    val themeColors = LocalSilemaThemeColors.current
    var isRunning by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableLongStateOf(0L) }

    androidx.compose.runtime.LaunchedEffect(isRunning) {
        if (isRunning) {
            while (isRunning) {
                delay(1000L)
                elapsedMs += 1000L
            }
        }
    }

    val hours = (elapsedMs / 3600000).toInt()
    val minutes = ((elapsedMs % 3600000) / 60000).toInt()
    val seconds = ((elapsedMs % 60000) / 1000).toInt()
    val timeStr = "%02d:%02d:%02d".format(hours, minutes, seconds)

    val distanceKm = (elapsedMs / 3600000.0) * 5.0
    val calories = (elapsedMs / 60000.0) * 8.0
    val steps = (elapsedMs / 1000L) * 2

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
        item {
            Column {
                Text(
                    text = "运动追踪",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "记录运动数据，保持健康活力",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            GlassCard {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        WorkoutStatItem(
                            icon = Icons.Default.Map,
                            value = String.format("%.2f", distanceKm),
                            unit = "km",
                            color = DataSteps,
                        )
                        WorkoutStatItem(
                            icon = Icons.Default.LocalFireDepartment,
                            value = String.format("%.0f", calories),
                            unit = "kcal",
                            color = CardGradientOrange.first(),
                        )
                        WorkoutStatItem(
                            icon = Icons.Default.DirectionsWalk,
                            value = steps.toString(),
                            unit = "步",
                            color = CardGradientGreen.first(),
                        )
                        WorkoutStatItem(
                            icon = Icons.Default.Favorite,
                            value = "--",
                            unit = "bpm",
                            color = DataHeart,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(if (isRunning) Color(0xFFE53935) else BrandGreen)
                                    .then(
                                        Modifier.clickable {
                                            if (isRunning) {
                                                onStopWorkout()
                                            } else {
                                                onStartWorkout()
                                            }
                                            isRunning = !isRunning
                                        },
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isRunning) "停止" else "开始",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                }
            }
        }

        if (workouts.isNotEmpty()) {
            item {
                Text(
                    text = "最近运动",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))
            }

            items(workouts.take(5).size) { index ->
                val workout = workouts[index]
                GlassCard {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = workout.type,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${workout.distanceMeters.toInt()}m · ${workout.caloriesKcal.toInt()} kcal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "${workout.durationMillis / 60000}分钟",
                            style = MaterialTheme.typography.labelMedium,
                            color = BrandGreen,
                        )
                    }
                }
            }
        }

        item {
            GradientBanner(
                title = "运动目标",
                subtitle = "每天运动 30 分钟，保持健康活力",
                gradientColors = CardGradientPurple,
                icon = Icons.Default.Speed,
            )
        }
    }
}

@Composable
private fun WorkoutStatItem(
    icon: ImageVector,
    value: String,
    unit: String,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 2.dp, bottom = 2.dp),
            )
        }
    }
}

@Composable
private fun GradientBanner(
    title: String,
    subtitle: String,
    gradientColors: List<Color>,
    icon: ImageVector,
) {
    GlassCard {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(gradientColors))
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
        }
    }
}
