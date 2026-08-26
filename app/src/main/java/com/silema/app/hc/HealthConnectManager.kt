package com.silema.app.hc

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalSource
import com.silema.app.data.VitalType
import java.time.Duration
import java.time.Instant
import kotlin.reflect.KClass

/**
 * Health Connect 接入层：读取华为运动健康 / 小米运动健康等
 * 写入 Health Connect 的心率、血氧、血压、步数数据。
 * Android 14+ 系统内置 Health Connect；低版本需安装 Health Connect 应用。
 */
object HealthConnectManager {

    val READ_PERMISSIONS: Set<String> = setOf(
        HeartRateRecord::class,
        OxygenSaturationRecord::class,
        BloodPressureRecord::class,
        StepsRecord::class
    ).map { HealthPermission.getReadPermission(it) }.toSet()

    fun isAvailable(context: Context): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    fun unavailableReason(context: Context): String = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> "可用"
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
            "需要更新或安装 Health Connect 应用"
        else -> "此设备不支持 Health Connect"
    }

    suspend fun grantedPermissions(context: Context): Set<String> {
        if (!isAvailable(context)) return emptySet()
        return HealthConnectClient.getOrCreate(context)
            .permissionController.getGrantedPermissions()
    }

    /** 拉取最近 24 小时的数据并转换为本地记录。 */
    suspend fun pullLast24h(context: Context): List<VitalRecord> {
        if (!isAvailable(context)) return emptyList()
        val client = HealthConnectClient.getOrCreate(context)
        val now = Instant.now()
        val range = TimeRangeFilter.between(now.minus(Duration.ofHours(24)), now)
        val out = mutableListOf<VitalRecord>()

        client.readRecords(
            ReadRecordsRequest<HeartRateRecord>(HeartRateRecord::class, range)
        ).records.forEach { rec ->
            rec.samples.maxByOrNull { it.time }?.let { sample ->
                out += VitalRecord.of(
                    VitalType.HEART_RATE, sample.beatsPerMinute.toDouble(),
                    sample.time.toEpochMilli(), VitalSource.HEALTH_CONNECT
                )
            }
        }

        client.readRecords(
            ReadRecordsRequest<OxygenSaturationRecord>(OxygenSaturationRecord::class, range)
        ).records.forEach { rec ->
            out += VitalRecord.of(
                VitalType.SPO2, rec.percentage.value,
                rec.time.toEpochMilli(), VitalSource.HEALTH_CONNECT
            )
        }

        client.readRecords(
            ReadRecordsRequest<BloodPressureRecord>(BloodPressureRecord::class, range)
        ).records.forEach { rec ->
            val t = rec.time.toEpochMilli()
            out += VitalRecord.of(
                VitalType.SYSTOLIC, rec.systolic.inMillimetersOfMercury,
                t, VitalSource.HEALTH_CONNECT
            )
            out += VitalRecord.of(
                VitalType.DIASTOLIC, rec.diastolic.inMillimetersOfMercury,
                t, VitalSource.HEALTH_CONNECT
            )
        }

        val stepsTotal = client.readRecords(
            ReadRecordsRequest<StepsRecord>(StepsRecord::class, range)
        ).records.sumOf { it.count }
        if (stepsTotal > 0) {
            out += VitalRecord.of(
                VitalType.STEPS, stepsTotal.toDouble(), now.toEpochMilli(),
                VitalSource.HEALTH_CONNECT
            )
        }

        return out
    }
}
