package com.silema.app.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.silema.app.db.dao.ContactDao
import com.silema.app.db.dao.VitalRecordDao
import com.silema.app.db.dao.WorkoutDao
import com.silema.app.db.entity.ContactEntity
import com.silema.app.db.entity.VitalRecordEntity
import com.silema.app.db.entity.WorkoutEntity
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
 * Room DAO 单元测试。
 *
 * 使用 in-memory 数据库，测试增删改查和 Flow 观察。
 * 运行环境：Robolectric（本地 JVM 测试，无需模拟器）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SilemaDatabaseTest {

    private lateinit var db: SilemaDatabase
    private lateinit var vitalDao: VitalRecordDao
    private lateinit var contactDao: ContactDao
    private lateinit var workoutDao: WorkoutDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SilemaDatabase::class.java
        )
            .allowMainThreadQueries() // 测试用，允许主线程查询
            .build()
        vitalDao = db.vitalRecordDao()
        contactDao = db.contactDao()
        workoutDao = db.workoutDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    // ---------- VitalRecordDao 测试 ----------

    @Test
    fun `插入体征记录后可查询`() = runBlocking {
        val record = VitalRecordEntity(
            typeId = "heart_rate",
            value = 72.0,
            timestampMillis = 1000L,
            source = "manual"
        )
        vitalDao.insert(record)

        val all = vitalDao.getAll()
        assertEquals(1, all.size)
        assertEquals("heart_rate", all[0].typeId)
        assertEquals(72.0, all[0].value, 0.0)
    }

    @Test
    fun `按类型查询体征记录`() = runBlocking {
        vitalDao.insert(
            VitalRecordEntity(typeId = "heart_rate", value = 72.0, timestampMillis = 1000L, source = "manual")
        )
        vitalDao.insert(
            VitalRecordEntity(typeId = "systolic", value = 120.0, timestampMillis = 2000L, source = "manual")
        )

        val hrRecords = vitalDao.getByType("heart_rate")
        assertEquals(1, hrRecords.size)
        assertEquals(72.0, hrRecords[0].value, 0.0)
    }

    @Test
    fun `删除指定体征记录`() = runBlocking {
        vitalDao.insert(
            VitalRecordEntity(typeId = "heart_rate", value = 72.0, timestampMillis = 1000L, source = "manual")
        )
        assertEquals(1, vitalDao.count())

        vitalDao.delete("heart_rate", 1000L)
        assertEquals(0, vitalDao.count())
    }

    @Test
    fun `按时间范围删除体征记录`() = runBlocking {
        vitalDao.insert(
            VitalRecordEntity(typeId = "heart_rate", value = 70.0, timestampMillis = 1000L, source = "manual")
        )
        vitalDao.insert(
            VitalRecordEntity(typeId = "heart_rate", value = 75.0, timestampMillis = 5000L, source = "manual")
        )
        vitalDao.insert(
            VitalRecordEntity(typeId = "heart_rate", value = 80.0, timestampMillis = 10000L, source = "manual")
        )

        // 删除 4000-6000 范围内的记录
        val deleted = vitalDao.deleteInRange("heart_rate", 4000L, 6000L)
        assertEquals(1, deleted)
        assertEquals(2, vitalDao.count())
    }

    @Test
    fun `统计时间范围内记录数`() = runBlocking {
        vitalDao.insert(
            VitalRecordEntity(typeId = "heart_rate", value = 70.0, timestampMillis = 1000L, source = "manual")
        )
        vitalDao.insert(
            VitalRecordEntity(typeId = "heart_rate", value = 75.0, timestampMillis = 5000L, source = "manual")
        )

        val count = vitalDao.countInRange("heart_rate", 0L, 3000L)
        assertEquals(1, count)
    }

    @Test
    fun `清空体征记录`() = runBlocking {
        vitalDao.insert(
            VitalRecordEntity(typeId = "heart_rate", value = 72.0, timestampMillis = 1000L, source = "manual")
        )
        vitalDao.insert(
            VitalRecordEntity(typeId = "systolic", value = 120.0, timestampMillis = 2000L, source = "manual")
        )
        assertEquals(2, vitalDao.count())

        vitalDao.clearAll()
        assertEquals(0, vitalDao.count())
    }

    // ---------- ContactDao 测试 ----------

    @Test
    fun `插入联系人后可查询`() = runBlocking {
        contactDao.insert(ContactEntity(phone = "13800138000", name = "张三"))

        val all = contactDao.getAll()
        assertEquals(1, all.size)
        assertEquals("张三", all[0].name)
        assertEquals("13800138000", all[0].phone)
    }

    @Test
    fun `按手机号删除联系人`() = runBlocking {
        contactDao.insert(ContactEntity(phone = "13800138000", name = "张三"))
        contactDao.insert(ContactEntity(phone = "13900139000", name = "李四"))
        assertEquals(2, contactDao.getAll().size)

        contactDao.deleteByPhone("13800138000")
        val remaining = contactDao.getAll()
        assertEquals(1, remaining.size)
        assertEquals("李四", remaining[0].name)
    }

    // ---------- WorkoutDao 测试 ----------

    @Test
    fun `插入运动记录后可查询`() = runBlocking {
        val workout = WorkoutEntity(
            id = "w1",
            type = "walk",
            startMillis = 1000L,
            durationMillis = 1800000L,
            distanceMeters = 2000.0,
            caloriesKcal = 68.7,
            track = "[]"
        )
        workoutDao.insert(workout)

        val all = workoutDao.getAll()
        assertEquals(1, all.size)
        assertEquals("w1", all[0].id)
        assertEquals(2000.0, all[0].distanceMeters, 0.0)
    }

    @Test
    fun `按时间范围查询运动记录`() = runBlocking {
        workoutDao.insert(
            WorkoutEntity("w1", "walk", 1000L, 1000L, 100.0, 1.0, "[]")
        )
        workoutDao.insert(
            WorkoutEntity("w2", "run", 5000L, 1000L, 200.0, 2.0, "[]")
        )

        val recent = workoutDao.getSince(4000L)
        assertEquals(1, recent.size)
        assertEquals("w2", recent[0].id)
    }

    @Test
    fun `按ID删除运动记录`() = runBlocking {
        workoutDao.insert(
            WorkoutEntity("w1", "walk", 1000L, 1000L, 100.0, 1.0, "[]")
        )
        assertEquals(1, workoutDao.getAll().size)

        workoutDao.deleteById("w1")
        assertEquals(0, workoutDao.getAll().size)
    }

    // ---------- Converters 测试 ----------

    @Test
    fun `轨迹点 JSON 转换正确`() {
        val converters = Converters()
        val track = listOf(
            listOf(39.9087, 116.3975, 1000.0),
            listOf(39.9088, 116.3976, 2000.0)
        )

        val json = converters.fromTrack(track)
        assertNotNull(json)
        assertTrue(json.isNotEmpty())

        val decoded = converters.toTrack(json)
        assertEquals(2, decoded.size)
        assertEquals(39.9087, decoded[0][0], 0.0001)
        assertEquals(116.3975, decoded[0][1], 0.0001)
    }

    @Test
    fun `空轨迹转换正确`() {
        val converters = Converters()
        val json = converters.fromTrack(emptyList())
        val decoded = converters.toTrack(json)
        assertTrue(decoded.isEmpty())
    }
}
