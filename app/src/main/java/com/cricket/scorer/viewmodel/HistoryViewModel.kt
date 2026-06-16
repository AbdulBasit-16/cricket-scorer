package com.cricket.scorer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cricket.scorer.data.database.entities.*
import com.cricket.scorer.data.model.*
import com.cricket.scorer.data.repository.CricketRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: CricketRepository) : ViewModel() {

    val allMatches: StateFlow<List<MatchEntity>> = repository.getAllMatchesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val filteredMatches: StateFlow<List<MatchEntity>> = combine(
        allMatches,
        _searchQuery
    ) { matches, query ->
        if (query.isBlank()) {
            matches.filter { it.status == "COMPLETED" }
        } else {
            matches.filter { 
                it.status == "COMPLETED" && 
                (it.teamAName.contains(query, ignoreCase = true) || it.teamBName.contains(query, ignoreCase = true))
            }
        }
    }
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedScorecard = MutableStateFlow<MatchScorecard?>(null)
    val selectedScorecard = _selectedScorecard.asStateFlow()

    fun searchMatches(query: String) {
        _searchQuery.value = query
    }

    fun selectMatch(matchId: Long) {
        viewModelScope.launch {
            try {
                val match = repository.getMatchById(matchId) ?: return@launch
                val innings = repository.getInningsForMatch(matchId)
                val players = repository.getAllPlayersFlow().first().associateBy { it.id }

                val inningsScores = innings.map { inning ->
                    val deliveries = repository.getDeliveriesForInning(inning.id)
                    
                    // Total runs and wickets
                    val totalRuns = deliveries.sumOf { it.runsScored + it.extrasRuns }
                    val totalWickets = deliveries.count { it.isWicket }
                    val totalBalls = deliveries.count { it.extraType != "WIDE" && it.extraType != "NO_BALL" }

                    // 1. Batting Scorecard
                    // Extract all players who batted
                    val batsmanIds = deliveries.flatMap { listOf(it.strikerId, it.nonStrikerId) }.distinct()
                    val battingScorecard = batsmanIds.map { batsmanId ->
                        val player = players[batsmanId] ?: PlayerEntity(name = "Unknown")
                        val batsmanDels = deliveries.filter { it.strikerId == batsmanId }
                        
                        val runs = batsmanDels.sumOf { it.runsScored }
                        val balls = batsmanDels.count { it.extraType != "WIDE" }
                        val fours = batsmanDels.count { it.runsScored == 4 }
                        val sixes = batsmanDels.count { it.runsScored == 6 }
                        val sr = if (balls == 0) 0.0 else (runs.toDouble() / balls) * 100.0

                        // Dismissal Info
                        val dismissal = deliveries.firstOrNull { it.isWicket && it.dismissedPlayerId == batsmanId }
                        val dismissalText = if (dismissal != null) {
                            val bowlerName = players[dismissal.bowlerId]?.name ?: "Unknown"
                            val fielderName = players[dismissal.assistingPlayerId]?.name ?: ""
                            
                            when (dismissal.wicketType) {
                                "BOWLED" -> "b $bowlerName"
                                "CAUGHT" -> if (fielderName.isNotBlank()) "c $fielderName b $bowlerName" else "c & b $bowlerName"
                                "LBW" -> "lbw b $bowlerName"
                                "STUMPED" -> if (fielderName.isNotBlank()) "st $fielderName b $bowlerName" else "st stumped b $bowlerName"
                                "RUN_OUT" -> if (fielderName.isNotBlank()) "run out ($fielderName)" else "run out"
                                "HIT_WICKET" -> "hit wicket b $bowlerName"
                                "RETIRED" -> "retired"
                                else -> "out"
                            }
                        } else {
                            "not out"
                        }

                        BatsmanScoreCard(
                            player = player,
                            runs = runs,
                            balls = balls,
                            fours = fours,
                            sixes = sixes,
                            strikeRate = sr,
                            dismissalText = dismissalText
                        )
                    }.sortedBy { card ->
                        // Sort by order of batting (first delivery they faced/appeared in)
                        deliveries.indexOfFirst { it.strikerId == card.player.id || it.nonStrikerId == card.player.id }
                    }

                    // 2. Bowling Scorecard
                    val bowlerIds = deliveries.map { it.bowlerId }.distinct()
                    val bowlingScorecard = bowlerIds.map { bowlerId ->
                        val player = players[bowlerId] ?: PlayerEntity(name = "Unknown")
                        val bowlerDels = deliveries.filter { it.bowlerId == bowlerId }
                        
                        val runsConceded = bowlerDels.sumOf {
                            it.runsScored + if (it.extraType == "WIDE" || it.extraType == "NO_BALL") it.extrasRuns else 0
                        }
                        val legalBalls = bowlerDels.count { it.extraType != "WIDE" && it.extraType != "NO_BALL" }
                        val overs = (legalBalls / 6) + (legalBalls % 6) / 10.0
                        val wickets = bowlerDels.count {
                            it.isWicket && it.wicketType != null && 
                            it.wicketType != "RUN_OUT" && it.wicketType != "RETIRED" && it.wicketType != "OBSTRUCTING_FIELD"
                        }
                        
                        val economy = if (legalBalls == 0) 0.0 else (runsConceded.toDouble() / (legalBalls.toDouble() / 6.0))
                        
                        // Calculate maidens
                        val bowlerOvers = bowlerDels.groupBy { it.overId }
                        var maidens = 0
                        for ((_, dels) in bowlerOvers) {
                            val overLegalBalls = dels.count { it.extraType != "WIDE" && it.extraType != "NO_BALL" }
                            if (overLegalBalls >= 6) {
                                val overRuns = dels.sumOf {
                                    it.runsScored + if (it.extraType == "WIDE" || it.extraType == "NO_BALL") it.extrasRuns else 0
                                }
                                if (overRuns == 0) {
                                    maidens++
                                }
                            }
                        }

                        BowlerScoreCard(
                            player = player,
                            overs = overs,
                            maidens = maidens,
                            runsConceded = runsConceded,
                            wickets = wickets,
                            economy = economy
                        )
                    }

                    // 3. Partnerships
                    val partnerships = mutableListOf<PartnershipScoreCard>()
                    var tempDeliveries = mutableListOf<DeliveryEntity>()
                    var wicketCount = 0
                    
                    for (del in deliveries) {
                        tempDeliveries.add(del)
                        if (del.isWicket) {
                            wicketCount++
                            val runs = tempDeliveries.sumOf { it.runsScored + it.extrasRuns }
                            val balls = tempDeliveries.count { it.extraType != "WIDE" }
                            val batter1 = players[del.strikerId]?.name ?: "Unknown"
                            val batter2 = players[del.nonStrikerId]?.name ?: "Unknown"
                            
                            partnerships.add(
                                PartnershipScoreCard(
                                    wicketNumber = wicketCount,
                                    runs = runs,
                                    balls = balls,
                                    batter1Name = batter1,
                                    batter2Name = batter2
                                )
                            )
                            tempDeliveries.clear()
                        }
                    }
                    
                    // Final partnership for remaining wickets
                    if (tempDeliveries.isNotEmpty()) {
                        val runs = tempDeliveries.sumOf { it.runsScored + it.extrasRuns }
                        val balls = tempDeliveries.count { it.extraType != "WIDE" }
                        val lastDel = tempDeliveries.last()
                        val batter1 = players[lastDel.strikerId]?.name ?: "Unknown"
                        val batter2 = players[lastDel.nonStrikerId]?.name ?: "Unknown"
                        
                        partnerships.add(
                            PartnershipScoreCard(
                                wicketNumber = wicketCount + 1,
                                runs = runs,
                                balls = balls,
                                batter1Name = batter1,
                                batter2Name = batter2
                            )
                        )
                    }

                    InningScorecard(
                        inning = inning,
                        totalRuns = totalRuns,
                        totalWickets = totalWickets,
                        totalBalls = totalBalls,
                        battingScorecard = battingScorecard,
                        bowlingScorecard = bowlingScorecard,
                        partnerships = partnerships
                    )
                }

                _selectedScorecard.value = MatchScorecard(
                    match = match,
                    inningsScores = inningsScores
                )
            } catch (e: Exception) {
                // error handling
            }
        }
    }

    fun clearSelectedScorecard() {
        _selectedScorecard.value = null
    }

    fun deleteMatch(match: MatchEntity) {
        viewModelScope.launch {
            try {
                repository.deleteMatch(match)
                clearSelectedScorecard()
            } catch (e: Exception) {
                // handle error
            }
        }
    }
}
