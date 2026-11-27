package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val updatedAt: Long
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conversationId"), Index("timestamp")]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val isUser: Boolean,
    val content: String,
    val imageUri: String?,
    val timestamp: Long,
    val raceCandidatesJson: String?,
    val activeRaceIndex: Int,
    val liquidVersionsJson: String?,
    val activeLiquidIndex: Int,
    val magicJson: String?,
    val isError: Boolean
)

@Entity(
    tableName = "strategy_logs",
    indices = [Index("timestamp")]
)
data class StrategyLogEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val promptPreview: String,
    val modeName: String,
    val winnerModel: String,
    val score: Int,
    val durationMs: Long,
    val candidatesCount: Int,
    val liquidApplied: Boolean
)
