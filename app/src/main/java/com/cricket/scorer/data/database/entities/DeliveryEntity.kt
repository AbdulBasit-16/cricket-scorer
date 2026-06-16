package com.cricket.scorer.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "deliveries",
    foreignKeys = [
        ForeignKey(
            entity = InningEntity::class,
            parentColumns = ["id"],
            childColumns = ["inningId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = OverEntity::class,
            parentColumns = ["id"],
            childColumns = ["overId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["strikerId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["nonStrikerId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["bowlerId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("inningId"),
        Index("overId"),
        Index("strikerId"),
        Index("nonStrikerId"),
        Index("bowlerId")
    ]
)
data class DeliveryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val inningId: Long,
    val overId: Long,
    val strikerId: Long,
    val nonStrikerId: Long,
    val bowlerId: Long,
    
    val runsScored: Int,      // Runs off the bat (0, 1, 2, 3, 4, 6)
    val extrasRuns: Int,      // Runs scored from extras
    val extraType: String,    // NONE, WIDE, NO_BALL, BYE, LEG_BYE
    
    val isWicket: Boolean,
    val wicketType: String? = null,        // BOWLED, CAUGHT, LBW, RUN_OUT, STUMPED, HIT_WICKET, RETIRED, etc.
    val dismissedPlayerId: Long? = null,   // Who got out
    val assistingPlayerId: Long? = null,   // Fielder ID (for caught, run out, stumped)
    
    val customNotes: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
