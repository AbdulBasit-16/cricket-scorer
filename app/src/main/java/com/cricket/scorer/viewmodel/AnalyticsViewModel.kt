package com.cricket.scorer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cricket.scorer.data.model.PlayerStats
import com.cricket.scorer.data.repository.CricketRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AnalyticsViewModel(private val repository: CricketRepository) : ViewModel() {

    private val _allPlayersStats = MutableStateFlow<List<PlayerStats>>(emptyList())
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val filteredPlayersStats: StateFlow<List<PlayerStats>> = combine(
        _allPlayersStats,
        _searchQuery
    ) { stats, query ->
        if (query.isBlank()) {
            stats
        } else {
            stats.filter { it.playerName.contains(query, ignoreCase = true) }
        }
    }
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedPlayerStats = MutableStateFlow<PlayerStats?>(null)
    val selectedPlayerStats = _selectedPlayerStats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val stats = repository.getAllPlayersWithStats()
                _allPlayersStats.value = stats
            } catch (e: Exception) {
                _allPlayersStats.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchPlayers(query: String) {
        _searchQuery.value = query
    }

    fun selectPlayer(playerId: Long) {
        viewModelScope.launch {
            try {
                val stats = repository.getPlayerStats(playerId)
                _selectedPlayerStats.value = stats
            } catch (e: Exception) {
                // handle error
            }
        }
    }
    
    fun clearSelectedPlayer() {
        _selectedPlayerStats.value = null
    }
}
