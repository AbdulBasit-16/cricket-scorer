package com.cricket.scorer.data.repository

import com.cricket.scorer.data.database.entities.*
import com.cricket.scorer.data.model.PlayerStats
import kotlinx.coroutines.flow.Flow

interface CricketRepository {
    // Players
    fun getAllPlayersFlow(): Flow<List<PlayerEntity>>
    suspend fun getOrCreatePlayer(name: String): PlayerEntity
    suspend fun getPlayerById(id: Long): PlayerEntity?
    suspend fun getPlayerStats(playerId: Long): PlayerStats
    suspend fun getAllPlayersWithStats(): List<PlayerStats>

    // Matches
    fun getAllMatchesFlow(): Flow<List<MatchEntity>>
    suspend fun getMatchById(id: Long): MatchEntity?
    suspend fun insertMatch(match: MatchEntity): Long
    suspend fun updateMatch(match: MatchEntity)
    suspend fun getLiveMatch(): MatchEntity?
    suspend fun deleteMatch(match: MatchEntity)

    // Innings
    suspend fun getInningsForMatch(matchId: Long): List<InningEntity>
    suspend fun insertInning(inning: InningEntity): Long
    suspend fun updateInning(inning: InningEntity)

    // Overs
    suspend fun getOversForInning(inningId: Long): List<OverEntity>
    suspend fun insertOver(over: OverEntity): Long
    suspend fun updateOver(over: OverEntity)
    suspend fun getActiveOver(inningId: Long): OverEntity?

    // Deliveries
    suspend fun getDeliveriesForInning(inningId: Long): List<DeliveryEntity>
    suspend fun insertDelivery(delivery: DeliveryEntity): Long
    suspend fun deleteDeliveryById(deliveryId: Long)
    suspend fun getLastDelivery(): DeliveryEntity?
}
