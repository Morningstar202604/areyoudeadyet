package com.silema.app.data

import kotlinx.serialization.Serializable

enum class VitalType(val id: String, val displayName: String, val unit: String) {
    HEART_RATE("heart_rate", "心率", "次/分"),
    SYSTOLIC("systolic", "收缩压(高压)", "mmHg"),
    DIASTOLIC("diastolic", "舒张压(低压)", "mmHg"),
    SPO2("spo2", "血氧饱和度", "%"),
    TEMPERATURE("temperature", "体温", "℃"),
    STEPS("steps", "今日步数", "步");

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
    val source: String = VitalSource.MANUAL
) {
    val type: VitalType? get() = VitalType.fromId(typeId)

    companion object {
        fun of(type: VitalType, value: Double, timestampMillis: Long, source: String): VitalRecord =
            VitalRecord(type.id, value, timestampMillis, source)
    }
}

@Serializable
data class Contact(
    val name: String,
    val phone: String
)
