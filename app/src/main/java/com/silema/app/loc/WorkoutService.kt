package com.silema.app.loc

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.silema.app.R
import com.silema.app.data.Workout
import com.silema.app.engine.Stats
import com.silema.app.store.AppRepository
import com.silema.app.store.appRepositoryFrom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * 运动记录前台服务（location 类型）：GPS 轨迹采集 → 距离/配速/卡路里实时计算，
 * 停止时落库。使用框架 LocationManager，无 Google Play Services 依赖。
 *
 * 卡路里经验公式：步行 kcal ≈ 体重kg × 公里 × 0.53；跑步 ≈ ×1.02。
 */
class WorkoutService : android.app.Service() {

    /** AppRepository 单例（v0.6.0 起通过 Hilt EntryPoint 获取）。 */
    private lateinit var repo: AppRepository

    data class Live(
        val type: String,
        val elapsedSec: Long,
        val distanceM: Double,
        val points: Int,
        val kcal: Double,
        val speedKmh: Double
    )

    companion object {
        const val ACTION_START = "com.silema.app.workout.START"
        const val ACTION_STOP = "com.silema.app.workout.STOP"
        const val CHANNEL_WORKOUT = "workout"
        const val NOTIF_ID = 3001

        private val _live = MutableStateFlow<Live?>(null)
        val live: StateFlow<Live?> = _live.asStateFlow()

        fun start(context: Context, type: String) {
            val intent = Intent(context, WorkoutService::class.java)
                .setAction(ACTION_START).putExtra("type", type)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, WorkoutService::class.java).setAction(ACTION_STOP))
        }
    }

    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private var type = "walk"
    private var startMillis = 0L
    private var distance = 0.0
    private var lastFix: Location? = null
    private var lastStored: Location? = null
    private val track = mutableListOf<List<Double>>()
    private var ticker: Handler? = null

    private val locationListener = LocationListener { loc ->
        if (!loc.hasAccuracy() || loc.accuracy > 25f) return@LocationListener
        val prev = lastFix
        if (prev == null) {
            appendTrack(loc)
        } else {
            val d = Stats.haversineMeters(prev.latitude, prev.longitude, loc.latitude, loc.longitude)
            if (d in 0.5..200.0) distance += d
            if (distanceFrom(lastStored, loc) >= 5.0) appendTrack(loc)
        }
        lastFix = loc
    }

    private fun distanceFrom(stored: Location?, loc: Location): Double =
        stored?.let { Stats.haversineMeters(it.latitude, it.longitude, loc.latitude, loc.longitude) } ?: Double.MAX_VALUE

    private fun appendTrack(loc: Location) {
        if (track.size < 4000) {
            track.add(listOf(loc.latitude, loc.longitude, System.currentTimeMillis().toDouble()))
        }
        lastStored = loc
    }

    override fun onBind(intent: Intent?): android.os.IBinder? = null

    override fun onCreate() {
        super.onCreate()
        repo = appRepositoryFrom(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_WORKOUT, "运动进行中", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
        }
    }
    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> {
                type = intent.getStringExtra("type") ?: "walk"
                startMillis = System.currentTimeMillis()
                distance = 0.0
                lastFix = null; lastStored = null
                track.clear()
                startAsForeground()

                handlerThread = HandlerThread("gps").apply { start() }
                handler = Handler(handlerThread!!.looper)
                val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
                if (hasFine && lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0.5f, locationListener, handler!!.looper)
                    tick()
                } else {
                    _live.value = Live(type, 0, 0.0, 0, 0.0, 0.0)
                    updateNotification("等待 GPS 信号…请到空旷处")
                }
            }
            ACTION_STOP -> finishWorkout()
        }
        return START_NOT_STICKY
    }

    private fun tick() {
        ticker = Handler(Looper.getMainLooper())
        val updateRunnable = object : Runnable {
            override fun run() {
                val elapsed = (System.currentTimeMillis() - startMillis) / 1000
                val km = distance / 1000.0
                val hours = elapsed / 3600.0
                val speed = if (hours > 0) km / hours else 0.0
                val kcal = km * repo.weightKg * (if (type == "run") 1.02 else 0.53)
                _live.value = Live(type, elapsed, distance, track.size, kcal, speed)
                updateNotification("${"%.2f".format(km)} km · ${elapsed / 60} 分钟")
                if (_live.value != null) ticker?.postDelayed(this, 3000)
            }
        }
        ticker?.post(updateRunnable)
    }

    private fun buildNotification(text: String) = NotificationCompat.Builder(this, CHANNEL_WORKOUT)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(if (type == "run") "跑步进行中" else "步行进行中")
        .setContentText(text)
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0, Intent(this, com.silema.app.MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .addAction(
            0, "结束运动",
            PendingIntent.getService(
                this, 1, Intent(this, WorkoutService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()

    private fun startAsForeground() {
        val notif = buildNotification("等待 GPS 数据…")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(this, NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            ServiceCompat.startForeground(this, NOTIF_ID, notif, 0)
        }
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        runCatching { nm?.notify(NOTIF_ID, buildNotification(text)) }
    }

    private fun finishWorkout() {
        runCatching { (getSystemService(Context.LOCATION_SERVICE) as LocationManager).removeUpdates(locationListener) }
        ticker?.removeCallbacksAndMessages(null)
        val elapsed = System.currentTimeMillis() - startMillis
        val km = distance / 1000.0
        val kcal = km * repo.weightKg * (if (type == "run") 1.02 else 0.53)
        if (distance >= 20.0 && elapsed > 60_000) {
            val w = Workout(
                id = UUID.randomUUID().toString(),
                type = type,
                startMillis = startMillis,
                durationMillis = elapsed,
                distanceMeters = distance,
                caloriesKcal = Math.round(kcal * 10) / 10.0,
                track = ArrayList(track)
            )
            Thread { repo.addWorkout(w) }.start()
        }
        _live.value = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        runCatching { (getSystemService(Context.LOCATION_SERVICE) as LocationManager).removeUpdates(locationListener) }
        handlerThread?.quitSafely()
        super.onDestroy()
    }
}
