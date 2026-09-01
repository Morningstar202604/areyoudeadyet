package com.silema.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalType
import com.silema.app.data.Workout
import com.silema.app.store.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 医疗屏幕 ViewModel。
 *
 * 展示健康数据统计、历史趋势、运动记录、PDF 报告生成。
 * 支持按时间范围筛选数据，生成健康报告。
 */
@HiltViewModel
class MedicalViewModel @Inject constructor() : ViewModel() {

    /**
     * 全部体征记录。
     */
    val records: StateFlow<List<VitalRecord>> = AppRepository.records
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 全部运动记录。
     */
    val workouts: StateFlow<List<Workout>> = AppRepository.workouts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 最近 7 天的心率记录（用于趋势图）。
     */
    val recentHeartRate: StateFlow<List<VitalRecord>> = AppRepository.records
        .map { records ->
            val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
            records.filter {
                it.typeId == VitalType.HEART_RATE.id && it.timestampMillis >= sevenDaysAgo
            }.sortedBy { it.timestampMillis }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 最近 7 天的血压记录（用于趋势图）。
     */
    val recentBloodPressure: StateFlow<List<VitalRecord>> = AppRepository.records
        .map { records ->
            val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
            records.filter {
                (it.typeId == VitalType.SYSTOLIC.id || it.typeId == VitalType.DIASTOLIC.id) &&
                    it.timestampMillis >= sevenDaysAgo
            }.sortedBy { it.timestampMillis }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 计算指定类型体征的平均值。
     */
    fun averageOfType(typeId: String, sinceMillis: Long = 0): Double {
        val filtered = records.value.filter {
            it.typeId == typeId && it.timestampMillis >= sinceMillis
        }
        return if (filtered.isEmpty()) 0.0
        else filtered.sumOf { it.value } / filtered.size
    }

    /**
     * 获取指定类型体征的最新值。
     */
    fun latestOfType(typeId: String): VitalRecord? {
        return records.value
            .filter { it.typeId == typeId }
            .maxByOrNull { it.timestampMillis }
    }

    /**
     * 添加运动记录。
     */
    fun addWorkout(workout: Workout) {
        AppRepository.addWorkout(workout)
    }

    /**
     * 删除运动记录。
     */
    fun removeWorkout(id: String) {
        AppRepository.removeWorkout(id)
    }

    /**
     * 导出全部数据为 JSON（用于备份或分享）。
     */
    suspend fun exportData(): String? = AppRepository.exportToJson()
}
