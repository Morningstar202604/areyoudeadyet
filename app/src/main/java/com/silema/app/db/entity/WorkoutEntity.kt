package com.silema.app.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 运动记录实体。
 *
 * 轨迹点 [track] 以 JSON 字符串存储（List<List<Double>> → String），
 * 由 [com.silema.app.db.Converters] 负责转换。
 */
@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey val id: String,
    val type: String,
    val startMillis: Long,
    val durationMillis: Long,
    val distanceMeters: Double,
    val caloriesKcal: Double,
    val track: String, // JSON: List<List<Double>>
)
