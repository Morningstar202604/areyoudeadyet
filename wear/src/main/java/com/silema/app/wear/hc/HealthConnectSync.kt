package com.silema.app.wear.hc

import android.content.Context
import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalSource
import com.silema.app.data.VitalType
import com.silema.app.wear.data.WearStore

/**
 * Wear OS Health Connect 同步（占位实现）
 * TODO: 添加 health-connect-client 依赖后实现完整功能
 */
class HealthConnectSync(private val context: Context) {

    suspend fun isAvailable(): Boolean = false

    suspend fun syncHeartRate() {
        // TODO: 实现心率同步
    }

    suspend fun syncSteps() {
        // TODO: 实现步数同步
    }

    suspend fun syncAll() {
        if (!isAvailable()) return
        syncHeartRate()
        syncSteps()
    }
}
