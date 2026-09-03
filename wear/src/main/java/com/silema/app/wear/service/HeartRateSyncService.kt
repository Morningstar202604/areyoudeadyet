package com.silema.app.wear.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.silema.app.wear.ble.BleVitals
import com.silema.app.wear.datalayer.WearDataLayerClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 心率同步后台服务
 * 持续监听心率传感器数据并推送到手机
 */
class HeartRateSyncService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var syncJob: Job? = null

    private lateinit var dataLayerClient: WearDataLayerClient
    private lateinit var bleVitals: BleVitals

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): HeartRateSyncService = this@HeartRateSyncService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        dataLayerClient = WearDataLayerClient(applicationContext)
        bleVitals = BleVitals(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startHeartRateSync()
        return START_STICKY
    }

    /**
     * 开始心率同步
     */
    fun startHeartRateSync() {
        syncJob?.cancel()
        syncJob = scope.launch {
            try {
                // 启动 BLE 心率监测
                bleVitals.startHeartRateMonitoring()

                // 收集心率数据并推送到手机
                bleVitals.heartRate.collectLatest { heartRate ->
                    if (heartRate > 0) {
                        dataLayerClient.sendHeartRate(heartRate)
                    }
                }
            } catch (e: Exception) {
                // 记录错误但不崩溃
            }
        }
    }

    /**
     * 停止心率同步
     */
    fun stopHeartRateSync() {
        syncJob?.cancel()
        bleVitals.stopMonitoring()
    }

    /**
     * 获取当前心率
     */
    fun getCurrentHeartRate(): Double {
        return bleVitals.getCurrentHeartRate()
    }

    /**
     * 检查同步状态
     */
    fun isSyncing(): Boolean {
        return syncJob?.isActive == true
    }

    override fun onDestroy() {
        super.onDestroy()
        stopHeartRateSync()
        scope.cancel()
    }

    companion object {
        fun start(context: android.content.Context) {
            val intent = Intent(context, HeartRateSyncService::class.java)
            context.startService(intent)
        }

        fun stop(context: android.content.Context) {
            val intent = Intent(context, HeartRateSyncService::class.java)
            context.stopService(intent)
        }
    }
}
