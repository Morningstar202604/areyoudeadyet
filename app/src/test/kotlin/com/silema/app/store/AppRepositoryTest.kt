package com.silema.app.store

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.silema.app.data.Contact
import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalSource
import com.silema.app.data.VitalType
import com.silema.app.data.Workout
import com.silema.app.db.SilemaDatabase
import com.silema.app.db.toEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * AppRepository 集成测试。
 *
 * 覆盖：数据导出、数据导入、导出-导入往返一致性、旧版格式兼容。
 *
 * 使用 Robolectric + 内存 Room 数据库，绕过 Hilt 直接注入数据库实例。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AppRepositoryTest {

    private lateinit var db: SilemaDatabase
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, SilemaDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // 通过反射注入数据库实例到 AppRepository object
        val dbField = AppRepository::class.java.getDeclaredField("db")
        dbField.isAccessible = true
        dbField.set(null, db)

        // 初始化 SharedPreferences
        val prefsField = AppRepository::class.java.getDeclaredField("prefs")
        prefsField.isAccessible = true
        prefsField.set(null, context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `导出空数据库返回有效JSON`() = runBlocking {
        val json = AppRepository.exportToJson()
        assertNotNull("导出不应返回 null", json)
        assertTrue("JSON 应包含 version 字段", json!!.contains("\"version\""))
        assertTrue("JSON 应包含 records 字段", json.contains("\"records\""))
    }

    @Test
    fun `导出体征记录数据正确`() = runBlocking {
        // 插入测试数据
        val records = listOf(
            VitalRecord(VitalType.HEART_RATE.id, 72.0, 1000L, VitalSource.MANUAL),
            VitalRecord(VitalType.SYSTOLIC.id, 120.0, 2000L, VitalSource.MANUAL)
        )
        db.vitalRecordDao().insertAll(records.toEntity())

        val json = AppRepository.exportToJson()
        assertNotNull(json)
        assertTrue("JSON 应包含心率数据", json!!.contains("\"typeId\":\"heart_rate\""))
        assertTrue("JSON 应包含收缩压数据", json.contains("\"typeId\":\"systolic\""))
    }

    @Test
    fun `导入体征记录数据正确`() = runBlocking {
        val json = """
            {
                "version": 1,
                "exportedAt": 1000,
                "records": [
                    {"typeId":"heart_rate","value":75.0,"timestampMillis":5000,"source":"manual"},
                    {"typeId":"diastolic","value":80.0,"timestampMillis":6000,"source":"manual"}
                ],
                "contacts": [],
                "workouts": []
            }
        """.trimIndent()

        val result = AppRepository.importFromJson(json)
        assertTrue("导入应成功", result)

        val imported = db.vitalRecordDao().getAll()
        assertEquals("应导入 2 条记录", 2, imported.size)
    }

    @Test
    fun `导出导入往返数据一致`() = runBlocking {
        // 插入原始数据
        val original = listOf(
            VitalRecord(VitalType.HEART_RATE.id, 68.0, 10000L, VitalSource.MANUAL),
            VitalRecord(VitalType.SPO2.id, 98.0, 11000L, VitalSource.MANUAL)
        )
        db.vitalRecordDao().insertAll(original.toEntity())

        val contact = Contact("张三", "13800138000")
        db.contactDao().insert(contact.toEntity())

        val workout = Workout("w1", "walk", 20000L, 1800000L, 2000.0, 68.7, emptyList())
        db.workoutDao().insert(workout.toEntity())

        // 导出
        val json = AppRepository.exportToJson()
        assertNotNull(json)

        // 清空数据库
        db.vitalRecordDao().clearAll()
        db.contactDao().clearAll()
        db.workoutDao().clearAll()
        assertEquals(0, db.vitalRecordDao().getAll().size)

        // 导入
        val result = AppRepository.importFromJson(json!!)
        assertTrue(result)

        // 验证数据一致
        val importedRecords = db.vitalRecordDao().getAll()
        assertEquals("体征记录数量应一致", original.size, importedRecords.size)
        assertEquals("心率值应一致", 68.0, importedRecords.first { it.typeId == "heart_rate" }.value, 0.01)

        val importedContacts = db.contactDao().getAll()
        assertEquals("联系人数量应一致", 1, importedContacts.size)
        assertEquals("联系人姓名应一致", "张三", importedContacts[0].name)

        val importedWorkouts = db.workoutDao().getAll()
        assertEquals("运动记录数量应一致", 1, importedWorkouts.size)
        assertEquals("运动类型应一致", "walk", importedWorkouts[0].type)
    }

    @Test
    fun `导入旧版JSON格式仅体征记录`() = runBlocking {
        // 旧版格式（v0.4.x 及之前）
        val legacyJson = """
            {"records":[
                {"typeId":"heart_rate","value":70.0,"timestampMillis":1000,"source":"manual"}
            ]}
        """.trimIndent()

        val result = AppRepository.importFromJson(legacyJson)
        assertTrue("旧版格式导入应成功", result)

        val imported = db.vitalRecordDao().getAll()
        assertEquals("应导入 1 条记录", 1, imported.size)
        assertEquals("心率值应为 70", 70.0, imported[0].value, 0.01)
    }

    @Test
    fun `导入无效JSON返回失败`() = runBlocking {
        val result = AppRepository.importFromJson("invalid json {{{")
        assertTrue("无效 JSON 导入应失败", !result)
    }

    @Test
    fun `导入会清空现有数据`() = runBlocking {
        // 先插入一条旧数据
        db.vitalRecordDao().insert(
            VitalRecord(VitalType.HEART_RATE.id, 99.0, 100L, VitalSource.MANUAL).toEntity()
        )
        assertEquals(1, db.vitalRecordDao().getAll().size)

        // 导入新数据
        val json = """
            {"version":1,"exportedAt":1000,
             "records":[{"typeId":"heart_rate","value":72.0,"timestampMillis":2000,"source":"manual"}],
             "contacts":[],"workouts":[]}
        """.trimIndent()
        AppRepository.importFromJson(json)

        // 验证旧数据被清空，只有新数据
        val all = db.vitalRecordDao().getAll()
        assertEquals("应只有 1 条记录（旧数据被清空）", 1, all.size)
        assertEquals("应为新导入的心率值 72", 72.0, all[0].value, 0.01)
    }
}
