package com.cricket.scorer.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.sp
import com.cricket.scorer.data.model.PlayerStats
import com.cricket.scorer.viewmodel.AnalyticsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsHubScreen(
    viewModel: AnalyticsViewModel,
    onBack: () -> Unit
) {
    val playersStats by viewModel.filteredPlayersStats.collectAsState()
    val selectedPlayer by viewModel.selectedPlayerStats.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Refresh data on enter
    LaunchedEffect(Unit) {
        viewModel.loadStats()
        viewModel.clearSelectedPlayer()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedPlayer != null) selectedPlayer!!.playerName else "Analytics Hub") },
                navigationIcon = {
                    TextButton(onClick = {
                        if (selectedPlayer != null) {
                            viewModel.clearSelectedPlayer()
                        } else {
                            onBack()
                        }
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
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (selectedPlayer != null) {
                // Detailed Player Profile View
                PlayerProfileDetail(player = selectedPlayer!!)
            } else {
                // Leaderboard Directory View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchPlayers(it) },
                        label = { Text("Search Players") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Directory List
                    if (playersStats.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No players found.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(playersStats) { stats ->
                                PlayerStatRow(stats = stats) {
                                    viewModel.selectPlayer(stats.playerId)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerStatRow(stats: PlayerStats, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(stats.playerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Runs: ${stats.batting.totalRuns}  |  Wickets: ${stats.bowling.wickets}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
            Text("Details >", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun PlayerProfileDetail(player: PlayerStats) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Player header
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(player.playerName, fontSize = 28.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Career Aggregate Statistics", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        }

        // Batting Stats Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Batting Metrics", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Divider()
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatLabelValue("Innings", player.batting.inningsPlayed.toString())
                    StatLabelValue("Total Runs", player.batting.totalRuns.toString())
                    StatLabelValue("Balls Faced", player.batting.ballsFaced.toString())
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatLabelValue("Batting Avg", String.format(Locale.getDefault(), "%.2f", player.batting.average))
                    StatLabelValue("Strike Rate", String.format(Locale.getDefault(), "%.2f", player.batting.strikeRate))
                    StatLabelValue("Highest", player.batting.highestScore.toString())
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatLabelValue("100s", player.batting.hundreds.toString())
                    StatLabelValue("50s", player.batting.fifties.toString())
                    StatLabelValue("Ducks", player.batting.ducks.toString())
                }
            }
        }

        // Bowling Stats Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Bowling Metrics", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Divider()
                
                val overs = (player.bowling.ballsBowled / 6) + (player.bowling.ballsBowled % 6) / 10.0
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatLabelValue("Overs", overs.toString())
                    StatLabelValue("Wickets", player.bowling.wickets.toString())
                    StatLabelValue("Runs Conc", player.bowling.runsConceded.toString())
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatLabelValue("Bowling Avg", String.format(Locale.getDefault(), "%.2f", player.bowling.average))
                    StatLabelValue("Economy", String.format(Locale.getDefault(), "%.2f", player.bowling.economy))
                    StatLabelValue("Strike Rate", String.format(Locale.getDefault(), "%.2f", player.bowling.strikeRate))
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatLabelValue("Maidens", player.bowling.maidens.toString())
                    StatLabelValue("Best Wkts", player.bowling.bestWickets.toString())
                    StatLabelValue("Best Runs", player.bowling.bestRuns.toString())
                }
            }
        }

        // Form Trend Charts Card
        if (player.recentPerformance.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Form Trends (Canvas Graph)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Divider()

                    // Batting chart
                    PerformanceChart(
                        performances = player.recentPerformance,
                        isBatting = true,
                        modifier = Modifier.height(200.dp).fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bowling chart
                    PerformanceChart(
                        performances = player.recentPerformance,
                        isBatting = false,
                        modifier = Modifier.height(200.dp).fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun StatLabelValue(label: String, value: String) {
    Column(
        modifier = Modifier.width(90.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(label, fontSize = 11.sp, color = Color.Gray)
    }
}
