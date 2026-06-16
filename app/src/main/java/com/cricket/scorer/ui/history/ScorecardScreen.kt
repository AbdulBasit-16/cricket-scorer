package com.cricket.scorer.ui.history

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cricket.scorer.data.model.InningScorecard
import com.cricket.scorer.data.model.MatchScorecard
import com.cricket.scorer.viewmodel.HistoryViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScorecardScreen(
    matchId: Long,
    viewModel: HistoryViewModel,
    onBack: () -> Unit
) {
    val scorecard by viewModel.selectedScorecard.collectAsState()

    LaunchedEffect(matchId) {
        viewModel.selectMatch(matchId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Match Scorecard") },
                navigationIcon = {
                    TextButton(onClick = {
                        viewModel.clearSelectedScorecard()
                        onBack()
                    }) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (scorecard == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                ScorecardTabs(scorecard = scorecard!!)
            }
        }
    }
}

@Composable
fun ScorecardTabs(scorecard: MatchScorecard) {
    var selectedTabState by remember { mutableStateOf(0) }
    val innings = scorecard.inningsScores

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTabState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            innings.forEachIndexed { index, inningScore ->
                Tab(
                    selected = selectedTabState == index,
                    onClick = { selectedTabState = index },
                    text = { Text("${inningScore.inning.battingTeamName} Innings") }
                )
            }
        }

        if (innings.isNotEmpty()) {
            val selectedInningScore = innings[selectedTabState]
            InningScorecardView(inningScore = selectedInningScore)
        }
    }
}

@Composable
fun InningScorecardView(inningScore: InningScorecard) {
    val oversFraction = (inningScore.totalBalls / 6) + (inningScore.totalBalls % 6) / 10.0
    val crr = if (inningScore.totalBalls == 0) 0.0 else (inningScore.totalRuns.toDouble() / (inningScore.totalBalls / 6.0))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Inning Core Summary Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${inningScore.inning.battingTeamName} Score",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${inningScore.totalRuns}/${inningScore.totalWickets}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Overs: $oversFraction", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text("CRR: ${String.format(Locale.getDefault(), "%.2f", crr)}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            }
        }

        // Batting Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "BATTING",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Divider()

                // Batting headers
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Batsman", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("R", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
                    Text("B", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
                    Text("4s", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
                    Text("6s", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
                    Text("SR", modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
                }
                Divider()

                // Batting items
                inningScore.battingScorecard.forEach { batsman ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = batsman.player.name,
                                modifier = Modifier.weight(2f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text("${batsman.runs}", modifier = Modifier.weight(0.5f), fontSize = 13.sp, textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
                            Text("${batsman.balls}", modifier = Modifier.weight(0.5f), fontSize = 13.sp, textAlign = TextAlign.End)
                            Text("${batsman.fours}", modifier = Modifier.weight(0.5f), fontSize = 13.sp, textAlign = TextAlign.End)
                            Text("${batsman.sixes}", modifier = Modifier.weight(0.5f), fontSize = 13.sp, textAlign = TextAlign.End)
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f", batsman.strikeRate),
                                modifier = Modifier.weight(0.7f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.End
                            )
                        }
                        Text(
                            text = batsman.dismissalText,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    }
                }
            }
        }

        // Bowling Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "BOWLING",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Divider()

                // Bowling headers
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Bowler", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("O", modifier = Modifier.weight(0.6f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
                    Text("M", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
                    Text("R", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
                    Text("W", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
                    Text("Econ", modifier = Modifier.weight(0.8f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
                }
                Divider()

                // Bowling items
                inningScore.bowlingScorecard.forEach { bowler ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = bowler.player.name,
                            modifier = Modifier.weight(2f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text("${bowler.overs}", modifier = Modifier.weight(0.6f), fontSize = 13.sp, textAlign = TextAlign.End)
                        Text("${bowler.maidens}", modifier = Modifier.weight(0.5f), fontSize = 13.sp, textAlign = TextAlign.End)
                        Text("${bowler.runsConceded}", modifier = Modifier.weight(0.5f), fontSize = 13.sp, textAlign = TextAlign.End)
                        Text("${bowler.wickets}", modifier = Modifier.weight(0.5f), fontSize = 13.sp, textAlign = TextAlign.End, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = String.format(Locale.getDefault(), "%.2f", bowler.economy),
                            modifier = Modifier.weight(0.8f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.End
                        )
                    }
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                }
            }
        }

        // Partnerships Card
        if (inningScore.partnerships.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "PARTNERSHIPS",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Divider()

                    inningScore.partnerships.forEach { part ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(2f)) {
                                Text(
                                    text = "${part.batter1Name} & ${part.batter2Name}",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Wicket #${part.wicketNumber}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            Text(
                                text = "${part.runs} runs (${part.balls}b)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Divider(color = Color.LightGray.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}
