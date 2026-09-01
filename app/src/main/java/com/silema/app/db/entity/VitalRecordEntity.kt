package com.silema.app.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 体征记录实体。
 *
 * 与领域模型 [com.silema.app.data.VitalRecord] 一一对应，
 * 单独定义 Entity 避免把 Room 注解污染纯 Kotlin 的 core 模块。
 */
@Entity(
    tableName = "vital_records",
    indices = [
        Index(value = ["typeId", "timestampMillis"]),
        Index(value = ["timestampMillis"]),
    ],
)
data class VitalRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val typeId: String,
    val value: Double,
    val timestampMillis: Long,
    val source: String,
)
