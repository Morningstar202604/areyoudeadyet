package com.silema.app.wear.data

import com.silema.app.data.VitalRecord
import com.silema.app.data.Workout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 手表端本地仓储：JSON 文件持久化 + StateFlow 供 UI 订阅。
 * 与手机端 AppRepository 同思路，但接受目录参数（不依赖 Android Context 全局单例），
 * 职责仅限手表自身产生的体征/运动，手机同步是后续阶段。
 */
@Serializable
private data class WearStoreFile(
    val records: List<VitalRecord> = emptyList(),
    val workouts: List<Workout> = emptyList()
)

object WearStore {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val lock = Any()
    private lateinit var dataFile: File

    private val _records = MutableStateFlow<List<VitalRecord>>(emptyList())
    val records: StateFlow<List<VitalRecord>> = _records.asStateFlow()

    private val _workouts = MutableStateFlow<List<Workout>>(emptyList())
    val workouts: StateFlow<List<Workout>> = _workouts.asStateFlow()

    fun init(dir: File) {
        dataFile = File(dir, "wear_store.json").apply { parentFile?.mkdirs() }
        load()
    }

    private fun load() {
        synchronized(lock) {
            val parsed = runCatching { json.decodeFromString<WearStoreFile>(dataFile.readText()) }.getOrNull()
            _records.value = parsed?.records ?: emptyList()
            _workouts.value = parsed?.workouts ?: emptyList()
        }
    }

    private fun save() {
        synchronized(lock) {
            runCatching { dataFile.writeText(json.encodeToString(WearStoreFile(_records.value, _workouts.value))) }
        }
    }

    fun addRecord(r: VitalRecord) {
        _records.value = (_records.value + r).sortedBy { it.timestampMillis }
        save()
    }

    fun addWorkout(w: Workout) {
        _workouts.value = (_workouts.value + w).sortedBy { it.startMillis }
        save()
    }
}
