package com.cricket.scorer.data.model

import com.cricket.scorer.data.database.entities.*

data class MatchScorecard(
    val match: MatchEntity,
    val inningsScores: List<InningScorecard>
)

data class InningScorecard(
    val inning: InningEntity,
    val totalRuns: Int,
    val totalWickets: Int,
    val totalBalls: Int,
    val battingScorecard: List<BatsmanScoreCard>,
    val bowlingScorecard: List<BowlerScoreCard>,
    val partnerships: List<PartnershipScoreCard>
)

data class BatsmanScoreCard(
    val player: PlayerEntity,
    val runs: Int,
    val balls: Int,
    val fours: Int,
    val sixes: Int,
    val strikeRate: Double,
    val dismissalText: String // e.g. "not out", "b Bowler", "c Fielder b Bowler", "run out (Fielder)"
)

data class BowlerScoreCard(
    val player: PlayerEntity,
    val overs: Double, // represented as e.g. 4.0 or 3.2
    val maidens: Int,
    val runsConceded: Int,
    val wickets: Int,
    val economy: Double
)

data class PartnershipScoreCard(
    val wicketNumber: Int,
    val runs: Int,
    val balls: Int,
    val batter1Name: String,
    val batter2Name: String
)
