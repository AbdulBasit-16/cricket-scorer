package com.cricket.scorer.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "overs",
    foreignKeys = [
        ForeignKey(
            entity = InningEntity::class,
            parentColumns = ["id"],
            childColumns = ["inningId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["bowlerId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("inningId"), Index("bowlerId")]
)
data class OverEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val inningId: Long,
    val overNumber: Int, // 0-indexed
    val bowlerId: Long,
    val isCompleted: Boolean = false
)
