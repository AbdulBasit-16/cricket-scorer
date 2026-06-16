package com.cricket.scorer.data.repository

import com.cricket.scorer.data.database.daos.*
import com.cricket.scorer.data.database.entities.*
import com.cricket.scorer.data.model.BattingStats
import com.cricket.scorer.data.model.BowlingStats
import com.cricket.scorer.data.model.MatchPerformance
import com.cricket.scorer.data.model.PlayerStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class CricketRepositoryImpl(
    private val playerDao: PlayerDao,
    private val matchDao: MatchDao,
    private val inningDao: InningDao,
    private val overDao: OverDao,
    private val deliveryDao: DeliveryDao
) : CricketRepository {

    override fun getAllPlayersFlow(): Flow<List<PlayerEntity>> = playerDao.getAllPlayersFlow()

    override suspend fun getOrCreatePlayer(name: String): PlayerEntity {
        val trimmed = name.trim()
        val existing = playerDao.getPlayerByName(trimmed)
        if (existing != null) return existing
        val id = playerDao.insert(PlayerEntity(name = trimmed))
        return PlayerEntity(id = id, name = trimmed)
    }

    override suspend fun getPlayerById(id: Long): PlayerEntity? = playerDao.getPlayerById(id)

    override fun getAllMatchesFlow(): Flow<List<MatchEntity>> = matchDao.getAllMatchesFlow()

    override suspend fun getMatchById(id: Long): MatchEntity? = matchDao.getMatchById(id)

    override suspend fun insertMatch(match: MatchEntity): Long = matchDao.insert(match)

    override suspend fun updateMatch(match: MatchEntity) = matchDao.update(match)

    override suspend fun getLiveMatch(): MatchEntity? = matchDao.getLiveMatch()

    override suspend fun deleteMatch(match: MatchEntity) = matchDao.delete(match)

    override suspend fun getInningsForMatch(matchId: Long): List<InningEntity> =
        inningDao.getInningsForMatch(matchId)

    override suspend fun insertInning(inning: InningEntity): Long = inningDao.insert(inning)

    override suspend fun updateInning(inning: InningEntity) = inningDao.update(inning)

    override suspend fun getOversForInning(inningId: Long): List<OverEntity> =
        overDao.getOversForInning(inningId)

    override suspend fun insertOver(over: OverEntity): Long = overDao.insert(over)

    override suspend fun updateOver(over: OverEntity) = overDao.update(over)

    override suspend fun getActiveOver(inningId: Long): OverEntity? = overDao.getActiveOver(inningId)

    override suspend fun getDeliveriesForInning(inningId: Long): List<DeliveryEntity> =
        deliveryDao.getDeliveriesForInning(inningId)

    override suspend fun insertDelivery(delivery: DeliveryEntity): Long =
        deliveryDao.insert(delivery)

    override suspend fun deleteDeliveryById(deliveryId: Long) =
        deliveryDao.deleteById(deliveryId)

    override suspend fun getLastDelivery(): DeliveryEntity? = deliveryDao.getLastDelivery()

    override suspend fun getPlayerStats(playerId: Long): PlayerStats {
        val player = playerDao.getPlayerById(playerId) ?: PlayerEntity(name = "Unknown")
        
        // Fetch all data for calculations
        val battingDeliveries = deliveryDao.getBattingDeliveries(playerId)
        val bowlingDeliveries = deliveryDao.getBowlingDeliveries(playerId)
        val dismissals = deliveryDao.getDismissals(playerId)

        // 1. Batting calculation
        val strikerDeliveries = battingDeliveries.filter { it.strikerId == playerId }
        val totalRuns = strikerDeliveries.sumOf { it.runsScored }
        val ballsFaced = strikerDeliveries.count { it.extraType != "WIDE" }
        val timesDismissed = dismissals.count {
            it.isWicket && it.dismissedPlayerId == playerId && 
            it.wicketType != "RETIRED" // retired hurt is not a dismissal in averages
        }

        // Group runs by inning
        val runsPerInning = strikerDeliveries.groupBy { it.inningId }
            .mapValues { entry -> entry.value.sumOf { it.runsScored } }
        
        val inningsPlayed = runsPerInning.size
        val highestScore = if (runsPerInning.isEmpty()) 0 else runsPerInning.values.maxOrNull() ?: 0
        
        val fifties = runsPerInning.values.count { it in 50..99 }
        val hundreds = runsPerInning.values.count { it >= 100 }
        val ducks = runsPerInning.keys.count { inningId ->
            val runs = runsPerInning[inningId] ?: 0
            runs == 0 && dismissals.any { it.inningId == inningId && it.dismissedPlayerId == playerId }
        }

        val battingAverage = if (timesDismissed == 0) {
            totalRuns.toDouble()
        } else {
            totalRuns.toDouble() / timesDismissed
        }
        val strikeRate = if (ballsFaced == 0) 0.0 else (totalRuns.toDouble() / ballsFaced) * 100.0

        val battingStats = BattingStats(
            totalRuns = totalRuns,
            inningsPlayed = inningsPlayed,
            ballsFaced = ballsFaced,
            highestScore = highestScore,
            average = battingAverage,
            strikeRate = strikeRate,
            fifties = fifties,
            hundreds = hundreds,
            ducks = ducks
        )

        // 2. Bowling calculation
        val bowlerDeliveries = bowlingDeliveries.filter { it.bowlerId == playerId }
        // Bowler wickets exclude run outs and retired outs
        val bowlerWickets = bowlerDeliveries.count {
            it.isWicket && it.wicketType != null && 
            it.wicketType != "RUN_OUT" && it.wicketType != "RETIRED" && it.wicketType != "OBSTRUCTING_FIELD"
        }
        
        // Bowler runs conceded = runs off bat + wides + no balls
        val bowlerRunsConceded = bowlerDeliveries.sumOf {
            it.runsScored + if (it.extraType == "WIDE" || it.extraType == "NO_BALL") it.extrasRuns else 0
        }
        
        val legalBallsBowled = bowlerDeliveries.count { it.extraType != "WIDE" && it.extraType != "NO_BALL" }
        
        val economy = if (legalBallsBowled == 0) 0.0 else (bowlerRunsConceded.toDouble() / (legalBallsBowled.toDouble() / 6.0))
        val bowlingAverage = if (bowlerWickets == 0) 0.0 else bowlerRunsConceded.toDouble() / bowlerWickets
        val bowlingStrikeRate = if (bowlerWickets == 0) 0.0 else legalBallsBowled.toDouble() / bowlerWickets

        // Calculate maidens
        val runsPerOver = bowlerDeliveries.groupBy { it.overId }
        var maidens = 0
        for ((overId, deliveries) in runsPerOver) {
            val legalBallsInOver = deliveries.count { it.extraType != "WIDE" && it.extraType != "NO_BALL" }
            if (legalBallsInOver >= 6) { // Completed over
                val runsInOver = deliveries.sumOf {
                    it.runsScored + if (it.extraType == "WIDE" || it.extraType == "NO_BALL") it.extrasRuns else 0
                }
                if (runsInOver == 0) {
                    maidens++
                }
            }
        }

        // Best figures
        val bowlingPerInning = bowlerDeliveries.groupBy { it.inningId }.mapValues { entry ->
            val inningWickets = entry.value.count {
                it.isWicket && it.wicketType != null && 
                it.wicketType != "RUN_OUT" && it.wicketType != "RETIRED" && it.wicketType != "OBSTRUCTING_FIELD"
            }
            val inningRuns = entry.value.sumOf {
                it.runsScored + if (it.extraType == "WIDE" || it.extraType == "NO_BALL") it.extrasRuns else 0
            }
            Pair(inningWickets, inningRuns)
        }

        var bestWickets = 0
        var bestRuns = 0
        for ((_, stats) in bowlingPerInning) {
            val (w, r) = stats
            if (w > bestWickets || (w == bestWickets && r < bestRuns) || (bestWickets == 0 && w > 0)) {
                bestWickets = w
                bestRuns = r
            }
        }

        val bowlingStats = BowlingStats(
            wickets = bowlerWickets,
            runsConceded = bowlerRunsConceded,
            ballsBowled = legalBallsBowled,
            economy = economy,
            average = bowlingAverage,
            strikeRate = bowlingStrikeRate,
            maidens = maidens,
            bestWickets = bestWickets,
            bestRuns = bestRuns
        )

        // 3. Recent Performance (Last 5 matches)
        // Find all matches where the player batted or bowled
        val allMatchIds = (strikerDeliveries.map { it.inningId } + bowlerDeliveries.map { it.inningId })
            .distinct()
            .mapNotNull { inningId -> inningDao.getInningById(inningId)?.matchId }
            .distinct()

        val recentMatches = allMatchIds.mapNotNull { matchId ->
            val match = matchDao.getMatchById(matchId) ?: return@mapNotNull null
            
            // Batting performance in this match
            val matchInnings = inningDao.getInningsForMatch(matchId)
            val matchInningIds = matchInnings.map { it.id }
            
            val matchBattingDeliveries = strikerDeliveries.filter { it.inningId in matchInningIds }
            val matchRuns = matchBattingDeliveries.sumOf { it.runsScored }
            val matchBalls = matchBattingDeliveries.count { it.extraType != "WIDE" }
            val matchDismissed = dismissals.any { it.inningId in matchInningIds && it.dismissedPlayerId == playerId }
            
            // Bowling performance in this match
            val matchBowlingDeliveries = bowlerDeliveries.filter { it.inningId in matchInningIds }
            val matchWickets = matchBowlingDeliveries.count {
                it.isWicket && it.wicketType != null && 
                it.wicketType != "RUN_OUT" && it.wicketType != "RETIRED" && it.wicketType != "OBSTRUCTING_FIELD"
            }
            val matchRunsConceded = matchBowlingDeliveries.sumOf {
                it.runsScored + if (it.extraType == "WIDE" || it.extraType == "NO_BALL") it.extrasRuns else 0
            }

            MatchPerformance(
                matchId = matchId,
                teamNames = "${match.teamAName} vs ${match.teamBName}",
                runsScored = matchRuns,
                ballsFaced = matchBalls,
                isDismissed = matchDismissed,
                wicketsTaken = matchWickets,
                runsConceded = matchRunsConceded,
                timestamp = match.createdAt
            )
        }
        .sortedByDescending { it.timestamp }
        .take(5)

        return PlayerStats(
            playerId = playerId,
            playerName = player.name,
            batting = battingStats,
            bowling = bowlingStats,
            recentPerformance = recentMatches
        )
    }

    override suspend fun getAllPlayersWithStats(): List<PlayerStats> {
        val players = playerDao.getAllPlayers()
        return players.map { getPlayerStats(it.id) }
    }
}
