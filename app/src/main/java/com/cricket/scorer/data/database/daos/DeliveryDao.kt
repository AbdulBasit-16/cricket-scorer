package com.cricket.scorer.data.database.daos

import androidx.room.*
import com.cricket.scorer.data.database.entities.DeliveryEntity

@Dao
interface DeliveryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(delivery: DeliveryEntity): Long

    @Delete
    suspend fun delete(delivery: DeliveryEntity)

    @Query("DELETE FROM deliveries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM deliveries WHERE inningId = :inningId ORDER BY timestamp ASC")
    suspend fun getDeliveriesForInning(inningId: Long): List<DeliveryEntity>

    @Query("SELECT * FROM deliveries WHERE overId = :overId ORDER BY timestamp ASC")
    suspend fun getDeliveriesForOver(overId: Long): List<DeliveryEntity>

    @Query("SELECT * FROM deliveries WHERE strikerId = :playerId OR nonStrikerId = :playerId ORDER BY timestamp ASC")
    suspend fun getBattingDeliveries(playerId: Long): List<DeliveryEntity>

    @Query("SELECT * FROM deliveries WHERE bowlerId = :playerId ORDER BY timestamp ASC")
    suspend fun getBowlingDeliveries(playerId: Long): List<DeliveryEntity>

    @Query("SELECT * FROM deliveries WHERE dismissedPlayerId = :playerId")
    suspend fun getDismissals(playerId: Long): List<DeliveryEntity>

    @Query("SELECT * FROM deliveries ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastDelivery(): DeliveryEntity?
}
