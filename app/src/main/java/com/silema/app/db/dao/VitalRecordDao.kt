package com.silema.app.db.dao

import androidx.room.Dao
import androidx.room.Delete
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: VitalRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<VitalRecordEntity>)

    @Query("DELETE FROM vital_records WHERE typeId = :typeId AND timestampMillis = :timestampMillis")
    suspend fun delete(typeId: String, timestampMillis: Long): Int

    @Query("DELETE FROM vital_records")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM vital_records")
    suspend fun count(): Int
}
