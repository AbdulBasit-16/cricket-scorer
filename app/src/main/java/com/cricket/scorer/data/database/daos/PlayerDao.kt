package com.cricket.scorer.data.database.daos

import androidx.room.*
import com.cricket.scorer.data.database.entities.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(player: PlayerEntity): Long

    @Query("SELECT * FROM players ORDER BY name ASC")
    fun getAllPlayersFlow(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players ORDER BY name ASC")
    suspend fun getAllPlayers(): List<PlayerEntity>

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getPlayerById(id: Long): PlayerEntity?

    @Query("SELECT * FROM players WHERE name = :name")
    suspend fun getPlayerByName(name: String): PlayerEntity?
}
