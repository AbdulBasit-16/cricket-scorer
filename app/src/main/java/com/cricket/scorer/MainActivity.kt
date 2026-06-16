package com.cricket.scorer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cricket.scorer.data.database.CricketDatabase
import com.cricket.scorer.data.repository.CricketRepositoryImpl
import com.cricket.scorer.ui.analytics.AnalyticsHubScreen
import com.cricket.scorer.ui.dashboard.DashboardScreen
import com.cricket.scorer.ui.history.MatchHistoryScreen
import com.cricket.scorer.ui.history.ScorecardScreen
import com.cricket.scorer.ui.live.LiveScorerScreen
import com.cricket.scorer.ui.live.MatchSetupScreen
import com.cricket.scorer.ui.theme.CricketScorerTheme
import com.cricket.scorer.viewmodel.AnalyticsViewModel
import com.cricket.scorer.viewmodel.HistoryViewModel
import com.cricket.scorer.viewmodel.ScoringViewModel
import com.cricket.scorer.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Manual Dependency Injection Setup
        val database = CricketDatabase.getDatabase(applicationContext)
        val repository = CricketRepositoryImpl(
            playerDao = database.playerDao(),
            matchDao = database.matchDao(),
            inningDao = database.inningDao(),
            overDao = database.overDao(),
            deliveryDao = database.deliveryDao()
        )
        val factory = ViewModelFactory(repository)

        // ViewModels instantiation
        val scoringViewModel: ScoringViewModel by viewModels { factory }
        val analyticsViewModel: AnalyticsViewModel by viewModels { factory }
        val historyViewModel: HistoryViewModel by viewModels { factory }

        setContent {
            CricketScorerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CricketAppNavigation(
                        scoringViewModel = scoringViewModel,
                        analyticsViewModel = analyticsViewModel,
                        historyViewModel = historyViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun CricketAppNavigation(
    scoringViewModel: ScoringViewModel,
    analyticsViewModel: AnalyticsViewModel,
    historyViewModel: HistoryViewModel
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                scoringViewModel = scoringViewModel,
                historyViewModel = historyViewModel,
                onStartNewMatch = { navController.navigate("setup") },
                onResumeMatch = { navController.navigate("live") },
                onViewAnalytics = { navController.navigate("analytics") },
                onViewHistory = { navController.navigate("history") },
                onViewScorecard = { matchId -> navController.navigate("scorecard/$matchId") }
            )
        }

        composable("setup") {
            MatchSetupScreen(
                viewModel = scoringViewModel,
                onMatchInitialized = {
                    navController.navigate("live") {
                        popUpTo("dashboard")
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("live") {
            LiveScorerScreen(
                viewModel = scoringViewModel,
                onNavigateToDashboard = {
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            )
        }

        composable("analytics") {
            AnalyticsHubScreen(
                viewModel = analyticsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("history") {
            MatchHistoryScreen(
                viewModel = historyViewModel,
                onViewScorecard = { matchId -> navController.navigate("scorecard/$matchId") },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "scorecard/{matchId}",
            arguments = listOf(navArgument("matchId") { type = NavType.LongType })
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getLong("matchId") ?: 0L
            ScorecardScreen(
                matchId = matchId,
                viewModel = historyViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
