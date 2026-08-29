package com.silema.app.store

import android.content.Context
import com.silema.app.data.Contact
import com.silema.app.data.VitalRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
private data class VitalsFile(val records: List<VitalRecord>)

@Serializable
private data class ContactsFile(val contacts: List<Contact>)

@Serializable
private data class WorkoutsFile(val workouts: List<com.silema.app.data.Workout>)

/**
 * 离线优先的本地仓储：JSON 文件持久化 + StateFlow 供 UI 订阅。
 * 数据量级（老人一年的体征记录 < 数千条）远小于需要数据库的规模，
 * 用互斥锁保护的同步文件读写即可保证一致性。
 */
object AppRepository {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val lock = Any()

    private var vitalsFile: File? = null
    private var contactsFile: File? = null
    private var workoutsFile: File? = null
    private var prefs: android.content.SharedPreferences? = null

    private val _records = MutableStateFlow<List<VitalRecord>>(emptyList())
    val records: StateFlow<List<VitalRecord>> = _records.asStateFlow()

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _workouts = MutableStateFlow<List<com.silema.app.data.Workout>>(emptyList())
    val workouts: StateFlow<List<com.silema.app.data.Workout>> = _workouts.asStateFlow()

    fun init(context: Context) {
        val dir = File(context.filesDir, "silema").apply { mkdirs() }
        vitalsFile = File(dir, "vitals.json")
        contactsFile = File(dir, "contacts.json")
        workoutsFile = File(dir, "workouts.json")
        prefs = context.getSharedPreferences("silema_prefs", Context.MODE_PRIVATE)
        synchronized(lock) {
            _records.value = readJson(vitalsFile)?.let {
                runCatching { json.decodeFromString<VitalsFile>(it).records }.getOrDefault(emptyList())
            } ?: emptyList()
            _contacts.value = readJson(contactsFile)?.let {
                runCatching { json.decodeFromString<ContactsFile>(it).contacts }.getOrDefault(emptyList())
            } ?: emptyList()
            _workouts.value = readJson(workoutsFile)?.let {
                runCatching { json.decodeFromString<WorkoutsFile>(it).workouts }.getOrDefault(emptyList())
            } ?: emptyList()
        }
    }

    /** 新增手动记录；同类型同分钟内的旧记录会被覆盖，避免重复保存。 */
    fun addRecord(record: VitalRecord) = synchronized(lock) {
        val filtered = _records.value.filterNot {
            it.typeId == record.typeId &&
                Math.abs(it.timestampMillis - record.timestampMillis) < 60_000L
        }
        _records.value = (filtered + record).sortedByDescending { it.timestampMillis }
        persistVitals()
    }

    fun removeRecord(typeId: String, timestampMillis: Long) = synchronized(lock) {
        _records.value = _records.value.filterNot { it.typeId == typeId && it.timestampMillis == timestampMillis }
        persistVitals()
    }

    /**
     * 合并 Health Connect 拉取的数据：
     * 与已有记录（无论来源）在 90 秒内且类型相同的视为同一测量，跳过；
     * 否则追加。返回实际新增条数。
     */
    fun mergeHealthConnect(incoming: List<VitalRecord>): Int = synchronized(lock) {
        var added = 0
        val current = _records.value.toMutableList()
        for (candidate in incoming.sortedBy { it.timestampMillis }) {
            val duplicate = current.any {
                it.typeId == candidate.typeId &&
                    Math.abs(it.timestampMillis - candidate.timestampMillis) <= 90_000L
            }
            if (!duplicate) {
                current += candidate
                added++
            }
        }
        _records.value = current.sortedByDescending { it.timestampMillis }
        persistVitals()
        added
    }

    fun addContact(contact: Contact) = synchronized(lock) {
        if (_contacts.value.none { it.phone == contact.phone }) {
            _contacts.value = _contacts.value + contact
            persistContacts()
        }
    }

    fun removeContact(phone: String) = synchronized(lock) {
        _contacts.value = _contacts.value.filterNot { it.phone == phone }
        persistContacts()
    }

    /** 一键清空全部数据（演示/隐私场景）。 */
    fun clearAll() = synchronized(lock) {
        _records.value = emptyList()
        _contacts.value = emptyList()
        persistVitals()
        persistContacts()
        runCatching { prefs?.edit()?.clear()?.apply() }
    }

    // ---------- 运动记录 ----------

    fun addWorkout(w: com.silema.app.data.Workout) = synchronized(lock) {
        _workouts.value = (listOf(w) + _workouts.value).sortedByDescending { it.startMillis }
        persistWorkouts()
    }

    fun removeWorkout(id: String) = synchronized(lock) {
        _workouts.value = _workouts.value.filterNot { it.id == id }
        persistWorkouts()
    }

    private fun persistWorkouts() {
        io.execute {
            val text = synchronized(lock) { json.encodeToString(WorkoutsFile(_workouts.value)) }
            runCatching { workoutsFile?.writeText(text) }
        }
    }

    // ---------- 目标与提醒设置 ----------

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

    /**
     * 持久化在单一后台线程串行执行：FIFO 保证最后一次写入对应最新状态，
     * 同时避免在主线程做文件 IO（BLE 连续推送时尤其重要）。
     */
    private val io = java.util.concurrent.Executors.newSingleThreadExecutor()

    private fun persistVitals() {
        io.execute {
            val text = synchronized(lock) { json.encodeToString(VitalsFile(_records.value)) }
            runCatching { vitalsFile?.writeText(text) }
        }
    }

    private fun persistContacts() {
        io.execute {
            val text = synchronized(lock) { json.encodeToString(ContactsFile(_contacts.value)) }
            runCatching { contactsFile?.writeText(text) }
        }
    }

    private fun readJson(file: File?): String? {
        if (file == null || !file.exists()) return null
        return runCatching { file.readText() }.getOrNull()
    }
}
