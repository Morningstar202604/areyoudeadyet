package com.silema.app.wear.util

import android.content.Context
import com.silema.app.data.VitalType
import com.silema.app.wear.R

/**
 * 体征类型本地化工具：将 VitalType 的 displayName 和 unit 映射到字符串资源
 */
object VitalFormatter {
    fun getDisplayName(
        context: Context,
        typeId: String,
    ): String =
        when (typeId) {
            VitalType.HEART_RATE.id -> context.getString(R.string.entry_heart_rate)
            VitalType.SYSTOLIC.id, VitalType.DIASTOLIC.id -> context.getString(R.string.entry_blood_pressure)
            VitalType.SPO2.id -> context.getString(R.string.entry_oxygen)
            VitalType.TEMPERATURE.id -> context.getString(R.string.entry_temperature)
            VitalType.STEPS.id -> context.getString(R.string.entry_steps)
            else -> typeId
        }

    fun getUnit(
        context: Context,
        typeId: String,
    ): String =
        when (typeId) {
            VitalType.HEART_RATE.id -> context.getString(R.string.unit_bpm)
            VitalType.SYSTOLIC.id, VitalType.DIASTOLIC.id -> context.getString(R.string.unit_mmhg)
            VitalType.SPO2.id -> context.getString(R.string.unit_percent)
            VitalType.TEMPERATURE.id -> context.getString(R.string.unit_celsius)
            else -> ""
        }
}
