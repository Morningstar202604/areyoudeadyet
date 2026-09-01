package com.silema.app.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.silema.app.MainActivity
import com.silema.app.R
import com.silema.app.data.VitalType
import com.silema.app.engine.RiskEngine
import com.silema.app.store.AppRepository
import com.silema.app.store.appRepositoryFrom
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * 提醒中心：测量提醒（智能——当天已测齐则不打扰）+ 久坐提醒（9:00-21:00 每小时）。
 * 基于 WorkManager 周期任务，系统重启后自动恢复。
 */
object Reminders {

    /** 通过 Hilt EntryPoint 获取 AppRepository 单例（v0.6.0 起 AppRepository 改为 @Singleton 类）。 */
    private fun repo(context: Context): AppRepository = appRepositoryFrom(context)

    const val CHANNEL_REMINDERS = "reminders"
    private const val WORK_MEASURE = "rem_measure_daily"
    private const val WORK_SEDENTARY = "rem_sedentary_hourly"
    private const val NOTIF_ID_MEASURE = 2001
    private const val NOTIF_ID_SEDENTARY = 2002

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_REMINDERS, "健康提醒", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "测量提醒与久坐提醒" }
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(ch)
        }
    }

    /** 依据当前设置同步测量提醒任务。 */
    fun syncMeasurement(context: Context) {
        val wm = WorkManager.getInstance(context)
        if (repo(context).measureReminderOn) {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(repo(context).measureReminderHour, repo(context).measureReminderMinute)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val delay = Duration.between(now, next)
            val request = PeriodicWorkRequestBuilder<MeasureWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .addTag(WORK_MEASURE)
                .build()
            wm.enqueueUniquePeriodicWork(WORK_MEASURE, ExistingPeriodicWorkPolicy.UPDATE, request)
        } else {
            wm.cancelUniqueWork(WORK_MEASURE)
        }
    }

    fun syncSedentary(context: Context) {
        val wm = WorkManager.getInstance(context)
        if (repo(context).sedentaryReminderOn) {
            val request = PeriodicWorkRequestBuilder<SedentaryWorker>(1, TimeUnit.HOURS)
                .addTag(WORK_SEDENTARY)
                .build()
            wm.enqueueUniquePeriodicWork(WORK_SEDENTARY, ExistingPeriodicWorkPolicy.UPDATE, request)
        } else {
            wm.cancelUniqueWork(WORK_SEDENTARY)
        }
    }

    private fun canNotify(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 33) {
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun contentIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** 测量提醒 Worker：当天核心指标已测齐则保持安静，缺哪项提醒哪项。 */
    class MeasureWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val records = repo(context).records.first()
            val zone = ZoneId.systemDefault()
            val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
            val missing = mutableListOf<String>()
            val bp = listOf(VitalType.SYSTOLIC, VitalType.DIASTOLIC)
                .any { t -> records.any { it.typeId == t.id && it.timestampMillis >= todayStart } }
            if (!bp) missing.add("血压")
            if (records.none { it.typeId == VitalType.HEART_RATE.id && it.timestampMillis >= todayStart }) missing.add("心率")
            if (records.none { it.typeId == VitalType.SPO2.id && it.timestampMillis >= todayStart }) missing.add("血氧")

            if (missing.isNotEmpty() && canNotify(applicationContext)) {
                ensureChannel(applicationContext)
                val notif = NotificationCompat.Builder(applicationContext, CHANNEL_REMINDERS)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("该测量啦")
                    .setContentText("今天还没测：${missing.joinToString("、")}。数据越全，预警越准。")
                    .setAutoCancel(true)
                    .setContentIntent(contentIntent(applicationContext))
                    .build()
                NotificationManagerCompat.from(applicationContext).notify(NOTIF_ID_MEASURE, notif)
            }
            return Result.success()
        }
    }

    /** 久坐提醒 Worker：9:00-21:00 之间每小时轻提醒一次。 */
    class SedentaryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val hour = LocalTime.now().hour
            if (hour in 9..20 && canNotify(applicationContext)) {
                ensureChannel(applicationContext)
                val stepsToday = repo(context).records.first()
                    .filter { it.typeId == VitalType.STEPS.id && RiskEngine.clockText(it.timestampMillis).startsWith(
                        java.time.format.DateTimeFormatter.ofPattern("MM-dd").format(java.time.LocalDate.now())) }
                    .maxOfOrNull { it.value } ?: 0.0
                val notif = NotificationCompat.Builder(applicationContext, CHANNEL_REMINDERS)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("起来走两步吧")
                    .setContentText("已连续静坐约 1 小时（今日步数 ${stepsToday.toInt()}）。活动 3 分钟，血管谢谢你。")
                    .setAutoCancel(true)
                    .setContentIntent(contentIntent(applicationContext))
                    .build()
                NotificationManagerCompat.from(applicationContext).notify(NOTIF_ID_SEDENTARY, notif)
            }
            return Result.success()
        }
    }
}
