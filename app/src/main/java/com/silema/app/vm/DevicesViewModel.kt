package com.silema.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silema.app.data.VitalRecord
import com.silema.app.store.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 设备屏幕 ViewModel。
 *
 * 展示已连接的设备列表、设备状态、最近一次测量数据。
 * 支持 BLE 设备、PPG 相机测量、Health Connect 同步等数据源管理。
 *
 * v0.6.0 起通过构造函数注入 [AppRepository]。
 */
@HiltViewModel
class DevicesViewModel
    @Inject
    constructor(
        private val repository: AppRepository,
    ) : ViewModel() {
        /**
         * 最近的体征记录（用于展示设备最近一次测量结果）。
         */
        val recentRecords: StateFlow<List<VitalRecord>> =
            repository.records
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        /**
         * 获取指定类型的最近一条记录。
         */
        fun latestOfType(typeId: String): VitalRecord? =
            recentRecords.value
                .filter { it.typeId == typeId }
                .maxByOrNull { it.timestampMillis }

        /**
         * 手动添加一条体征记录（设备测量后保存）。
         */
        fun addRecord(record: VitalRecord) {
            repository.addRecord(record)
        }

        /**
         * 删除指定体征记录。
         */
        fun removeRecord(
            typeId: String,
            timestampMillis: Long,
        ) {
            repository.removeRecord(typeId, timestampMillis)
        }
    }
