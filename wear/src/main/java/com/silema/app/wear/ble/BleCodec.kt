package com.silema.app.ble

import kotlin.math.pow

/**
 * 蓝牙标准协议解析器 —— 纯函数、零 Android 依赖，可直接在 JVM 上单测。
 */
object BleCodec {

    /** Heart Rate Measurement 0x2A37：flags 决定 uint8 / uint16 LE。 */
    fun parseHeartRate(b: ByteArray): Double? {
        if (b.size < 2) return null
        val flags = b[0].toInt() and 0xFF
        return if (flags and 0x01 != 0) {
            if (b.size < 3) null else u16Le(b, 1).toDouble()
        } else {
            (b[1].toInt() and 0xFF).toDouble()
        }
    }

    /** Blood Pressure Measurement 0x2A35：SYS/DIA/MAP 三个 SFLOAT 依次排列。 */
    fun parseBloodPressure(b: ByteArray): List<Double>? {
        if (b.size < 7) return null
        val sys = sfloat(b, 1) ?: return null
        val dia = sfloat(b, 3) ?: return null
        if (sys <= 0 || dia <= 0 || sys > 400 || dia > 300) return null
        return listOf(sys, dia)
    }

    /** PLX Continuous Measurement 0x2A5F：SpO₂ 与脉率两个 SFLOAT。 */
    fun parsePulseOx(b: ByteArray): Pair<Double, Double>? {
        if (b.size < 5) return null
        val spo2 = sfloat(b, 1) ?: return null
        val pr = sfloat(b, 3) ?: return null
        if (spo2 <= 0 || spo2 > 100 || pr <= 0 || pr > 300) return null
        return spo2 to pr
    }

    /**
     * IEEE-11073 16-bit SFLOAT：
     * 低 12 位为二进制补码尾数 m，高 4 位为二进制补码指数 e，
     * 实际值 = m × 10^e。保留特殊值（NaN/-∞/+∞/NRes）返回 null。
     */
    fun sfloat(b: ByteArray, offset: Int): Double? {
        if (b.size < offset + 2) return null
        val raw = u16Le(b, offset)
        var mantissa = raw and 0x0FFF
        var exponent = (raw shr 12) and 0x0F
        if (mantissa >= 0x0800) mantissa -= 0x1000
        if (exponent >= 0x08) exponent -= 0x10
        if (raw == 0x07FF || raw == 0x0800 || raw == 0x0FFE || raw == 0x0FFF) return null
        return mantissa.toDouble() * 10.0.pow(exponent.toDouble())
    }

    private fun u16Le(b: ByteArray, off: Int): Int =
        (b[off].toInt() and 0xFF) or ((b[off + 1].toInt() and 0xFF) shl 8)
}
