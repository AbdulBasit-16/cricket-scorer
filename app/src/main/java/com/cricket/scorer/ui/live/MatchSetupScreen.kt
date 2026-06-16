package com.cricket.scorer.ui.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cricket.scorer.viewmodel.ScoringViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchSetupScreen(
    viewModel: ScoringViewModel,
    onMatchInitialized: () -> Unit,
    onBack: () -> Unit
) {
    var teamA by remember { mutableStateOf("Team Alpha") }
    var teamB by remember { mutableStateOf("Team Beta") }
    
    var matchType by remember { mutableStateOf("T20") } // T20, ODI, CUSTOM
    var oversCount by remember { mutableStateOf("20") }
    
    var tossWinner by remember { mutableStateOf("Team Alpha") }
    var tossDecision by remember { mutableStateOf("BAT") } // BAT, BOWL
    
    // Preset rosters
    var teamARosterText by remember { 
        mutableStateOf("Alpha A1, Alpha A2, Alpha A3, Alpha A4, Alpha A5, Alpha A6, Alpha A7, Alpha A8, Alpha A9, Alpha A10, Alpha A11") 
    }
    var teamBRosterText by remember { 
        mutableStateOf("Beta B1, Beta B2, Beta B3, Beta B4, Beta B5, Beta B6, Beta B7, Beta B8, Beta B9, Beta B10, Beta B11") 
    }

    var showErrorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(teamA, teamB) {
        if (tossWinner != teamA && tossWinner != teamB) {
            tossWinner = teamA
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure Match") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back", color = MaterialTheme.colorScheme.onPrimary)
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
            // Team configurations
            Text("Teams Configuration", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            
            OutlinedTextField(
                value = teamA,
                onValueChange = { teamA = it },
                label = { Text("Team A Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = teamB,
                onValueChange = { teamB = it },
                label = { Text("Team B Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Divider()

            // Match structure
            Text("Match Format", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // T20 Option
                FilterChip(
                    selected = matchType == "T20",
                    onClick = { 
                        matchType = "T20"
                        oversCount = "20"
                    },
                    label = { Text("T20 (20 Overs)") },
                    modifier = Modifier.weight(1f)
                )
                // ODI Option
                FilterChip(
                    selected = matchType == "ODI",
                    onClick = { 
                        matchType = "ODI"
                        oversCount = "50"
                    },
                    label = { Text("ODI (50 Overs)") },
                    modifier = Modifier.weight(1f)
                )
                // Custom Option
                FilterChip(
                    selected = matchType == "CUSTOM",
                    onClick = { matchType = "CUSTOM" },
                    label = { Text("Custom") },
                    modifier = Modifier.weight(1f)
                )
            }

            if (matchType == "CUSTOM") {
                OutlinedTextField(
                    value = oversCount,
                    onValueChange = { oversCount = it.filter { char -> char.isDigit() } },
                    label = { Text("Overs Count") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Divider()

            // Toss Selection
            Text("Toss Setup", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Who won the toss? ", modifier = Modifier.weight(1.2f))
                Row(
                    modifier = Modifier.weight(2f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ElevatedButton(
                        onClick = { tossWinner = teamA },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (tossWinner == teamA) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(teamA, maxLines = 1)
                    }
                    ElevatedButton(
                        onClick = { tossWinner = teamB },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (tossWinner == teamB) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(teamB, maxLines = 1)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Toss Decision: ", modifier = Modifier.weight(1.2f))
                Row(
                    modifier = Modifier.weight(2f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ElevatedButton(
                        onClick = { tossDecision = "BAT" },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (tossDecision == "BAT") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("BAT FIRST")
                    }
                    ElevatedButton(
                        onClick = { tossDecision = "BOWL" },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (tossDecision == "BOWL") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("BOWL FIRST")
                    }
                }
            }

            Divider()

            // Squad rosters
            Text("Squad Rosters (Comma Separated Names)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = teamARosterText,
                onValueChange = { teamARosterText = it },
                label = { Text("$teamA Squad") },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = teamBRosterText,
                onValueChange = { teamBRosterText = it },
                label = { Text("$teamB Squad") },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            showErrorMsg?.let { msg ->
                Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = {
                    val listA = teamARosterText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    val listB = teamBRosterText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    val overs = oversCount.toIntOrNull() ?: 20

                    when {
                        teamA.isBlank() || teamB.isBlank() -> {
                            showErrorMsg = "Team names cannot be blank"
                        }
                        teamA.trim() == teamB.trim() -> {
                            showErrorMsg = "Team names must be different"
                        }
                        overs <= 0 -> {
                            showErrorMsg = "Overs count must be greater than 0"
                        }
                        listA.size < 2 || listB.size < 2 -> {
                            showErrorMsg = "Each team must have at least 2 players"
                        }
                        else -> {
                            viewModel.setupMatch(
                                teamA = teamA,
                                teamB = teamB,
                                overs = overs,
                                matchType = matchType,
                                tossWinner = tossWinner,
                                tossDecision = tossDecision,
                                teamARoster = listA,
                                teamBRoster = listB
                            )
                            onMatchInitialized()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Start Scoring Match", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
