package com.cricket.scorer.data.database.daos

import androidx.room.*
import com.cricket.scorer.data.database.entities.InningEntity

@Dao
interface InningDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(inning: InningEntity): Long

    @Update
    suspend fun update(inning: InningEntity)

    @Query("SELECT * FROM innings WHERE matchId = :matchId ORDER BY inningNumber ASC")
    suspend fun getInningsForMatch(matchId: Long): List<InningEntity>

    @Query("SELECT * FROM innings WHERE id = :id")
    suspend fun getInningById(id: Long): InningEntity?
}
