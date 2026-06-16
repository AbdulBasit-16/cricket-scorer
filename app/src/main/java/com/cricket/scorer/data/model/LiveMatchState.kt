package com.cricket.scorer.data.model

import com.cricket.scorer.data.database.entities.*

data class LiveMatchState(
    val match: MatchEntity,
    val firstInning: InningEntity?,
    val secondInning: InningEntity?,
    val currentInning: InningEntity,
    
    // Core scores
    val totalRuns: Int,
    val totalWickets: Int,
    val totalBalls: Int, // total legal balls
    val currentOverBalls: Int, // balls in current over (0-5)
    val completedOvers: Int,
    
    // Active batsman details
    val striker: PlayerEntity?,
    val strikerRuns: Int,
    val strikerBalls: Int,
    val strikerFours: Int,
    val strikerSixes: Int,
    
    val nonStriker: PlayerEntity?,
    val nonStrikerRuns: Int,
    val nonStrikerBalls: Int,
    val nonStrikerFours: Int,
    val nonStrikerSixes: Int,
    
    // Active bowler details
    val bowler: PlayerEntity?,
    val bowlerRunsConceded: Int,
    val bowlerBallsBowled: Int,
    val bowlerWickets: Int,
    val bowlerMaidens: Int,
    
    // Rates & Chasing Info
    val crr: Double,
    val target: Int?,
    val rrr: Double?,
    
    // Partnership
    val partnershipRuns: Int,
    val partnershipBalls: Int,
    
    // Ball-by-ball log
    val currentOverDeliveries: List<DeliveryEntity>,
    val recentDeliveries: List<DeliveryEntity> // For undo stack (last 6)
)
