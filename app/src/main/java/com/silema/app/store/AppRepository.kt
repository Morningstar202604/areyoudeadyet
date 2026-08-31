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
import timber.log.Timber

/**
 * 离线优先的本地仓储。
 *
 * v0.5.0 起底层从 JSON 文件迁移到 Room 数据库，对外接口（StateFlow + 同步方法）保持不变，
 * UI 层无需改动。旧版本 JSON 数据会在首次启动时自动导入 Room，导入完成后 JSON 文件保留为备份。
 *
 * 设计要点：
 * - Room DAO 返回 Flow，收集后更新 StateFlow，保证 UI 即时响应
 * - 写操作在 IO Dispatcher 执行，不阻塞主线程
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

    /** 新增手动记录；同类型同分钟内的旧记录会被覆盖，避免重复保存。 */
    fun addRecord(record: VitalRecord) {
        scope.launch {
            val dao = db?.vitalRecordDao() ?: return@launch
            // 同类型同分钟内的旧记录删除
            val cutoff = record.timestampMillis - 60_000L
            val duplicates = dao.getByType(record.typeId)
                .filter { it.timestampMillis in cutoff..record.timestampMillis }
            duplicates.forEach { dao.delete(it.typeId, it.timestampMillis) }
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
     */
    fun mergeHealthConnect(incoming: List<VitalRecord>): Int {
        if (incoming.isEmpty()) return 0
        var added = 0
        scope.launch {
            val dao = db?.vitalRecordDao() ?: return@launch
            val existing = dao.getAll()
            val toInsert = mutableListOf<VitalRecord>()
            for (candidate in incoming.sortedBy { it.timestampMillis }) {
                val duplicate = existing.any {
                    it.typeId == candidate.typeId &&
                        Math.abs(it.timestampMillis - candidate.timestampMillis) <= 90_000L
                } || toInsert.any {
                    it.typeId == candidate.typeId &&
                        Math.abs(it.timestampMillis - candidate.timestampMillis) <= 90_000L
                }
                if (!duplicate) {
                    toInsert += candidate
                    added++
                }
            }
            if (toInsert.isNotEmpty()) {
                dao.insertAll(toInsert.toEntity())
            }
            Timber.d("mergeHealthConnect: incoming=${incoming.size} added=$added")
        }
        return added
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
                    @kotlinx.serialization.Serializable
                    data class VitalsFile(val records: List<VitalRecord>)
                    val data = json.decodeFromString<VitalsFile>(file.readText())
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
                    @kotlinx.serialization.Serializable
                    data class ContactsFile(val contacts: List<Contact>)
                    val data = json.decodeFromString<ContactsFile>(file.readText())
                    data.contacts.forEach { db?.contactDao()?.insert(it.toEntity()) }
                    Timber.i("migrated ${data.contacts.size} contacts from legacy JSON")
                }.onFailure { Timber.w(it, "legacy contacts migration failed") }
            }
        }

        // 迁移运动记录
        legacyWorkoutsFile?.let { file ->
            if (file.exists()) {
                runCatching {
                    @kotlinx.serialization.Serializable
                    data class WorkoutsFile(val workouts: List<Workout>)
                    val data = json.decodeFromString<WorkoutsFile>(file.readText())
                    data.workouts.forEach { db?.workoutDao()?.insert(it.toEntity()) }
                    Timber.i("migrated ${data.workouts.size} workouts from legacy JSON")
                }.onFailure { Timber.w(it, "legacy workouts migration failed") }
            }
        }
    }
}
