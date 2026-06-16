package com.cricket.scorer.ui.live

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cricket.scorer.data.database.entities.DeliveryEntity
import com.cricket.scorer.data.database.entities.PlayerEntity
import com.cricket.scorer.viewmodel.ScoringViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScorerScreen(
    viewModel: ScoringViewModel,
    onNavigateToDashboard: () -> Unit
) {
    val state by viewModel.liveMatchState.collectAsState()
    val showWicketDialog by viewModel.showWicketDialog.collectAsState()
    val showBowlerSelection by viewModel.showBowlerSelection.collectAsState()
    val showBatsmanSelection by viewModel.showBatsmanSelection.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Dialog state for wickets
    var selectedWicketType by remember { mutableStateOf("BOWLED") }
    var selectedDismissedPlayerId by remember { mutableStateOf(0L) }
    var assistingFielderName by remember { mutableStateOf("") }

    // Dialog selection states
    var selectedStrikerName by remember { mutableStateOf("") }
    var selectedNonStrikerName by remember { mutableStateOf("") }
    var selectedBowlerName by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        state?.let {
            if (selectedDismissedPlayerId == 0L) {
                selectedDismissedPlayerId = it.currentInning.currentStrikerId ?: 0L
            }
        }
    }

    if (state == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val match = state!!.match
    val inning = state!!.currentInning
    val battingRoster = (if (inning.battingTeamName == match.teamAName) match.teamARoster else match.teamBRoster)
        .split(",").map { it.trim() }
    val bowlingRoster = (if (inning.bowlingTeamName == match.teamAName) match.teamARoster else match.teamBRoster)
        .split(",").map { it.trim() }

    // Determine players who have not batted yet in this inning
    val deliveries = state!!.recentDeliveries
    val dismissedIds = state!!.recentDeliveries.filter { it.isWicket && it.dismissedPlayerId != null }.map { it.dismissedPlayerId!! }.toSet()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${inning.battingTeamName} Innings", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { viewModel.endMatchManually() }) {
                        Text("End Match", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = onNavigateToDashboard) {
                        Text("Exit", color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error Message Banner
            errorMessage?.let { error ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        error, 
                        color = MaterialTheme.colorScheme.onErrorContainer, 
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // 1. Scoring Board Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${inning.battingTeamName} bats vs ${inning.bowlingTeamName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${state!!.totalRuns}/${state!!.totalWickets}",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${state!!.completedOvers}.${state!!.currentOverBalls} ov)",
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Runs needed / Target
                    if (inning.inningNumber == 2 && state!!.target != null) {
                        val runsNeeded = state!!.target!! - state!!.totalRuns
                        val totalBalls = match.oversCount * 6
                        val ballsRemaining = totalMatchBalls(match) - state!!.totalBalls
                        
                        Text(
                            text = if (runsNeeded <= 0) "Innings Completed" else "Need $runsNeeded runs from $ballsRemaining balls",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Text("Target: ${state!!.target}", style = MaterialTheme.typography.bodyMedium)
                            Text("RRR: ${String.format(Locale.getDefault(), "%.2f", state!!.rrr ?: 0.0)}", style = MaterialTheme.typography.bodyMedium)
                            Text("CRR: ${String.format(Locale.getDefault(), "%.2f", state!!.crr)}", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Text("CRR: ${String.format(Locale.getDefault(), "%.2f", state!!.crr)}", style = MaterialTheme.typography.bodyMedium)
                            Text("Partnership: ${state!!.partnershipRuns} (${state!!.partnershipBalls}b)", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // 2. Active Batsmen & Bowler Lists
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Batting", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Divider()
                    
                    // Striker Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${state!!.striker?.name ?: "No Striker"}*",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(2f)
                        )
                        Row(
                            modifier = Modifier.weight(3f),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${state!!.strikerRuns} runs", textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                            Text("${state!!.strikerBalls} balls", textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                            Text("${state!!.strikerFours}x4", textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                            Text("${state!!.strikerSixes}x6", textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                        }
                    }

                    // Non-Striker Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = state!!.nonStriker?.name ?: "No Non-Striker",
                            modifier = Modifier.weight(2f)
                        )
                        Row(
                            modifier = Modifier.weight(3f),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${state!!.nonStrikerRuns} runs", textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                            Text("${state!!.nonStrikerBalls} balls", textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                            Text("${state!!.nonStrikerFours}x4", textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                            Text("${state!!.nonStrikerSixes}x6", textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Bowling", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Divider()

                    // Bowler Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = state!!.bowler?.name ?: "No Bowler",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(2f)
                        )
                        val bowlerOvers = (state!!.bowlerBallsBowled / 6) + (state!!.bowlerBallsBowled % 6) / 10.0
                        Row(
                            modifier = Modifier.weight(3f),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("$bowlerOvers ov", textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                            Text("${state!!.bowlerMaidens} md", textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                            Text("${state!!.bowlerRunsConceded} runs", textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                            Text("${state!!.bowlerWickets} wkts", textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // 3. Current Over Log View
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "This Over: ", 
                    style = MaterialTheme.typography.bodyMedium, 
                    fontWeight = FontWeight.Bold
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    items(state!!.currentOverDeliveries) { del ->
                        val text = getDeliveryText(del)
                        val bgColor = when {
                            del.isWicket -> Color(0xFFC0392B)
                            del.runsScored >= 4 -> Color(0xFF27AE60)
                            del.extraType != "NONE" -> Color(0xFFD35400)
                            else -> Color(0xFF7F8C8D)
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(bgColor, RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // 4. Scoring Grid
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Runs row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ScoreButton(text = "0", modifier = Modifier.weight(1f)) { viewModel.recordNormalDelivery(0, "NONE") }
                        ScoreButton(text = "1", modifier = Modifier.weight(1f)) { viewModel.recordNormalDelivery(1, "NONE") }
                        ScoreButton(text = "2", modifier = Modifier.weight(1f)) { viewModel.recordNormalDelivery(2, "NONE") }
                        ScoreButton(text = "3", modifier = Modifier.weight(1f)) { viewModel.recordNormalDelivery(3, "NONE") }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ScoreButton(text = "4", modifier = Modifier.weight(1f)) { viewModel.recordNormalDelivery(4, "NONE") }
                        ScoreButton(text = "6", modifier = Modifier.weight(1f)) { viewModel.recordNormalDelivery(6, "NONE") }
                        ScoreButton(text = "Wd", description = "+1 Wide", modifier = Modifier.weight(1f)) { viewModel.recordNormalDelivery(0, "WIDE") }
                        ScoreButton(text = "Nb", description = "+1 NoBall", modifier = Modifier.weight(1f)) { viewModel.recordNormalDelivery(0, "NO_BALL") }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ScoreButton(text = "Bye", modifier = Modifier.weight(1f)) { viewModel.recordNormalDelivery(1, "BYE") }
                        ScoreButton(text = "Lb", description = "LegBye", modifier = Modifier.weight(1f)) { viewModel.recordNormalDelivery(1, "LEG_BYE") }
                        
                        // Wicket button (Red Accent)
                        Button(
                            onClick = { viewModel.prepareWicketRecording() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC0392B)),
                            modifier = Modifier
                                .weight(2f)
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("WICKET", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    
                    Divider()
                    
                    // Undo Redo Panel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        IconButton(onClick = { viewModel.undoLastDelivery() }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Undo, contentDescription = "Undo")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Undo")
                            }
                        }
                        IconButton(onClick = { viewModel.redoLastDelivery() }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Redo, contentDescription = "Redo")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Redo")
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs Setup

    // 1. Initial Openers Selection Dialog
    if (state!!.striker == null && state!!.nonStriker == null && state!!.bowler == null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Select Openers") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select Opening Striker")
                    TextField(
                        value = selectedStrikerName,
                        onValueChange = { selectedStrikerName = it },
                        placeholder = { Text("Striker Name") }
                    )

                    Text("Select Opening Non-Striker")
                    TextField(
                        value = selectedNonStrikerName,
                        onValueChange = { selectedNonStrikerName = it },
                        placeholder = { Text("Non-Striker Name") }
                    )

                    Text("Select Opening Bowler")
                    TextField(
                        value = selectedBowlerName,
                        onValueChange = { selectedBowlerName = it },
                        placeholder = { Text("Bowler Name") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedStrikerName.isNotBlank() && selectedNonStrikerName.isNotBlank() && selectedBowlerName.isNotBlank()) {
                            viewModel.selectOpeners(selectedStrikerName, selectedNonStrikerName, selectedBowlerName)
                        }
                    }
                ) {
                    Text("Ready")
                }
            }
        )
    }

    // 2. Wicket dialog
    if (showWicketDialog) {
        val strikerId = state!!.currentInning.currentStrikerId ?: 0L
        val nonStrikerId = state!!.currentInning.currentNonStrikerId ?: 0L

        AlertDialog(
            onDismissRequest = { viewModel.dismissWicketDialog() },
            title = { Text("Wicket Fall") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text("Dismissal Type")
                    val dismissalTypes = listOf("BOWLED", "CAUGHT", "LBW", "RUN_OUT", "STUMPED", "HIT_WICKET", "RETIRED")
                    
                    var expandedWType by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { expandedWType = true }) {
                            Text(selectedWicketType)
                        }
                        DropdownMenu(expanded = expandedWType, onDismissRequest = { expandedWType = false }) {
                            dismissalTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        selectedWicketType = type
                                        expandedWType = false
                                    }
                                )
                            }
                        }
                    }

                    Text("Dismissed Player")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ElevatedButton(
                            onClick = { selectedDismissedPlayerId = strikerId },
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = if (selectedDismissedPlayerId == strikerId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(state!!.striker?.name ?: "Striker")
                        }

                        ElevatedButton(
                            onClick = { selectedDismissedPlayerId = nonStrikerId },
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = if (selectedDismissedPlayerId == nonStrikerId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(state!!.nonStriker?.name ?: "Non-Striker")
                        }
                    }

                    if (selectedWicketType == "CAUGHT" || selectedWicketType == "RUN_OUT" || selectedWicketType == "STUMPED") {
                        OutlinedTextField(
                            value = assistingFielderName,
                            onValueChange = { assistingFielderName = it },
                            label = { Text("Fielder Name (Optional)") }
                        )
                    }

                    // Optional Wide/No ball flag for run outs
                    if (selectedWicketType == "RUN_OUT") {
                        Text("Was the ball a wide or no-ball?")
                        // Let's support normal/wide/no ball selection
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.recordWicket(
                            wicketType = selectedWicketType,
                            dismissedPlayerId = selectedDismissedPlayerId,
                            assistingPlayerName = assistingFielderName.trim()
                        )
                        // Reset local dialog state
                        assistingFielderName = ""
                        selectedWicketType = "BOWLED"
                    }
                ) {
                    Text("Record Wicket")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissWicketDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 3. New Batsman Selection Dialog
    if (showBatsmanSelection) {
        var newBatterName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {},
            title = { Text(if (viewModel.selectNewBatsmanForStriker) "Select Striker" else "Select Non-Striker") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select batsman from roster or enter new name:")
                    OutlinedTextField(
                        value = newBatterName,
                        onValueChange = { newBatterName = it },
                        label = { Text("Batsman Name") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newBatterName.isNotBlank()) {
                            viewModel.selectNewBatsman(newBatterName)
                            newBatterName = ""
                        }
                    }
                ) {
                    Text("Confirm")
                }
            }
        )
    }

    // 4. New Bowler Selection Dialog
    if (showBowlerSelection) {
        var newBowlerName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Select Bowler") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select bowler for the next over:")
                    OutlinedTextField(
                        value = newBowlerName,
                        onValueChange = { newBowlerName = it },
                        label = { Text("Bowler Name") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newBowlerName.isNotBlank()) {
                            viewModel.selectNewBowler(newBowlerName)
                            newBowlerName = ""
                        }
                    }
                ) {
                    Text("Confirm")
                }
            }
        )
    }
}

@Composable
fun ScoreButton(
    text: String,
    description: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (!description.isNullOrBlank()) {
                Text(description, fontSize = 9.sp, color = Color.Gray)
            }
        }
    }
}

private fun getDeliveryText(delivery: DeliveryEntity): String {
    return when {
        delivery.isWicket -> "W"
        delivery.extraType == "WIDE" -> "Wd"
        delivery.extraType == "NO_BALL" -> "Nb"
        delivery.extraType == "BYE" -> "${delivery.extrasRuns}B"
        delivery.extraType == "LEG_BYE" -> "${delivery.extrasRuns}Lb"
        else -> delivery.runsScored.toString()
    }
}

private fun totalMatchBalls(match: com.cricket.scorer.data.database.entities.MatchEntity): Int {
    return match.oversCount * 6
}
