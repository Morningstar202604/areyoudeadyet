package com.silema.app.engine

/**
 * 基础统计原语 —— 全部为可直接单测的纯函数。
 */
object Stats {

    /** 算术平均。 */
    fun mean(values: List<Double>): Double {
        if (values.isEmpty()) throw IllegalArgumentException("mean of empty list")
        var sum = 0.0
        for (v in values) sum += v
        return sum / values.size
    }

    /** 总体标准差。 */
    fun std(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mu = mean(values)
        var ss = 0.0
        for (v in values) {
            val d = v - mu
            ss += d * d
        }
        return kotlin.math.sqrt(ss / values.size)
    }

    /**
     * 最小二乘法线性回归斜率：slope = Σ(x-x̄)(y-ȳ) / Σ(x-x̄)²
     * 用于体征趋势分析（如"收缩压每天上升多少 mmHg"）。
     */
    fun leastSquaresSlope(xs: List<Double>, ys: List<Double>): Double {
        require(xs.size == ys.size && xs.size >= 2) { "leastSquares needs equal non-trivial lists" }
        val mx = mean(xs)
        val my = mean(ys)
        var num = 0.0
        var den = 0.0
        for (i in xs.indices) {
            val dx = xs[i] - mx
            num += dx * (ys[i] - my)
            den += dx * dx
        }
        return if (den == 0.0) 0.0 else num / den
    }

    /** z 分数：(x - μ) / σ。用于判断某次读数是否显著偏离个人基线。 */
    fun zScore(value: Double, mu: Double, sigma: Double): Double =
        if (sigma == 0.0) 0.0 else (value - mu) / sigma

    /** 中位数。 */
    fun median(values: List<Double>): Double {
        require(values.isNotEmpty()) { "median of empty list" }
        val s = values.sorted()
        val n = s.size
        return if (n % 2 == 1) s[n / 2] else (s[n / 2 - 1] + s[n / 2]) / 2.0
    }

    /**
     * Haversine 球面距离（米）—— 运动轨迹两点间距。
     * 地球半径取 6371008.8m（IUGG 平均半径）。
     */
    fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371008.8
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        return 2 * r * kotlin.math.asin(kotlin.math.min(1.0, kotlin.math.sqrt(a)))
    }
}

/**
 * 血流动力学衍生指标 —— 公式来自公开生理学定义。
 */
object VitalsMath {

    /** 平均动脉压 MAP = DBP + (SBP - DBP) / 3，正常范围约 70~100 mmHg，<65 提示器官灌注不足。 */
    fun meanArterialPressure(systolic: Double, diastolic: Double): Double =
        diastolic + (systolic - diastolic) / 3.0

    /** 脉压差 PP = SBP - DBP，正常约 30~60 mmHg。 */
    fun pulsePressure(systolic: Double, diastolic: Double): Double = systolic - diastolic

    /** 休克指数 SI = 心率 / 收缩压，正常约 0.5~0.7，>0.9 提示失代偿风险。 */
    fun shockIndex(heartRate: Double, systolic: Double): Double =
        if (systolic == 0.0) 0.0 else heartRate / systolic
}
