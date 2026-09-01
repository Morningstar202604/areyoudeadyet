package com.silema.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silema.app.data.VitalRecord
import com.silema.app.data.Workout
import com.silema.app.engine.HealthReport
import com.silema.app.store.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 健康报告 ViewModel。
 *
 * 生成周报/月报，包含体征趋势、运动统计、睡眠分析、风险评估。
 * 支持 PDF 导出和分享。
 *
 * v0.6.0 起通过构造函数注入 [AppRepository]。
 */
@HiltViewModel
class ReportViewModel
    @Inject
    constructor(
        private val repository: AppRepository,
    ) : ViewModel() {
        /**
         * 全部体征记录。
         */
        val records: StateFlow<List<VitalRecord>> =
            repository.records
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        /**
         * 全部运动记录。
         */
        val workouts: StateFlow<List<Workout>> =
            repository.workouts
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        /**
         * 最近一周的健康报告。
         */
        val weeklyReport =
            combine(records, workouts) { recs, works ->
                HealthReport.weekly(recs, works, System.currentTimeMillis(), 2)
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                HealthReport.weekly(emptyList(), emptyList(), System.currentTimeMillis(), 2),
            )

        /**
         * 生成指定周数的健康报告。
         *
         * @param weeksAgo 几周前（0 = 本周，1 = 上周）
         */
        fun generateWeeklyReport(weeksAgo: Int = 0): HealthReport.Weekly {
            val now = System.currentTimeMillis()
            val weekStart = now - weeksAgo * 7 * 24 * 60 * 60 * 1000L
            return HealthReport.weekly(records.value, workouts.value, weekStart, 2)
        }

        /**
         * 获取指定类型体征的周平均值。
         */
        fun weeklyAverage(typeId: String): Double =
            weeklyReport.value.metrics
                .firstOrNull { it.type.id == typeId }
                ?.thisWeekAvg ?: 0.0

        /**
         * 获取指定类型体征的周变化百分比。
         */
        fun weeklyChange(typeId: String): Double? =
            weeklyReport.value.metrics
                .firstOrNull { it.type.id == typeId }
                ?.deltaPct

        /**
         * 本周运动总次数。
         */
        fun weeklyWorkoutCount(): Int = weeklyReport.value.workoutCount

        /**
         * 本周运动总距离（公里）。
         */
        fun weeklyWorkoutDistance(): Double = weeklyReport.value.workoutKm

        /**
         * 本周平均睡眠时长（小时）。
         */
        fun weeklySleepAverage(): Double? = weeklyReport.value.sleepAvgHours

        /**
         * 健康报告摘要文本列表。
         */
        fun reportSummary(): List<String> = weeklyReport.value.summary
    }
