package com.silema.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silema.app.data.Contact
import com.silema.app.data.VitalRecord
import com.silema.app.engine.RiskEngine
import com.silema.app.store.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 主页 ViewModel。
 *
 * 从 [AppRepository] 获取体征记录和联系人，通过 [RiskEngine] 评估当前风险等级，
 * 暴露给 UI 层。UI 层只观察 StateFlow，不直接操作数据。
 *
 * v0.6.0 起通过构造函数注入 [AppRepository]，不再使用静态单例。
 */
@HiltViewModel
class DashboardViewModel
    @Inject
    constructor(
        private val repository: AppRepository,
    ) : ViewModel() {
        /**
         * 最新的体征记录列表（按时间倒序）。
         */
        val records: StateFlow<List<VitalRecord>> =
            repository.records
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        /**
         * 紧急联系人列表。
         */
        val contacts: StateFlow<List<Contact>> =
            repository.contacts
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        /**
         * 当前风险评估结果（基于最近一次各类型体征）。
         * UI 观察此值即可获取风险等级和告警列表。
         */
        val assessment =
            repository.records
                .map { records ->
                    if (records.isEmpty()) {
                        RiskEngine.evaluate(emptyList(), System.currentTimeMillis())
                    } else {
                        // 取每种类型的最新一条记录进行评估
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
         * 新增体征记录。
         */
        fun addRecord(record: VitalRecord) {
            repository.addRecord(record)
        }

        /**
         * 删除体征记录。
         */
        fun removeRecord(
            typeId: String,
            timestampMillis: Long,
        ) {
            repository.removeRecord(typeId, timestampMillis)
        }

        /**
         * 添加紧急联系人。
         */
        fun addContact(contact: Contact) {
            repository.addContact(contact)
        }

        /**
         * 删除紧急联系人。
         */
        fun removeContact(phone: String) {
            repository.removeContact(phone)
        }

        /**
         * 一键清空全部数据。
         */
        fun clearAll() {
            repository.clearAll()
        }
    }
