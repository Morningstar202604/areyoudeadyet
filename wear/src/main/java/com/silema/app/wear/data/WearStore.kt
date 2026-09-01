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
 *
 * 与手机端 AppRepository 同思路，但接受目录参数（不依赖 Android Context 全局单例），
 * 职责仅限手表自身产生的体征/运动，手机同步是后续阶段。
 *
 * v0.5.0 起添加删除、清空、导出/导入功能，与手机端功能对齐。
 */
@Serializable
private data class WearStoreFile(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val records: List<VitalRecord> = emptyList(),
    val workouts: List<Workout> = emptyList(),
)

object WearStore {
    private val json =
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }
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
            runCatching {
                dataFile.writeText(
                    json.encodeToString(
                        WearStoreFile(
                            version = 1,
                            exportedAt = System.currentTimeMillis(),
                            records = _records.value,
                            workouts = _workouts.value,
                        ),
                    ),
                )
            }
        }
    }

    // ---------- 体征记录 ----------

    fun addRecord(r: VitalRecord) {
        _records.value = (_records.value + r).sortedByDescending { it.timestampMillis }
        save()
    }

    fun removeRecord(
        typeId: String,
        timestampMillis: Long,
    ) {
        _records.value =
            _records.value.filterNot {
                it.typeId == typeId && it.timestampMillis == timestampMillis
            }
        save()
    }

    // ---------- 运动记录 ----------

    fun addWorkout(w: Workout) {
        _workouts.value = (_workouts.value + w).sortedByDescending { it.startMillis }
        save()
    }

    fun removeWorkout(id: String) {
        _workouts.value = _workouts.value.filterNot { it.id == id }
        save()
    }

    // ---------- 数据管理 ----------

    /** 一键清空全部数据（演示/隐私场景）。 */
    fun clearAll() {
        _records.value = emptyList()
        _workouts.value = emptyList()
        save()
    }

    /**
     * 导出全部数据为 JSON 字符串，用于备份或与手机端同步。
     */
    fun exportToJson(): String =
        json.encodeToString(
            WearStoreFile(
                version = 1,
                exportedAt = System.currentTimeMillis(),
                records = _records.value,
                workouts = _workouts.value,
            ),
        )

    /**
     * 从 JSON 字符串导入数据，导入前清空现有数据。
     *
     * @return 导入是否成功
     */
    fun importFromJson(jsonString: String): Boolean =
        runCatching {
            val data = json.decodeFromString<WearStoreFile>(jsonString)
            _records.value = data.records
            _workouts.value = data.workouts
            save()
            true
        }.getOrDefault(false)

    /**
     * 获取指定类型的最新一条记录。
     */
    fun latestOfType(typeId: String): VitalRecord? =
        _records.value
            .filter { it.typeId == typeId }
            .maxByOrNull { it.timestampMillis }
}
