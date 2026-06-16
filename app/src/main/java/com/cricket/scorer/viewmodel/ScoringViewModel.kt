package com.cricket.scorer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cricket.scorer.data.database.entities.*
import com.cricket.scorer.data.model.LiveMatchState
import com.cricket.scorer.data.repository.CricketRepository
import com.cricket.scorer.data.repository.CricketScoringCalculator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ScoringViewModel(private val repository: CricketRepository) : ViewModel() {

    private val _liveMatchState = MutableStateFlow<LiveMatchState?>(null)
    val liveMatchState: StateFlow<LiveMatchState?> = _liveMatchState.asStateFlow()

    private val _showWicketDialog = MutableStateFlow(false)
    val showWicketDialog: StateFlow<Boolean> = _showWicketDialog.asStateFlow()

    private val _showBowlerSelection = MutableStateFlow(false)
    val showBowlerSelection: StateFlow<Boolean> = _showBowlerSelection.asStateFlow()

    private val _showBatsmanSelection = MutableStateFlow(false)
    val showBatsmanSelection: StateFlow<Boolean> = _showBatsmanSelection.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    var selectNewBatsmanForStriker = true
        private set

    // Redo stack for the last 6 deliveries
    private val redoStack = mutableListOf<DeliveryEntity>()

    init {
        loadActiveMatch()
    }

    fun loadActiveMatch() {
        viewModelScope.launch {
            try {
                val match = repository.getLiveMatch()
                if (match != null) {
                    val innings = repository.getInningsForMatch(match.id)
                    val activeInning = innings.firstOrNull { !it.isCompleted } 
                        ?: innings.lastOrNull() // If all innings completed, load the last one
                    
                    if (activeInning != null) {
                        refreshState(match, activeInning)
                    } else {
                        _liveMatchState.value = null
                    }
                } else {
                    _liveMatchState.value = null
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load active match: ${e.localizedMessage}"
            }
        }
    }

    private suspend fun refreshState(match: MatchEntity, inning: InningEntity) {
        val deliveries = repository.getDeliveriesForInning(inning.id)
        val players = repository.getAllPlayersFlow().first().associateBy { it.id }
        
        val firstInning = repository.getInningsForMatch(match.id).firstOrNull { it.inningNumber == 1 }
        val firstInningRuns = if (firstInning != null && inning.inningNumber == 2) {
            repository.getDeliveriesForInning(firstInning.id).sumOf { it.runsScored + it.extrasRuns }
        } else {
            null
        }

        val state = CricketScoringCalculator.calculateLiveMatchState(
            match = match,
            firstInning = firstInning,
            secondInning = if (inning.inningNumber == 2) inning else null,
            currentInning = inning,
            deliveries = deliveries,
            players = players,
            firstInningTotalRuns = firstInningRuns
        )
        _liveMatchState.value = state

        // Check if we need to show prompts
        if (state.striker == null || state.nonStriker == null) {
            // Check if there are wickets left to bat
            val currentWickets = state.totalWickets
            val totalPlayers = match.teamARoster.split(",").size // assume standard match size
            if (currentWickets < totalPlayers - 1 && !inning.isCompleted) {
                selectNewBatsmanForStriker = state.striker == null
                _showBatsmanSelection.value = true
            }
        } else if (state.bowler == null && !inning.isCompleted) {
            _showBowlerSelection.value = true
        }
    }

    fun setupMatch(
        teamA: String,
        teamB: String,
        overs: Int,
        matchType: String,
        tossWinner: String,
        tossDecision: String,
        teamARoster: List<String>,
        teamBRoster: List<String>
    ) {
        viewModelScope.launch {
            try {
                // Ensure all players are registered in DB
                teamARoster.forEach { repository.getOrCreatePlayer(it) }
                teamBRoster.forEach { repository.getOrCreatePlayer(it) }

                val match = MatchEntity(
                    teamAName = teamA.trim(),
                    teamBName = teamB.trim(),
                    oversCount = overs,
                    matchType = matchType,
                    tossWinner = tossWinner.trim(),
                    tossDecision = tossDecision,
                    status = "LIVE",
                    teamARoster = teamARoster.joinToString(","),
                    teamBRoster = teamBRoster.joinToString(",")
                )
                val matchId = repository.insertMatch(match)

                // Determine batting and bowling teams
                val (battingTeam, bowlingTeam) = if (tossWinner == teamA) {
                    if (tossDecision == "BAT") Pair(teamA, teamB) else Pair(teamB, teamA)
                } else {
                    if (tossDecision == "BAT") Pair(teamB, teamA) else Pair(teamA, teamB)
                }

                val inning = InningEntity(
                    matchId = matchId,
                    battingTeamName = battingTeam,
                    bowlingTeamName = bowlingTeam,
                    inningNumber = 1,
                    isCompleted = false
                )
                val inningId = repository.insertInning(inning)

                val createdMatch = match.copy(id = matchId)
                val createdInning = inning.copy(id = inningId)
                
                redoStack.clear()
                refreshState(createdMatch, createdInning)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to setup match: ${e.localizedMessage}"
            }
        }
    }

    fun selectOpeners(strikerName: String, nonStrikerName: String, bowlerName: String) {
        val state = _liveMatchState.value ?: return
        viewModelScope.launch {
            try {
                val striker = repository.getOrCreatePlayer(strikerName)
                val nonStriker = repository.getOrCreatePlayer(nonStrikerName)
                val bowler = repository.getOrCreatePlayer(bowlerName)

                val updatedInning = state.currentInning.copy(
                    currentStrikerId = striker.id,
                    currentNonStrikerId = nonStriker.id,
                    currentBowlerId = bowler.id
                )
                repository.updateInning(updatedInning)

                // Create first over
                val over = OverEntity(
                    inningId = state.currentInning.id,
                    overNumber = 0,
                    bowlerId = bowler.id,
                    isCompleted = false
                )
                repository.insertOver(over)

                _showBatsmanSelection.value = false
                _showBowlerSelection.value = false
                refreshState(state.match, updatedInning)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to select openers: ${e.localizedMessage}"
            }
        }
    }

    fun selectNewBatsman(name: String) {
        val state = _liveMatchState.value ?: return
        viewModelScope.launch {
            try {
                val player = repository.getOrCreatePlayer(name)
                val updatedInning = if (selectNewBatsmanForStriker) {
                    state.currentInning.copy(currentStrikerId = player.id)
                } else {
                    state.currentInning.copy(currentNonStrikerId = player.id)
                }
                repository.updateInning(updatedInning)
                _showBatsmanSelection.value = false
                refreshState(state.match, updatedInning)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to set batsman: ${e.localizedMessage}"
            }
        }
    }

    fun selectNewBowler(name: String) {
        val state = _liveMatchState.value ?: return
        viewModelScope.launch {
            try {
                val bowler = repository.getOrCreatePlayer(name)
                val updatedInning = state.currentInning.copy(currentBowlerId = bowler.id)
                repository.updateInning(updatedInning)

                // Find completed overs to determine new over number
                val overs = repository.getOversForInning(state.currentInning.id)
                val newOverNumber = overs.size

                val over = OverEntity(
                    inningId = state.currentInning.id,
                    overNumber = newOverNumber,
                    bowlerId = bowler.id,
                    isCompleted = false
                )
                repository.insertOver(over)

                _showBowlerSelection.value = false
                refreshState(state.match, updatedInning)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to set bowler: ${e.localizedMessage}"
            }
        }
    }

    fun recordNormalDelivery(runs: Int, extraType: String) {
        val state = _liveMatchState.value ?: return
        val strikerId = state.currentInning.currentStrikerId ?: return
        val nonStrikerId = state.currentInning.currentNonStrikerId ?: return
        val bowlerId = state.currentInning.currentBowlerId ?: return

        viewModelScope.launch {
            try {
                val activeOver = repository.getActiveOver(state.currentInning.id) 
                    ?: OverEntity(inningId = state.currentInning.id, overNumber = 0, bowlerId = bowlerId)
                val overId = if (activeOver.id == 0L) repository.insertOver(activeOver) else activeOver.id

                val extrasRuns = when (extraType) {
                    "WIDE", "NO_BALL" -> 1
                    "BYE", "LEG_BYE" -> runs
                    else -> 0
                }
                val runsScored = if (extraType == "BYE" || extraType == "LEG_BYE" || extraType == "WIDE") 0 else runs

                val delivery = DeliveryEntity(
                    inningId = state.currentInning.id,
                    overId = overId,
                    strikerId = strikerId,
                    nonStrikerId = nonStrikerId,
                    bowlerId = bowlerId,
                    runsScored = runsScored,
                    extrasRuns = extrasRuns,
                    extraType = extraType,
                    isWicket = false
                )
                repository.insertDelivery(delivery)
                redoStack.clear()

                processRotationAndTransitions(state, delivery, runsScored, extraType, extrasRuns)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to record delivery: ${e.localizedMessage}"
            }
        }
    }

    fun prepareWicketRecording() {
        _showWicketDialog.value = true
    }

    fun dismissWicketDialog() {
        _showWicketDialog.value = false
    }

    fun recordWicket(
        wicketType: String,
        dismissedPlayerId: Long,
        assistingPlayerName: String?,
        extraType: String = "NONE"
    ) {
        val state = _liveMatchState.value ?: return
        val strikerId = state.currentInning.currentStrikerId ?: return
        val nonStrikerId = state.currentInning.currentNonStrikerId ?: return
        val bowlerId = state.currentInning.currentBowlerId ?: return

        viewModelScope.launch {
            try {
                _showWicketDialog.value = false
                val activeOver = repository.getActiveOver(state.currentInning.id) ?: return@launch
                val assistingPlayer = if (!assistingPlayerName.isNullOrBlank()) {
                    repository.getOrCreatePlayer(assistingPlayerName)
                } else null

                // Wide/No-ball penalty runs check for run out on extras
                val extrasRuns = if (extraType == "WIDE" || extraType == "NO_BALL") 1 else 0

                val delivery = DeliveryEntity(
                    inningId = state.currentInning.id,
                    overId = activeOver.id,
                    strikerId = strikerId,
                    nonStrikerId = nonStrikerId,
                    bowlerId = bowlerId,
                    runsScored = 0,
                    extrasRuns = extrasRuns,
                    extraType = extraType,
                    isWicket = true,
                    wicketType = wicketType,
                    dismissedPlayerId = dismissedPlayerId,
                    assistingPlayerId = assistingPlayer?.id
                )
                repository.insertDelivery(delivery)
                redoStack.clear()

                // Mark dismissed batsman as null in inning
                var updatedInning = state.currentInning
                if (dismissedPlayerId == strikerId) {
                    updatedInning = updatedInning.copy(currentStrikerId = null)
                } else if (dismissedPlayerId == nonStrikerId) {
                    updatedInning = updatedInning.copy(currentNonStrikerId = null)
                }
                repository.updateInning(updatedInning)

                processRotationAndTransitions(state, delivery, 0, extraType, extrasRuns, isWicket = true)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to record wicket: ${e.localizedMessage}"
            }
        }
    }

    private suspend fun processRotationAndTransitions(
        state: LiveMatchState,
        delivery: DeliveryEntity,
        runsScored: Int,
        extraType: String,
        extrasRuns: Int,
        isWicket: Boolean = false
    ) {
        var inning = repository.getInningsForMatch(state.match.id).first { it.id == state.currentInning.id }
        
        // 1. Strike Rotation based on runs run
        val runsRun = when (extraType) {
            "NONE", "NO_BALL" -> runsScored
            "WIDE" -> extrasRuns - 1
            "BYE", "LEG_BYE" -> extrasRuns
            else -> 0
        }
        val shouldSwap = runsRun % 2 != 0

        var currentStrikerId = inning.currentStrikerId
        var currentNonStrikerId = inning.currentNonStrikerId

        if (shouldSwap && currentStrikerId != null && currentNonStrikerId != null) {
            val temp = currentStrikerId
            currentStrikerId = currentNonStrikerId
            currentNonStrikerId = temp
            inning = inning.copy(
                currentStrikerId = currentStrikerId,
                currentNonStrikerId = currentNonStrikerId
            )
            repository.updateInning(inning)
        }

        // Fetch deliveries in inning to evaluate over completions
        val deliveries = repository.getDeliveriesForInning(inning.id)
        val legalBalls = deliveries.count { it.extraType != "WIDE" && it.extraType != "NO_BALL" }

        // 2. Over Completion Check
        val overCompleted = legalBalls > 0 && legalBalls % 6 == 0 && (delivery.extraType != "WIDE" && delivery.extraType != "NO_BALL")
        if (overCompleted) {
            val activeOver = repository.getActiveOver(inning.id)
            if (activeOver != null) {
                repository.updateOver(activeOver.copy(isCompleted = true))
            }
            
            // Strike changes at end of over (swap ends)
            if (currentStrikerId != null && currentNonStrikerId != null) {
                val temp = currentStrikerId
                currentStrikerId = currentNonStrikerId
                currentNonStrikerId = temp
            }

            inning = inning.copy(
                currentStrikerId = currentStrikerId,
                currentNonStrikerId = currentNonStrikerId,
                currentBowlerId = null // trigger bowler prompt
            )
            repository.updateInning(inning)
        }

        // 3. Inning Completion Check
        val rosterSize = state.match.teamARoster.split(",").size
        val wicketsLimit = rosterSize - 1
        val oversLimit = state.match.oversCount
        
        val totalRuns = deliveries.sumOf { it.runsScored + it.extrasRuns }
        val totalWickets = deliveries.count { it.isWicket }
        
        var inningCompleted = false
        if (totalWickets >= wicketsLimit) {
            inningCompleted = true
        } else if (legalBalls >= oversLimit * 6) {
            inningCompleted = true
        } else if (inning.inningNumber == 2 && state.target != null && totalRuns >= state.target) {
            inningCompleted = true
        }

        if (inningCompleted) {
            repository.updateInning(inning.copy(isCompleted = true, currentStrikerId = null, currentNonStrikerId = null, currentBowlerId = null))
            
            if (inning.inningNumber == 1) {
                // Start 2nd Inning
                val secondInning = InningEntity(
                    matchId = state.match.id,
                    battingTeamName = inning.bowlingTeamName,
                    bowlingTeamName = inning.battingTeamName,
                    inningNumber = 2,
                    isCompleted = false
                )
                repository.insertInning(secondInning)
                loadActiveMatch()
            } else {
                // Complete Match
                val firstInning = repository.getInningsForMatch(state.match.id).first { it.inningNumber == 1 }
                val firstInningRuns = repository.getDeliveriesForInning(firstInning.id).sumOf { it.runsScored + it.extrasRuns }
                
                val winner = when {
                    totalRuns > firstInningRuns -> inning.battingTeamName
                    totalRuns < firstInningRuns -> inning.bowlingTeamName
                    else -> "Tie"
                }

                repository.updateMatch(
                    state.match.copy(
                        status = "COMPLETED",
                        winnerTeamName = winner
                    )
                )
                loadActiveMatch()
            }
        } else {
            refreshState(state.match, inning)
        }
    }

    fun undoLastDelivery() {
        val state = _liveMatchState.value ?: return
        viewModelScope.launch {
            try {
                val deliveries = repository.getDeliveriesForInning(state.currentInning.id)
                if (deliveries.isEmpty()) return@launch

                val lastDelivery = deliveries.last()
                repository.deleteDeliveryById(lastDelivery.id)
                redoStack.add(lastDelivery)

                // Recalculate striker rotation from remaining deliveries to restore positions exactly
                reconstructPositionsFromDeliveries(state.match, state.currentInning)
            } catch (e: Exception) {
                _errorMessage.value = "Undo failed: ${e.localizedMessage}"
            }
        }
    }

    fun redoLastDelivery() {
        val state = _liveMatchState.value ?: return
        if (redoStack.isEmpty()) return
        
        viewModelScope.launch {
            try {
                val deliveryToReinsert = redoStack.removeLast()
                repository.insertDelivery(deliveryToReinsert)

                // Recalculate positions
                reconstructPositionsFromDeliveries(state.match, state.currentInning)
            } catch (e: Exception) {
                _errorMessage.value = "Redo failed: ${e.localizedMessage}"
            }
        }
    }

    private suspend fun reconstructPositionsFromDeliveries(match: MatchEntity, inning: InningEntity) {
        val deliveries = repository.getDeliveriesForInning(inning.id)
        val overs = repository.getOversForInning(inning.id)

        if (deliveries.isEmpty()) {
            // Revert back to openers setup
            // Get opener bowler from the first over
            val firstOver = overs.firstOrNull()
            val resetInning = inning.copy(
                currentStrikerId = firstOver?.let { getFirstDeliveryStriker(inning.id) },
                currentNonStrikerId = firstOver?.let { getFirstDeliveryNonStriker(inning.id) },
                currentBowlerId = firstOver?.bowlerId
            )
            repository.updateInning(resetInning)
            
            // Mark all overs as incomplete
            overs.forEach { repository.updateOver(it.copy(isCompleted = false)) }
            
            refreshState(match, resetInning)
            return
        }

        // Trace from the openers (first delivery values)
        val firstDel = deliveries.first()
        var strikerId = firstDel.strikerId
        var nonStrikerId = firstDel.nonStrikerId
        var bowlerId = firstDel.bowlerId

        // Active over tracker
        var currentOverId = firstDel.overId
        var currentOverNumber = 0

        // Reset all overs completeness to match the delivery timeline
        val overStatusMap = mutableMapOf<Long, Boolean>()
        
        for (i in deliveries.indices) {
            val d = deliveries[i]
            
            // Handle wicket falling
            if (d.isWicket && d.dismissedPlayerId != null) {
                if (d.dismissedPlayerId == strikerId) {
                    // Striker got out. In subsequent deliveries, did we get a new striker?
                    // Let's scan forward to see the next striker ID that is not nonStrikerId
                    val nextDeliveryWithNewStriker = deliveries.subList(i + 1, deliveries.size)
                        .firstOrNull { it.strikerId != nonStrikerId && it.strikerId != strikerId }
                    strikerId = nextDeliveryWithNewStriker?.strikerId ?: 0L // if 0L, means needs prompt
                } else if (d.dismissedPlayerId == nonStrikerId) {
                    val nextDeliveryWithNewNonStriker = deliveries.subList(i + 1, deliveries.size)
                        .firstOrNull { it.strikerId != strikerId && it.strikerId != nonStrikerId }
                    nonStrikerId = nextDeliveryWithNewNonStriker?.strikerId ?: 0L
                }
            }

            // Striker rotation on runs
            val extraType = d.extraType
            val runsScored = d.runsScored
            val extrasRuns = d.extrasRuns
            
            val runsRun = when (extraType) {
                "NONE", "NO_BALL" -> runsScored
                "WIDE" -> extrasRuns - 1
                "BYE", "LEG_BYE" -> extrasRuns
                else -> 0
            }
            if (runsRun % 2 != 0 && strikerId != 0L && nonStrikerId != 0L) {
                val temp = strikerId
                strikerId = nonStrikerId
                nonStrikerId = temp
            }

            // End of over check
            val inningDelsUpToNow = deliveries.subList(0, i + 1)
            val legalBallsUpToNow = inningDelsUpToNow.count { it.extraType != "WIDE" && it.extraType != "NO_BALL" }
            
            val overCompleted = legalBallsUpToNow > 0 && legalBallsUpToNow % 6 == 0 && (d.extraType != "WIDE" && d.extraType != "NO_BALL")
            if (overCompleted) {
                overStatusMap[d.overId] = true
                if (strikerId != 0L && nonStrikerId != 0L) {
                    val temp = strikerId
                    strikerId = nonStrikerId
                    nonStrikerId = temp
                }
                
                // Bowler for next ball
                val nextDel = deliveries.getOrNull(i + 1)
                bowlerId = nextDel?.bowlerId ?: 0L // 0L means select bowler prompt
            } else {
                overStatusMap[d.overId] = false
                bowlerId = d.bowlerId
            }
        }

        // Apply calculated statuses to overs in DB
        overs.forEach { ov ->
            val isCompleted = overStatusMap[ov.id] ?: false
            repository.updateOver(ov.copy(isCompleted = isCompleted))
        }

        val updatedInning = inning.copy(
            currentStrikerId = if (strikerId == 0L) null else strikerId,
            currentNonStrikerId = if (nonStrikerId == 0L) null else nonStrikerId,
            currentBowlerId = if (bowlerId == 0L) null else bowlerId
        )
        repository.updateInning(updatedInning)
        refreshState(match, updatedInning)
    }

    private suspend fun getFirstDeliveryStriker(inningId: Long): Long? {
        val dels = repository.getDeliveriesForInning(inningId)
        return dels.firstOrNull()?.strikerId
    }

    private suspend fun getFirstDeliveryNonStriker(inningId: Long): Long? {
        val dels = repository.getDeliveriesForInning(inningId)
        return dels.firstOrNull()?.nonStrikerId
    }

    fun endMatchManually() {
        val state = _liveMatchState.value ?: return
        viewModelScope.launch {
            try {
                val firstInning = repository.getInningsForMatch(state.match.id).first { it.inningNumber == 1 }
                val secondInning = repository.getInningsForMatch(state.match.id).firstOrNull { it.inningNumber == 2 }
                
                val firstInningRuns = repository.getDeliveriesForInning(firstInning.id).sumOf { it.runsScored + it.extrasRuns }
                val secondInningRuns = secondInning?.let { repository.getDeliveriesForInning(it.id).sumOf { it.runsScored + it.extrasRuns } } ?: 0

                val winner = when {
                    secondInning == null -> firstInning.battingTeamName
                    secondInningRuns > firstInningRuns -> secondInning.battingTeamName
                    secondInningRuns < firstInningRuns -> firstInning.battingTeamName
                    else -> "Tie"
                }

                repository.updateInning(state.currentInning.copy(isCompleted = true))
                repository.updateMatch(
                    state.match.copy(
                        status = "COMPLETED",
                        winnerTeamName = winner
                    )
                )
                loadActiveMatch()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to end match: ${e.localizedMessage}"
            }
        }
    }
}
