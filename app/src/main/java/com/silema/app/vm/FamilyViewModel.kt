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
 * 家人屏幕 ViewModel。
 *
 * 展示紧急联系人列表、老人当前健康状态、风险等级。
 * 支持添加/删除紧急联系人，一键 SOS 呼叫。
 *
 * v0.6.0 起通过构造函数注入 [AppRepository]。
 */
@HiltViewModel
class FamilyViewModel
    @Inject
    constructor(
        private val repository: AppRepository,
    ) : ViewModel() {
        /**
         * 紧急联系人列表。
         */
        val contacts: StateFlow<List<Contact>> =
            repository.contacts
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        /**
         * 最近的体征记录。
         */
        val records: StateFlow<List<VitalRecord>> =
            repository.records
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        /**
         * 当前风险评估结果（基于最近一次各类型体征）。
         */
        val assessment =
            repository.records
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
         * 获取第一个紧急联系人（用于一键 SOS）。
         */
        fun primaryContact(): Contact? = contacts.value.firstOrNull()
    }
