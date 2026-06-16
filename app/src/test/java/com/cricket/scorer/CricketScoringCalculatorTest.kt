package com.cricket.scorer

import com.cricket.scorer.data.database.entities.*
import com.cricket.scorer.data.repository.CricketScoringCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class CricketScoringCalculatorTest {

    private val player1 = PlayerEntity(id = 1, name = "Batter A")
    private val player2 = PlayerEntity(id = 2, name = "Batter B")
    private val player3 = PlayerEntity(id = 3, name = "Bowler X")
    private val players = mapOf(1L to player1, 2L to player2, 3L to player3)

    private val match = MatchEntity(
        id = 1,
        teamAName = "Team Alpha",
        teamBName = "Team Beta",
        oversCount = 2,
        matchType = "T20",
        tossWinner = "Team Alpha",
        tossDecision = "BAT",
        teamARoster = "Batter A, Batter B",
        teamBRoster = "Bowler X"
    )

    private val inning = InningEntity(
        id = 1,
        matchId = 1,
        battingTeamName = "Team Alpha",
        bowlingTeamName = "Team Beta",
        inningNumber = 1,
        currentStrikerId = 1,
        currentNonStrikerId = 2,
        currentBowlerId = 3
    )

    @Test
    fun testRunsAndWicketsCalculation() {
        val deliveries = listOf(
            DeliveryEntity(
                id = 1, inningId = 1, overId = 1, strikerId = 1, nonStrikerId = 2, bowlerId = 3,
                runsScored = 1, extrasRuns = 0, extraType = "NONE", isWicket = false
            ),
            DeliveryEntity(
                id = 2, inningId = 1, overId = 1, strikerId = 2, nonStrikerId = 1, bowlerId = 3,
                runsScored = 4, extrasRuns = 0, extraType = "NONE", isWicket = false
            ),
            DeliveryEntity(
                id = 3, inningId = 1, overId = 1, strikerId = 2, nonStrikerId = 1, bowlerId = 3,
                runsScored = 0, extrasRuns = 1, extraType = "WIDE", isWicket = false
            ),
            DeliveryEntity(
                id = 4, inningId = 1, overId = 1, strikerId = 2, nonStrikerId = 1, bowlerId = 3,
                runsScored = 0, extrasRuns = 0, extraType = "NONE", isWicket = true, wicketType = "BOWLED", dismissedPlayerId = 2
            )
        )

        val state = CricketScoringCalculator.calculateLiveMatchState(
            match = match,
            firstInning = inning,
            secondInning = null,
            currentInning = inning,
            deliveries = deliveries,
            players = players
        )

        // 1 run + 4 runs + 1 wide = 6 runs
        assertEquals(6, state.totalRuns)
        // 1 wicket
        assertEquals(1, state.totalWickets)
        // 3 legal balls (wide doesn't count)
        assertEquals(3, state.totalBalls)
        // Bowler runs conceded = 1 + 4 + 1 = 6 runs
        assertEquals(6, state.bowlerRunsConceded)
        // Bowler wickets = 1
        assertEquals(1, state.bowlerWickets)
    }
}
