package com.silema.app.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.silema.app.db.dao.ContactDao
import com.silema.app.db.dao.VitalRecordDao
import com.silema.app.db.dao.WorkoutDao
import com.silema.app.db.entity.ContactEntity
import com.silema.app.db.entity.VitalRecordEntity
import com.silema.app.db.entity.WorkoutEntity

/**
 * Silema 应用数据库。
 *
 * 三张表：
 * - vital_records：体征记录（心率/血压/血氧/体温/步数/睡眠/压力）
 * - contacts：紧急联系人
 * - workouts：运动记录（含轨迹）
 *
 * 版本号从 1 开始；后续结构变更需升级 version 并提供 Migration。
 */
@Database(
    entities = [
        VitalRecordEntity::class,
        ContactEntity::class,
        WorkoutEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SilemaDatabase : RoomDatabase() {
    abstract fun vitalRecordDao(): VitalRecordDao

    abstract fun contactDao(): ContactDao

    abstract fun workoutDao(): WorkoutDao

    companion object {
        const val DB_NAME = "silema.db"
    }
}
