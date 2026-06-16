package com.cricket.scorer.data.database.daos

import androidx.room.*
import com.cricket.scorer.data.database.entities.MatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(match: MatchEntity): Long

    @Update
    suspend fun update(match: MatchEntity)

    @Query("SELECT * FROM matches ORDER BY createdAt DESC")
    fun getAllMatchesFlow(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches ORDER BY createdAt DESC")
    suspend fun getAllMatches(): List<MatchEntity>

    @Query("SELECT * FROM matches WHERE id = :id")
    suspend fun getMatchById(id: Long): MatchEntity?

    @Query("SELECT * FROM matches WHERE status = 'LIVE' LIMIT 1")
    suspend fun getLiveMatch(): MatchEntity?
    
    @Delete
    suspend fun delete(match: MatchEntity)
}
