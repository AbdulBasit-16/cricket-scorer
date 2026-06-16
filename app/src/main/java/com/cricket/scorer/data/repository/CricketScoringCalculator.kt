package com.cricket.scorer.data.repository

import com.cricket.scorer.data.database.entities.*
import com.cricket.scorer.data.model.LiveMatchState

object CricketScoringCalculator {

    fun calculateLiveMatchState(
        match: MatchEntity,
        firstInning: InningEntity?,
        secondInning: InningEntity?,
        currentInning: InningEntity,
        deliveries: List<DeliveryEntity>,
        players: Map<Long, PlayerEntity>,
        firstInningTotalRuns: Int? = null
    ): LiveMatchState {
        // Runs & Wickets
        val totalRuns = deliveries.sumOf { it.runsScored + it.extrasRuns }
        val totalWickets = deliveries.count { it.isWicket }
        
        // Balls & Overs
        val totalBalls = deliveries.count { it.extraType != "WIDE" && it.extraType != "NO_BALL" }
        val completedOvers = totalBalls / 6
        val currentOverBalls = totalBalls % 6

        val striker = players[currentInning.currentStrikerId]
        val nonStriker = players[currentInning.currentNonStrikerId]
        val bowler = players[currentInning.currentBowlerId]

        // Batsman statistics in this inning
        var strikerRuns = 0
        var strikerBalls = 0
        var strikerFours = 0
        var strikerSixes = 0
        if (currentInning.currentStrikerId != null) {
            val strikerDels = deliveries.filter { it.strikerId == currentInning.currentStrikerId }
            strikerRuns = strikerDels.sumOf { it.runsScored }
            strikerBalls = strikerDels.count { it.extraType != "WIDE" }
            strikerFours = strikerDels.count { it.runsScored == 4 }
            strikerSixes = strikerDels.count { it.runsScored == 6 }
        }

        var nonStrikerRuns = 0
        var nonStrikerBalls = 0
        var nonStrikerFours = 0
        var nonStrikerSixes = 0
        if (currentInning.currentNonStrikerId != null) {
            val nonStrikerDels = deliveries.filter { it.strikerId == currentInning.currentNonStrikerId }
            nonStrikerRuns = nonStrikerDels.sumOf { it.runsScored }
            nonStrikerBalls = nonStrikerDels.count { it.extraType != "WIDE" }
            nonStrikerFours = nonStrikerDels.count { it.runsScored == 4 }
            nonStrikerSixes = nonStrikerDels.count { it.runsScored == 6 }
        }

        // Bowler statistics in this inning
        var bowlerRunsConceded = 0
        var bowlerBallsBowled = 0
        var bowlerWickets = 0
        var bowlerMaidens = 0
        if (currentInning.currentBowlerId != null) {
            val bowlerDels = deliveries.filter { it.bowlerId == currentInning.currentBowlerId }
            bowlerRunsConceded = bowlerDels.sumOf {
                it.runsScored + if (it.extraType == "WIDE" || it.extraType == "NO_BALL") it.extrasRuns else 0
            }
            bowlerBallsBowled = bowlerDels.count { it.extraType != "WIDE" && it.extraType != "NO_BALL" }
            bowlerWickets = bowlerDels.count {
                it.isWicket && it.wicketType != null && 
                it.wicketType != "RUN_OUT" && it.wicketType != "RETIRED" && it.wicketType != "OBSTRUCTING_FIELD"
            }

            // Calculate bowler maidens in current inning
            val bowlerOvers = bowlerDels.groupBy { it.overId }
            for ((_, dels) in bowlerOvers) {
                val legalBalls = dels.count { it.extraType != "WIDE" && it.extraType != "NO_BALL" }
                if (legalBalls >= 6) {
                    val runs = dels.sumOf {
                        it.runsScored + if (it.extraType == "WIDE" || it.extraType == "NO_BALL") it.extrasRuns else 0
                    }
                    if (runs == 0) {
                        bowlerMaidens++
                    }
                }
            }
        }

        // CRR
        val oversFraction = totalBalls / 6.0
        val crr = if (totalBalls == 0) 0.0 else totalRuns / oversFraction

        // Target & RRR (For 2nd Inning)
        var target: Int? = null
        var rrr: Double? = null
        if (currentInning.inningNumber == 2 && firstInningTotalRuns != null) {
            target = firstInningTotalRuns + 1
            val runsNeeded = target - totalRuns
            val totalMatchBalls = match.oversCount * 6
            val ballsRemaining = totalMatchBalls - totalBalls
            
            rrr = if (ballsRemaining <= 0) {
                if (runsNeeded > 0) Double.POSITIVE_INFINITY else 0.0
            } else {
                (runsNeeded.toDouble() / (ballsRemaining / 6.0))
            }
        }

        // Partnership
        // Find deliveries since the last wicket in the current inning
        val lastWicketIndex = deliveries.indexOfLast { it.isWicket }
        val partnershipDeliveries = if (lastWicketIndex == -1) {
            deliveries
        } else {
            if (lastWicketIndex + 1 < deliveries.size) {
                deliveries.subList(lastWicketIndex + 1, deliveries.size)
            } else {
                emptyList()
            }
        }
        val partnershipRuns = partnershipDeliveries.sumOf { it.runsScored + it.extrasRuns }
        val partnershipBalls = partnershipDeliveries.count { it.extraType != "WIDE" }

        // Current Over Deliveries
        // Get the active over's deliveries (by looking at the last overId)
        val lastDelivery = deliveries.lastOrNull()
        val currentOverDeliveries = if (lastDelivery != null) {
            deliveries.filter { it.overId == lastDelivery.overId }
        } else {
            emptyList()
        }

        // Recent deliveries for undo (last 6)
        val recentDeliveries = deliveries.takeLast(6)

        return LiveMatchState(
            match = match,
            firstInning = firstInning,
            secondInning = secondInning,
            currentInning = currentInning,
            totalRuns = totalRuns,
            totalWickets = totalWickets,
            totalBalls = totalBalls,
            currentOverBalls = currentOverBalls,
            completedOvers = completedOvers,
            striker = striker,
            strikerRuns = strikerRuns,
            strikerBalls = strikerBalls,
            strikerFours = strikerFours,
            strikerSixes = strikerSixes,
            nonStriker = nonStriker,
            nonStrikerRuns = nonStrikerRuns,
            nonStrikerBalls = nonStrikerBalls,
            nonStrikerFours = nonStrikerFours,
            nonStrikerSixes = nonStrikerSixes,
            bowler = bowler,
            bowlerRunsConceded = bowlerRunsConceded,
            bowlerBallsBowled = bowlerBallsBowled,
            bowlerWickets = bowlerWickets,
            bowlerMaidens = bowlerMaidens,
            crr = crr,
            target = target,
            rrr = rrr,
            partnershipRuns = partnershipRuns,
            partnershipBalls = partnershipBalls,
            currentOverDeliveries = currentOverDeliveries,
            recentDeliveries = recentDeliveries
        )
    }
}
