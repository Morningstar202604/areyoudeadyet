package com.silema.app.ppg

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sqrt

/**
 * PPG（光电容积脉搏波）信号分析器 —— 纯算法，不依赖相机，
 * 输入为逐帧红色通道平均值序列，输出心率与 HRV(RMSSD) 估计。
 *
 * 处理链：
 *  1. 去趋势：减去约 1 秒宽的滑动均值（消除直流漂移/按压压力变化）
 *  2. 平滑：3 点滑动均值
 *  3. 自适应峰值检测：局部窗口均值 + 0.6×局部标准差 作为阈值，
 *     要求上升沿穿越 + 280ms 不应期（对应最高约 214 bpm）
 *  4. 心跳间期 IBI 中值过滤异常，HR = 60000 / median(IBI)
 *  5. RMSSD = sqrt(mean(diff(IBI)²))，即相邻心跳间期差的均方根（HRV 时域指标）
 */
class PpgAnalyzer {

    data class Result(
        val bpm: Double,
        val rmssdMs: Double,
        val beatCount: Int,
        val confidence: Double
    )

    private data class Sample(val tMs: Long, val value: Double)

    private val samples = ArrayList<Sample>(2048)

    /** 相机线程调用；与 analyze()（主线程）互斥，保护 samples。 */
    @Synchronized
    fun reset() = samples.clear()

    /** 逐帧喂入：时间戳(ms) 与该帧红色通道平均值。 */
    @Synchronized
    fun addSample(tMs: Long, value: Double) {
        samples.add(Sample(tMs, value))
        // 只保留最近 45 秒，防止长会话内存增长
        val cutoff = tMs - MAX_WINDOW_MS
        while (samples.isNotEmpty() && samples.first().tMs < cutoff) samples.removeAt(0)
    }

    /** 数据不足返回 null；信号质量太差也返回 null（诚实优于编造数字）。 */
    @Synchronized
    fun analyze(): Result? {
        if (samples.size < MIN_SAMPLES) return null
        val durationMs = samples.last().tMs - samples.first().tMs
        if (durationMs < MIN_DURATION_MS) return null

        // 1) 去趋势（滑动均值窗口 ≈ 1 秒）
        val spanMs = 1000.0
        val detrended = DoubleArray(samples.size)
        var left = 0
        var right = 0
        var sum = 0.0
        var count = 0
        for (i in samples.indices) {
            while (right < samples.size && samples[right].tMs - samples[i].tMs <= spanMs / 2) {
                sum += samples[right].value; count++; right++
            }
            while (left < i && samples[i].tMs - samples[left].tMs > spanMs / 2) {
                sum -= samples[left].value; count--; left++
            }
            detrended[i] = samples[i].value - sum / count
        }

        // 2) 三点平滑
        val smooth = DoubleArray(samples.size)
        for (i in smooth.indices) {
            smooth[i] = when (i) {
                0 -> detrended[0]
                samples.size - 1 -> detrended.last()
                else -> (detrended[i - 1] + detrended[i] + detrended[i + 1]) / 3.0
            }
        }

        // 3) 自适应峰值检测（局部 ±0.75s 窗口统计量）
        val winMs = 750.0
        val peaks = ArrayList<Long>()
        var lastPeakT = Long.MIN_VALUE
        for (i in 1 until samples.size - 1) {
            val t = samples[i].tMs
            if (lastPeakT != Long.MIN_VALUE && t - lastPeakT < REFRACTORY_MS) continue
            if (!(smooth[i] >= smooth[i - 1] && smooth[i] > smooth[i + 1])) continue

            var lo = i; var hi = i
            while (lo > 0 && t - samples[lo].tMs <= winMs) lo--
            while (hi < samples.size - 1 && samples[hi].tMs - t <= winMs) hi++
            var wSum = 0.0; var wSq = 0.0; var wN = 0
            for (j in lo..hi) {
                wSum += smooth[j]; wSq += smooth[j] * smooth[j]; wN++
            }
            val wMean = wSum / wN
            val wStd = sqrt(maxOf(0.0, wSq / wN - wMean * wMean))
            val threshold = wMean + 0.6 * wStd
            if (smooth[i] > threshold) {
                peaks.add(t)
                lastPeakT = t
            }
        }
        if (peaks.size < MIN_BEATS) return null

        // 4) IBI 过滤
        val ibis = ArrayList<Double>()
        for (i in 1 until peaks.size) {
            val ibi = (peaks[i] - peaks[i - 1]).toDouble()
            if (ibi in MIN_IBI_MS..MAX_IBI_MS) ibis.add(ibi)
        }
        if (ibis.size < MIN_BEATS - 1) return null
        var medianIbi = StatsBridge.median(ibis)
        val kept = ibis.filter { abs(it - medianIbi) <= medianIbi * 0.4 }
        val finalIbis = if (kept.size >= MIN_BEATS - 1) kept else ibis
        medianIbi = StatsBridge.median(finalIbis)

        // 5) HR 与 RMSSD
        val bpm = 60000.0 / medianIbi
        if (bpm !in 35.0..200.0) return null
        val diffs = ArrayList<Double>()
        for (i in 1 until finalIbis.size) diffs.add(finalIbis[i] - finalIbis[i - 1])
        val rmssd = if (diffs.isEmpty()) 0.0
        else sqrt(diffs.fold(0.0) { acc, d -> acc + d * d } / diffs.size)

        val plausibility = finalIbis.size.toDouble() / maxOf(1, ibis.size)
        val confidence = min(1.0, finalIbis.size / 20.0) *
            (if (plausibility >= 0.85) 1.0 else if (plausibility >= 0.6) 0.7 else 0.4)

        return Result(
            bpm = Math.round(bpm * 10) / 10.0,
            rmssdMs = Math.round(rmssd * 10) / 10.0,
            beatCount = finalIbis.size + 1,
            confidence = Math.round(confidence * 100) / 100.0
        )
    }

    companion object {
        private const val MAX_WINDOW_MS = 45_000L
        private const val MIN_SAMPLES = 240          // 约 8 秒 @30fps 的绝对下限
        private const val MIN_DURATION_MS = 18_000L  // 至少积累 18 秒信号
        private const val REFRACTORY_MS = 280L       // 不应期
        private const val MIN_IBI_MS = 300.0         // 200 bpm 上限
        private const val MAX_IBI_MS = 1800.0        // 33 bpm 下限
        private const val MIN_BEATS = 8
    }
}

/** 把中位数依赖转发到 engine.Stats，避免 ppg 包反向耦合过多。 */
internal object StatsBridge {
    fun median(values: List<Double>): Double = com.silema.app.engine.Stats.median(values)
}
