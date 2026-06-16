package com.cricket.scorer.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val teamAName: String,
    val teamBName: String,
    val oversCount: Int,
    val matchType: String, // T20, ODI, CUSTOM
    val tossWinner: String,
    val tossDecision: String, // BAT, BOWL
    val status: String, // LIVE, COMPLETED
    val winnerTeamName: String? = null,
    val playerOfTheMatchId: Long? = null,
    val teamARoster: String = "", // Comma-separated player names
    val teamBRoster: String = "", // Comma-separated player names
    val createdAt: Long = System.currentTimeMillis()
)
