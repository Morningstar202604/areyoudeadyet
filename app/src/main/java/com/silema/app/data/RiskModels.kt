package com.silema.app.data

enum class RiskLevel(val rank: Int, val label: String) {
    NORMAL(0, "正常"),
    WATCH(1, "注意"),
    WARNING(2, "警告"),
    CRITICAL(3, "危险");

    fun maxWith(other: RiskLevel): RiskLevel = if (other.rank > rank) other else this
}

data class AlertItem(
    val level: RiskLevel,
    val metric: String,
    val measured: String,
    val problem: String,
    val why: String,
    val action: String
)

data class Assessment(
    val level: RiskLevel,
    val alerts: List<AlertItem>,
    val evaluatedAtMillis: Long,
    val missingToday: List<String>
)
