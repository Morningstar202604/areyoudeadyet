package com.silema.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.silema.app.data.Workout
import com.silema.app.loc.WorkoutService
import com.silema.app.store.AppRepository
import com.silema.app.ui.components.BigButton
import com.silema.app.ui.components.EmptyState
import com.silema.app.ui.components.GradientCard
import com.silema.app.ui.components.SectionTitle
import com.silema.app.ui.components.StatTile
import com.silema.app.ui.theme.BrandBlue
import com.silema.app.ui.theme.BrandGreen
import com.silema.app.ui.theme.BrandWarm
import com.silema.app.ui.theme.CardGradientBlue
import com.silema.app.ui.theme.CardGradientGreen
import com.silema.app.ui.theme.CardGradientOrange
import com.silema.app.ui.theme.CardGradientPurple
import com.silema.app.ui.theme.LevelCritical
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun WorkoutScreen() {
    val context = LocalContext.current
    val live by WorkoutService.live.collectAsState()
    val workouts by AppRepository.workouts.collectAsState()

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var pendingStartType by remember { mutableStateOf<String?>(null) }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted && pendingStartType != null) {
            WorkoutService.start(context, pendingStartType!!)
            pendingStartType = null
        }
    }

    val isRunning = live != null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("运动", style = MaterialTheme.typography.headlineSmall)
            Text(
                "GPS 记录步行/跑步：距离、配速、卡路里与轨迹",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        item {
            GradientCard(
                gradientColors = if (isRunning) CardGradientGreen else CardGradientBlue
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isRunning) {
                        ActiveWorkoutPanel(live!!)
                    } else {
                        IdleWorkoutPanel()
                    }
                }
            }
        }

        if (!isRunning) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BigButton(
                        text = "开始步行",
                        container = BrandBlue,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (granted) WorkoutService.start(context, "walk") else {
                                pendingStartType = "walk"
                                permLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                            }
                        }
                    )
                    BigButton(
                        text = "开始跑步",
                        container = BrandWarm,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (granted) WorkoutService.start(context, "run") else {
                                pendingStartType = "run"
                                permLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                            }
                        }
                    )
                }
            }
        } else {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                BigButton(
                    text = "结束运动并保存",
                    container = LevelCritical,
                    onClick = { WorkoutService.stop(context) }
                )
            }
        }

        item {
            SectionTitle("历史记录（${workouts.size}）")
        }

        if (workouts.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.LocationOn,
                    title = "还没有运动记录",
                    message = "出门走一圈试试，需要授予定位权限"
                )
            }
        } else {
            items(workouts.take(20)) { workout ->
                WorkoutHistoryCard(workout)
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun IdleWorkoutPanel() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "GPS 运动追踪",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "选择步行或跑步开始记录轨迹、距离与卡路里",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ActiveWorkoutPanel(live: WorkoutService.Live) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("mm:ss") }

    Text(
        text = "%.2f".format(live.distanceM / 1000.0),
        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
        color = Color.White
    )
    Text(
        text = "公里",
        style = MaterialTheme.typography.titleMedium,
        color = Color.White.copy(alpha = 0.85f)
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = if (live.type == "run") "跑步中…" else "步行中…",
        style = MaterialTheme.typography.labelLarge,
        color = Color.White.copy(alpha = 0.7f)
    )
    Spacer(modifier = Modifier.height(16.dp))

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        LiveStatItem(
            value = formatElapsed(live.elapsedSec),
            label = "时长",
            color = Color.White
        )
        LiveStatItem(
            value = "%.1f".format(live.speedKmh),
            label = "速度 km/h",
            color = Color.White
        )
        LiveStatItem(
            value = "%.0f".format(live.kcal),
            label = "千卡",
            color = Color.White
        )
        LiveStatItem(
            value = "${live.points}",
            label = "轨迹点",
            color = Color.White
        )
    }
}

@Composable
private fun LiveStatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color.copy(alpha = 0.75f)
        )
    }
}

@Composable
private fun WorkoutHistoryCard(workout: Workout) {
    val zdt = Instant.ofEpochMilli(workout.startMillis).atZone(ZoneId.systemDefault())
    val dateStr = zdt.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
    val typeLabel = if (workout.type == "run") "跑步" else "步行"
    val durationMin = workout.durationMillis / 60000

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (workout.type == "run") BrandWarm.copy(alpha = 0.12f)
                            else BrandBlue.copy(alpha = 0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (workout.type == "run") Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (workout.type == "run") BrandWarm else BrandBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$typeLabel · $dateStr",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "轨迹 ${workout.track.size} 点",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile(
                    label = "距离",
                    value = "%.2f km".format(workout.distanceKm),
                    valueColor = BrandGreen,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "时长",
                    value = "$durationMin 分",
                    valueColor = BrandBlue,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "配速",
                    value = if (workout.paceMinPerKm > 0) "%.1f 分/km".format(workout.paceMinPerKm) else "--",
                    valueColor = BrandWarm,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "千卡",
                    value = "%.0f".format(workout.caloriesKcal),
                    valueColor = Color(0xFFE57373),
                    modifier = Modifier.weight(1f)
                )
            }

            if (workout.track.size > 2) {
                Spacer(modifier = Modifier.height(8.dp))
                TrackCanvas(workout.track)
            }
        }
    }
}

private fun formatElapsed(elapsedSec: Long): String {
    val h = elapsedSec / 3600
    val m = (elapsedSec % 3600) / 60
    val s = elapsedSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@Composable
private fun TrackCanvas(track: List<List<Double>>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        if (track.size < 2) return@Canvas
        val lats = track.map { it[0] }
        val lons = track.map { it[1] }
        val minLat = lats.min(); val maxLat = lats.max()
        val minLon = lons.min(); val maxLon = lons.max()
        val spanLat = (maxLat - minLat).takeIf { it > 1e-6 } ?: 1.0
        val spanLon = (maxLon - minLon).takeIf { it > 1e-6 } ?: 1.0
        val pad = 16f
        val w = size.width - pad * 2
        val h = size.height - pad * 2
        val path = Path()
        track.forEachIndexed { i, p ->
            val x = (pad + w * ((p[1] - minLon) / spanLon)).toFloat()
            val y = (pad + h * (1.0 - (p[0] - minLat) / spanLat)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path, Color(0xFFB71C1C),
            style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        val last = track.last()
        drawCircle(
            Color(0xFF2E7D32), radius = 8f,
            center = Offset(
                (pad + w * ((last[1] - minLon) / spanLon)).toFloat(),
                (pad + h * (1.0 - (last[0] - minLat) / spanLat)).toFloat()
            )
        )
    }
}


