package com.silema.app.data

import kotlinx.serialization.Serializable

enum class VitalType(
    val id: String,
    val displayName: String,
    val unit: String,
) {
    HEART_RATE("heart_rate", "心率", "次/分"),
    SYSTOLIC("systolic", "收缩压(高压)", "mmHg"),
    DIASTOLIC("diastolic", "舒张压(低压)", "mmHg"),
    SPO2("spo2", "血氧饱和度", "%"),
    TEMPERATURE("temperature", "体温", "℃"),
    STEPS("steps", "今日步数", "步"),
    SLEEP("sleep", "睡眠时长", "小时"),
    STRESS("stress", "压力指数", "分"),
    ;

    companion object {
        fun fromId(id: String): VitalType? = entries.firstOrNull { it.id == id }
    }
}

object VitalSource {
    const val MANUAL = "manual"
    const val HEALTH_CONNECT = "health_connect"
    const val DEMO = "demo"
    const val BLE = "ble"
    const val PPG_CAMERA = "ppg_camera"
}

@Serializable
data class VitalRecord(
    val typeId: String,
    val value: Double,
    val timestampMillis: Long,
    val source: String = VitalSource.MANUAL,
) {
    val type: VitalType? get() = VitalType.fromId(typeId)

    companion object {
        fun of(
            type: VitalType,
            value: Double,
            timestampMillis: Long,
            source: String,
        ): VitalRecord = VitalRecord(type.id, value, timestampMillis, source)
    }
}

@Serializable
data class Contact(
    val name: String,
    val phone: String,
    val relation: String = "",
)

/** 一次运动记录（跑步/步行），轨迹点为 [lat, lon, tMillis] 扁平列表。 */
@Serializable
data class Workout(
    val id: String,
    val type: String, // "walk" | "run"
    val startMillis: Long,
    val durationMillis: Long,
    val distanceMeters: Double,
    val caloriesKcal: Double,
    val track: List<List<Double>>, // lat, lon, t（节流后存储）
) {
    val distanceKm: Double get() = distanceMeters / 1000.0
    val avgSpeedKmh: Double
        get() = if (durationMillis <= 0) 0.0 else distanceKm / (durationMillis / 3600000.0)
    val paceMinPerKm: Double
        get() = if (distanceMeters < 10) 0.0 else (durationMillis / 60000.0) / distanceKm
}
