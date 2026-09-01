package com.silema.app.wear.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalType
import com.silema.app.data.Workout
import com.silema.app.engine.RiskEngine
import com.silema.app.wear.data.WearStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 手表端主页 ViewModel。
 *
 * 展示当前体征数据、风险等级、最近运动记录。
 * 手表端是主产品（给老人佩戴），UI 简洁，数据实时更新。
 */
@HiltViewModel
class WearHomeViewModel
    @Inject
    constructor() : ViewModel() {
        /**
         * 体征记录列表（按时间倒序）。
         */
        val records: StateFlow<List<VitalRecord>> =
            WearStore.records
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        /**
         * 运动记录列表。
         */
        val workouts: StateFlow<List<Workout>> =
            WearStore.workouts
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        /**
         * 当前风险评估结果（基于最近一次各类型体征）。
         */
        val assessment =
            WearStore.records
                .map { records ->
                    if (records.isEmpty()) {
                        RiskEngine.evaluate(emptyList(), System.currentTimeMillis())
                    } else {
                        val latestByType =
                            records
                                .groupBy { it.typeId }
                                .mapValues { it.value.maxByOrNull { r -> r.timestampMillis } }
                                .values
                                .filterNotNull()
                        RiskEngine.evaluate(latestByType, System.currentTimeMillis())
                    }
                }.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    RiskEngine.evaluate(emptyList(), System.currentTimeMillis()),
                )

        /**
         * 当前心率（最新一条）。
         */
        val currentHeartRate: StateFlow<Double?> =
            WearStore.records
                .map { records ->
                    records
                        .filter { it.typeId == VitalType.HEART_RATE.id }
                        .maxByOrNull { it.timestampMillis }
                        ?.value
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        /**
         * 当前血氧（最新一条）。
         */
        val currentSpo2: StateFlow<Double?> =
            WearStore.records
                .map { records ->
                    records
                        .filter { it.typeId == VitalType.SPO2.id }
                        .maxByOrNull { it.timestampMillis }
                        ?.value
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        /**
         * 添加体征记录（BLE 设备测量后保存）。
         */
        fun addRecord(record: VitalRecord) {
            WearStore.addRecord(record)
        }

        /**
         * 添加运动记录。
         */
        fun addWorkout(workout: Workout) {
            WearStore.addWorkout(workout)
        }

        /**
         * 获取指定类型的最新一条记录。
         */
        fun latestOfType(typeId: String): VitalRecord? =
            records.value
                .filter { it.typeId == typeId }
                .maxByOrNull { it.timestampMillis }
    }
