package com.silema.app.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.silema.app.db.entity.VitalRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VitalRecordDao {
    @Query("SELECT * FROM vital_records ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<VitalRecordEntity>>

    @Query("SELECT * FROM vital_records ORDER BY timestampMillis DESC")
    suspend fun getAll(): List<VitalRecordEntity>

    @Query("SELECT * FROM vital_records WHERE typeId = :typeId ORDER BY timestampMillis DESC")
    suspend fun getByType(typeId: String): List<VitalRecordEntity>

    @Query("SELECT * FROM vital_records WHERE timestampMillis >= :sinceMillis ORDER BY timestampMillis DESC")
    suspend fun getSince(sinceMillis: Long): List<VitalRecordEntity>

    /**
     * 查询指定类型、指定时间范围内是否存在记录（用于去重判断）。
     */
    @Query(
        """
        SELECT COUNT(*) FROM vital_records
        WHERE typeId = :typeId AND timestampMillis BETWEEN :fromMillis AND :toMillis
        """,
    )
    suspend fun countInRange(
        typeId: String,
        fromMillis: Long,
        toMillis: Long,
    ): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: VitalRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<VitalRecordEntity>)

    /**
     * 删除指定类型、指定时间戳的记录。
     */
    @Query("DELETE FROM vital_records WHERE typeId = :typeId AND timestampMillis = :timestampMillis")
    suspend fun delete(
        typeId: String,
        timestampMillis: Long,
    ): Int

    /**
     * 删除指定类型、指定时间范围内的所有记录（用于 addRecord 时的同分钟去重）。
     */
    @Query("DELETE FROM vital_records WHERE typeId = :typeId AND timestampMillis BETWEEN :fromMillis AND :toMillis")
    suspend fun deleteInRange(
        typeId: String,
        fromMillis: Long,
        toMillis: Long,
    ): Int

    @Query("DELETE FROM vital_records")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM vital_records")
    suspend fun count(): Int
}
