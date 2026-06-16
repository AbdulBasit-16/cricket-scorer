package com.cricket.scorer.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "innings",
    foreignKeys = [
        ForeignKey(
            entity = MatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["matchId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["currentStrikerId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["currentNonStrikerId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["currentBowlerId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("matchId"),
        Index("currentStrikerId"),
        Index("currentNonStrikerId"),
        Index("currentBowlerId")
    ]
)
data class InningEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val matchId: Long,
    val battingTeamName: String,
    val bowlingTeamName: String,
    val inningNumber: Int, // 1 or 2
    val isCompleted: Boolean = false,
    val currentStrikerId: Long? = null,
    val currentNonStrikerId: Long? = null,
    val currentBowlerId: Long? = null
)
