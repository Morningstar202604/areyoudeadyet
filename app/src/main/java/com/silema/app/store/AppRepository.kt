package com.silema.app.store

import android.content.Context
import android.content.SharedPreferences
import com.silema.app.data.Contact
import com.silema.app.data.VitalRecord
import com.silema.app.data.Workout
import com.silema.app.db.SilemaDatabase
import com.silema.app.db.toDomain
import com.silema.app.db.toEntity
import com.silema.app.di.AppRepositoryEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * 离线优先的本地仓储。
 *
 * v0.5.0 起底层从 JSON 文件迁移到 Room 数据库，对外接口（StateFlow + 方法）保持不变，
 * UI 层无需改动。旧版本 JSON 数据会在首次启动时自动导入 Room，导入完成后 JSON 文件保留为备份。
 *
 * 设计要点：
 * - Room DAO 返回 Flow，收集后更新 StateFlow，保证 UI 即时响应
 * - 写操作在 IO Dispatcher 执行，不阻塞主线程
 * - [mergeHealthConnect] 为 suspend 函数，返回实际新增条数（修复了旧版异步返回 0 的 bug）
 * - SharedPreferences 仍用于轻量设置（目标、提醒开关等）
 */
object AppRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var db: SilemaDatabase? = null
    private var prefs: SharedPreferences? = null

    // 旧版 JSON 文件路径（迁移用，首次导入后不再写入）
    private var legacyVitalsFile: java.io.File? = null
    private var legacyContactsFile: java.io.File? = null
    private var legacyWorkoutsFile: java.io.File? = null

    private val _records = MutableStateFlow<List<VitalRecord>>(emptyList())
    val records: StateFlow<List<VitalRecord>> = _records.asStateFlow()

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _workouts = MutableStateFlow<List<Workout>>(emptyList())
    val workouts: StateFlow<List<Workout>> = _workouts.asStateFlow()

    /**
     * 初始化数据库并订阅数据流。必须在 Application.onCreate 调用一次。
     *
     * 通过 Hilt EntryPoint 获取 [SilemaDatabase] 单例，避免重复构建。
     * 这是 object 单例接入 Hilt 的过渡方式，后续可改为 @Singleton 类 + 构造函数注入。
     */
    fun init(context: Context) {
        val appContext = context.applicationContext

        // 通过 Hilt EntryPoint 获取数据库单例
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            AppRepositoryEntryPoint::class.java
        )
        val database = entryPoint.database()
        db = database

        prefs = appContext.getSharedPreferences("silema_prefs", Context.MODE_PRIVATE)

        // 旧版 JSON 文件定位（迁移用）
        val dir = java.io.File(appContext.filesDir, "silema").apply { mkdirs() }
        legacyVitalsFile = java.io.File(dir, "vitals.json")
        legacyContactsFile = java.io.File(dir, "contacts.json")
        legacyWorkoutsFile = java.io.File(dir, "workouts.json")

        // 订阅 Room Flow → StateFlow
        scope.launch {
            database.vitalRecordDao().observeAll().collect { entities ->
                _records.value = entities.toDomain()
            }
        }
        scope.launch {
            database.contactDao().observeAll().collect { entities ->
                _contacts.value = entities.toDomain()
            }
        }
        scope.launch {
            database.workoutDao().observeAll().collect { entities ->
                _workouts.value = entities.toDomain()
            }
        }

        // 首次启动：从旧版 JSON 迁移数据（仅当 Room 为空且 JSON 存在时）
        scope.launch {
            migrateLegacyJsonIfNeeded()
        }
    }

    // ---------- 体征记录 ----------

    /**
     * 新增手动记录；同类型同分钟内的旧记录会被覆盖，避免重复保存。
     *
     * 使用 SQL 直接删除时间范围内的旧记录，避免先查再删的低效操作。
     */
    fun addRecord(record: VitalRecord) {
        scope.launch {
            val dao = db?.vitalRecordDao() ?: return@launch
            // 同类型同分钟内的旧记录直接 SQL 删除
            val from = record.timestampMillis - 60_000L
            val to = record.timestampMillis + 1_000L
            dao.deleteInRange(record.typeId, from, to)
            dao.insert(record.toEntity())
            Timber.d("addRecord: type=${record.typeId} value=${record.value}")
        }
    }

    fun removeRecord(typeId: String, timestampMillis: Long) {
        scope.launch {
            db?.vitalRecordDao()?.delete(typeId, timestampMillis)
        }
    }

    /**
     * 合并 Health Connect 拉取的数据：
     * 与已有记录（无论来源）在 90 秒内且类型相同的视为同一测量，跳过；
     * 否则追加。返回实际新增条数。
     *
     * 注意：这是 suspend 函数，必须在协程中调用。修复了旧版异步返回 0 的 bug。
     */
    suspend fun mergeHealthConnect(incoming: List<VitalRecord>): Int = withContext(Dispatchers.IO) {
        if (incoming.isEmpty()) return@withContext 0
        val dao = db?.vitalRecordDao() ?: return@withContext 0

        var added = 0
        val toInsert = mutableListOf<VitalRecord>()

        for (candidate in incoming.sortedBy { it.timestampMillis }) {
            val from = candidate.timestampMillis - 90_000L
            val to = candidate.timestampMillis + 90_000L

            // 先查数据库中是否有重复
            val existsInDb = dao.countInRange(candidate.typeId, from, to) > 0
            // 再查本次待插入列表中是否有重复
            val existsInBatch = toInsert.any {
                it.typeId == candidate.typeId &&
                    Math.abs(it.timestampMillis - candidate.timestampMillis) <= 90_000L
            }

            if (!existsInDb && !existsInBatch) {
                toInsert += candidate
                added++
            }
        }

        if (toInsert.isNotEmpty()) {
            dao.insertAll(toInsert.toEntity())
        }
        Timber.d("mergeHealthConnect: incoming=${incoming.size} added=$added")
        added
    }

    // ---------- 联系人 ----------

    fun addContact(contact: Contact) {
        scope.launch {
            db?.contactDao()?.insert(contact.toEntity())
        }
    }

    fun removeContact(phone: String) {
        scope.launch {
            db?.contactDao()?.deleteByPhone(phone)
        }
    }

    // ---------- 运动记录 ----------

    fun addWorkout(w: Workout) {
        scope.launch {
            db?.workoutDao()?.insert(w.toEntity())
        }
    }

    fun removeWorkout(id: String) {
        scope.launch {
            db?.workoutDao()?.deleteById(id)
        }
    }

    // ---------- 一键清空 ----------

    /** 一键清空全部数据（演示/隐私场景）。 */
    fun clearAll() {
        scope.launch {
            db?.vitalRecordDao()?.clearAll()
            db?.contactDao()?.clearAll()
            db?.workoutDao()?.clearAll()
            runCatching { prefs?.edit()?.clear()?.apply() }
            Timber.w("clearAll: all data cleared")
        }
    }

    // ---------- 数据导出/导入（备份恢复） ----------

    /**
     * 将全部数据导出为 JSON 字符串，用于备份或迁移。
     *
     * 包含：体征记录、联系人、运动记录、SharedPreferences 设置。
     * 导出格式与旧版 JSON 格式兼容，可被 [importFromJson] 恢复。
     *
     * @return JSON 字符串，失败时返回 null
     */
    suspend fun exportToJson(): String? = withContext(Dispatchers.IO) {
        val database = db ?: return@withContext null
        val json = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }
        runCatching {
            val data = ExportData(
                version = 1,
                exportedAt = System.currentTimeMillis(),
                records = database.vitalRecordDao().getAll().toDomain(),
                contacts = database.contactDao().getAll().toDomain(),
                workouts = database.workoutDao().getAll().toDomain(),
                settings = ExportSettings(
                    stepsGoal = stepsGoal,
                    sleepGoalHours = sleepGoalHours,
                    weightKg = weightKg,
                    measureReminderOn = measureReminderOn,
                    measureReminderHour = measureReminderHour,
                    measureReminderMinute = measureReminderMinute,
                    sedentaryReminderOn = sedentaryReminderOn
                )
            )
            json.encodeToString(ExportData.serializer(), data)
        }.onFailure { Timber.w(it, "exportToJson failed") }
            .getOrNull()
    }

    /**
     * 从 JSON 字符串导入数据，用于恢复备份。
     *
     * 导入前会清空现有数据，然后插入备份中的数据。
     * 支持旧版 JSON 格式（仅体征记录）和新版格式（含设置）。
     *
     * @param json JSON 字符串
     * @return 导入是否成功
     */
    suspend fun importFromJson(json: String): Boolean = withContext(Dispatchers.IO) {
        val database = db ?: return@withContext false
        val parser = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        runCatching {
            // 先尝试解析新版格式
            val data = parser.decodeFromString(ExportData.serializer(), json)
            // 清空现有数据
            database.vitalRecordDao().clearAll()
            database.contactDao().clearAll()
            database.workoutDao().clearAll()
            // 插入备份数据
            if (data.records.isNotEmpty()) {
                database.vitalRecordDao().insertAll(data.records.toEntity())
            }
            if (data.contacts.isNotEmpty()) {
                data.contacts.forEach { database.contactDao().insert(it.toEntity()) }
            }
            if (data.workouts.isNotEmpty()) {
                data.workouts.forEach { database.workoutDao().insert(it.toEntity()) }
            }
            // 恢复设置
            data.settings?.let { s ->
                stepsGoal = s.stepsGoal
                sleepGoalHours = s.sleepGoalHours
                weightKg = s.weightKg
                measureReminderOn = s.measureReminderOn
                measureReminderHour = s.measureReminderHour
                measureReminderMinute = s.measureReminderMinute
                sedentaryReminderOn = s.sedentaryReminderOn
            }
            Timber.i("importFromJson: records=${data.records.size} contacts=${data.contacts.size} workouts=${data.workouts.size}")
            true
        }.recoverCatching {
            // 新版解析失败，尝试旧版格式（仅体征记录）
            val legacy = parser.decodeFromString<LegacyVitalsFile>(json)
            database.vitalRecordDao().clearAll()
            if (legacy.records.isNotEmpty()) {
                database.vitalRecordDao().insertAll(legacy.records.toEntity())
            }
            Timber.i("importFromJson (legacy): records=${legacy.records.size}")
            true
        }.onFailure { Timber.w(it, "importFromJson failed") }
            .getOrDefault(false)
    }

    // ---------- 目标与提醒设置（SharedPreferences） ----------

    var stepsGoal: Int
        get() = prefs?.getInt("steps_goal", 6000) ?: 6000
        set(v) { prefs?.edit()?.putInt("steps_goal", v)?.apply() }

    var sleepGoalHours: Int
        get() = prefs?.getInt("sleep_goal_h", 7) ?: 7
        set(v) { prefs?.edit()?.putInt("sleep_goal_h", v)?.apply() }

    var weightKg: Int
        get() = prefs?.getInt("weight_kg", 65) ?: 65
        set(v) { prefs?.edit()?.putInt("weight_kg", v)?.apply() }

    var measureReminderOn: Boolean
        get() = prefs?.getBoolean("rem_measure", false) ?: false
        set(v) { prefs?.edit()?.putBoolean("rem_measure", v)?.apply() }

    var measureReminderHour: Int
        get() = prefs?.getInt("rem_measure_h", 20) ?: 20
        set(v) { prefs?.edit()?.putInt("rem_measure_h", v)?.apply() }

    var measureReminderMinute: Int
        get() = prefs?.getInt("rem_measure_m", 0) ?: 0
        set(v) { prefs?.edit()?.putInt("rem_measure_m", v)?.apply() }

    var sedentaryReminderOn: Boolean
        get() = prefs?.getBoolean("rem_sedentary", false) ?: false
        set(v) { prefs?.edit()?.putBoolean("rem_sedentary", v)?.apply() }

    // ---------- 旧版 JSON 迁移 ----------

    /**
     * 仅当 Room 为空且旧版 JSON 文件存在时，执行一次性导入。
     * 导入后 JSON 文件保留（不删除），作为人工备份。
     */
    private suspend fun migrateLegacyJsonIfNeeded() {
        val dao = db?.vitalRecordDao() ?: return
        if (dao.count() > 0) return // 已有数据，不迁移

        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

        // 迁移体征记录
        legacyVitalsFile?.let { file ->
            if (file.exists()) {
                runCatching {
                    val data = json.decodeFromString<LegacyVitalsFile>(file.readText())
                    if (data.records.isNotEmpty()) {
                        dao.insertAll(data.records.toEntity())
                        Timber.i("migrated ${data.records.size} vital records from legacy JSON")
                    }
                }.onFailure { Timber.w(it, "legacy vitals migration failed") }
            }
        }

        // 迁移联系人
        legacyContactsFile?.let { file ->
            if (file.exists()) {
                runCatching {
                    val data = json.decodeFromString<LegacyContactsFile>(file.readText())
                    data.contacts.forEach { db?.contactDao()?.insert(it.toEntity()) }
                    Timber.i("migrated ${data.contacts.size} contacts from legacy JSON")
                }.onFailure { Timber.w(it, "legacy contacts migration failed") }
            }
        }

        // 迁移运动记录
        legacyWorkoutsFile?.let { file ->
            if (file.exists()) {
                runCatching {
                    val data = json.decodeFromString<LegacyWorkoutsFile>(file.readText())
                    data.workouts.forEach { db?.workoutDao()?.insert(it.toEntity()) }
                    Timber.i("migrated ${data.workouts.size} workouts from legacy JSON")
                }.onFailure { Timber.w(it, "legacy workouts migration failed") }
            }
        }
    }

    // ---------- 旧版 JSON 文件结构（仅迁移用） ----------

    @kotlinx.serialization.Serializable
    private data class LegacyVitalsFile(val records: List<VitalRecord>)

    @kotlinx.serialization.Serializable
    private data class LegacyContactsFile(val contacts: List<Contact>)

    @kotlinx.serialization.Serializable
    private data class LegacyWorkoutsFile(val workouts: List<Workout>)

    // ---------- 导出/导入数据结构（v1 格式） ----------

    @kotlinx.serialization.Serializable
    internal data class ExportData(
        val version: Int,
        val exportedAt: Long,
        val records: List<VitalRecord>,
        val contacts: List<Contact>,
        val workouts: List<Workout>,
        val settings: ExportSettings? = null
    )

    @kotlinx.serialization.Serializable
    internal data class ExportSettings(
        val stepsGoal: Int,
        val sleepGoalHours: Int,
        val weightKg: Int,
        val measureReminderOn: Boolean,
        val measureReminderHour: Int,
        val measureReminderMinute: Int,
        val sedentaryReminderOn: Boolean
    )
}
