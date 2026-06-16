package com.cricket.scorer.data.model

data class PlayerStats(
    val playerId: Long,
    val playerName: String,
    val batting: BattingStats,
    val bowling: BowlingStats,
    val recentPerformance: List<MatchPerformance>
)

data class BattingStats(
    val totalRuns: Int,
    val inningsPlayed: Int,
    val ballsFaced: Int,
    val highestScore: Int,
    val average: Double, // runs / times dismissed
    val strikeRate: Double, // (runs / balls) * 100
    val fifties: Int,
    val hundreds: Int,
    val ducks: Int
)

data class BowlingStats(
    val wickets: Int,
    val runsConceded: Int,
    val ballsBowled: Int,
    val economy: Double, // runsConceded / (ballsBowled / 6.0)
    val average: Double, // runsConceded / wickets
    val strikeRate: Double, // ballsBowled / wickets
    val maidens: Int,
    val bestWickets: Int,
    val bestRuns: Int
)

data class MatchPerformance(
    val matchId: Long,
    val teamNames: String,
    val runsScored: Int,
    val ballsFaced: Int,
    val isDismissed: Boolean,
    val wicketsTaken: Int,
    val runsConceded: Int,
    val timestamp: Long
)
