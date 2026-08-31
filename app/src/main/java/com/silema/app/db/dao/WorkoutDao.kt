package com.silema.app.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.silema.app.db.entity.WorkoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Query("SELECT * FROM workouts ORDER BY startMillis DESC")
    fun observeAll(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts ORDER BY startMillis DESC")
    suspend fun getAll(): List<WorkoutEntity>

    @Query("SELECT * FROM workouts WHERE startMillis >= :sinceMillis ORDER BY startMillis DESC")
    suspend fun getSince(sinceMillis: Long): List<WorkoutEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workout: WorkoutEntity)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM workouts")
    suspend fun clearAll()
}
