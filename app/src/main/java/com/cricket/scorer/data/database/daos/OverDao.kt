package com.cricket.scorer.data.database.daos

import androidx.room.*
import com.cricket.scorer.data.database.entities.OverEntity

@Dao
interface OverDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(over: OverEntity): Long

    @Update
    suspend fun update(over: OverEntity)

    @Query("SELECT * FROM overs WHERE inningId = :inningId ORDER BY overNumber ASC")
    suspend fun getOversForInning(inningId: Long): List<OverEntity>

    @Query("SELECT * FROM overs WHERE id = :id")
    suspend fun getOverById(id: Long): OverEntity?

    @Query("SELECT * FROM overs WHERE inningId = :inningId AND isCompleted = 0 LIMIT 1")
    suspend fun getActiveOver(inningId: Long): OverEntity?
}
