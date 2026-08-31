package com.silema.app.db

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room 类型转换器：复杂类型 ↔ 可存储的基础类型。
 *
 * 当前仅处理运动轨迹 [List]<[List]<[Double]>> → JSON 字符串。
 */
class Converters {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromTrack(track: List<List<Double>>): String = json.encodeToString(track)

    @TypeConverter
    fun toTrack(value: String): List<List<Double>> =
        runCatching { json.decodeFromString<List<List<Double>>>(value) }
            .getOrDefault(emptyList())
}
