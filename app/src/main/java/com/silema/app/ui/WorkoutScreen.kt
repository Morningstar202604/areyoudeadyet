package com.silema.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.silema.app.loc.WorkoutService
import com.silema.app.store.AppRepository
import com.silema.app.ui.components.BigButton
import com.silema.app.ui.components.SectionTitle
import com.silema.app.ui.theme.BrandRed
import java.time.Instant
import java.time.ZoneId

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("运动", style = MaterialTheme.typography.headlineSmall)
        Text(
            "GPS 记录步行/跑步：距离、配速、卡路里与轨迹。卡路里为经验公式估算（步行 0.53×体重×公里，跑步 1.02）。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        val current = live
        if (current == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigButton(
                    text = "开始步行",
                    container = MaterialTheme.colorScheme.secondary,
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
                    container = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (granted) WorkoutService.start(context, "run") else {
                            pendingStartType = "run"
                            permLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                        }
                    }
                )
            }
        } else {
            LivePanel(current)
            Spacer(Modifier.height(12.dp))
            BigButton(
                text = "结束运动并保存",
                container = BrandRed,
                onClick = { WorkoutService.stop(context) }
            )
        }

        SectionTitle("历史记录（${workouts.size}）")
        if (workouts.isEmpty()) {
            EmptyHint(text = "还没有运动记录。出门走一圈试试，需要授予定位权限。")
        } else {
            workouts.take(20).forEach { w ->
                val zdt = Instant.ofEpochMilli(w.startMillis).atZone(ZoneId.systemDefault())
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Text(
                        "%02d-%02d %02d:%02d · %s · %.2f km · %d 分钟 · %s 千卡".format(
                            zdt.monthValue, zdt.dayOfMonth, zdt.hour, zdt.minute,
                            if (w.type == "run") "跑步" else "步行",
                            w.distanceKm, w.durationMillis / 60000,
                            String.format("%.1f", w.caloriesKcal)
                        ),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "配速 %.1f 分钟/公里 · 轨迹 %d 点".format(w.paceMinPerKm, w.track.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (w.track.size > 2) TrackCanvas(w.track)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LivePanel(live: WorkoutService.Live) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = "%.2f".format(live.distanceM / 1000.0),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text("公里", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            LiveStat("${live.elapsedSec / 60}", "分钟")
            LiveStat("%.1f".format(live.speedKmh), "公里/时")
            LiveStat("%.0f".format(live.kcal), "千卡")
            LiveStat("${live.points}", "轨迹点")
        }
    }
}

@Composable
private fun LiveStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** 轨迹缩略图：经纬度归一化后连线（无底图，离线渲染）。 */
@Composable
fun TrackCanvas(track: List<List<Double>>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(top = 4.dp)
    ) {
        if (track.size < 2) return@Canvas
        val lats = track.map { it[0] }
        val lons = track.map { it[1] }
        val minLat = lats.min(); val maxLat = lats.max()
        val minLon = lons.min(); val maxLon = lons.max()
        val spanLat = (maxLat - minLat).takeIf { it > 1e-6 } ?: 1.0
        val spanLon = (maxLon - minLon).takeIf { it > 1e-6 } ?: 1.0
        val pad = 24f
        val w = size.width - pad * 2
        val h = size.height - pad * 2
        val path = Path()
        track.forEachIndexed { i, p ->
            val x = (pad + w * ((p[1] - minLon) / spanLon)).toFloat()
            val y = (pad + h * (1.0 - (p[0] - minLat) / spanLat)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, Color(0xFFB71C1C), style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val last = track.last()
        drawCircle(
            Color(0xFF2E7D32), radius = 10f,
            center = Offset(
                (pad + w * ((last[1] - minLon) / spanLon)).toFloat(),
                (pad + h * (1.0 - (last[0] - minLat) / spanLat)).toFloat()
            )
        )
    }
}
