package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class ConversationWithCount(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val messageCount: Int
)

@Dao
interface StudioDao {
    @Query(
        """
        SELECT c.id, c.title, c.updatedAt, COUNT(m.id) AS messageCount
        FROM conversations c
        LEFT JOIN messages m ON c.id = m.conversationId
        GROUP BY c.id
        ORDER BY c.updatedAt DESC
        """
    )
    fun observeConversations(): Flow<List<ConversationWithCount>>

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    suspend fun getAllConversations(): List<ConversationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Query("UPDATE conversations SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun renameConversation(id: String, title: String, updatedAt: Long)

    @Query("UPDATE conversations SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touchConversation(id: String, updatedAt: Long)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessagesForConversation(conversationId: String): List<MessageEntity>

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    suspend fun getAllMessages(): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(message: MessageEntity)

    @Query("UPDATE messages SET activeRaceIndex = :index WHERE id = :messageId")
    suspend fun updateActiveRaceIndex(messageId: String, index: Int)

    @Query("UPDATE messages SET activeLiquidIndex = :index WHERE id = :messageId")
    suspend fun updateActiveLiquidIndex(messageId: String, index: Int)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId AND timestamp >= :fromTimestamp")
    suspend fun deleteMessagesFrom(conversationId: String, fromTimestamp: Long)

    @Query("SELECT * FROM strategy_logs ORDER BY timestamp DESC LIMIT 200")
    fun observeStrategyLogs(): Flow<List<StrategyLogEntity>>

    @Query("SELECT * FROM strategy_logs ORDER BY timestamp DESC")
    suspend fun getAllStrategyLogs(): List<StrategyLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStrategyLog(log: StrategyLogEntity)

    @Query("DELETE FROM strategy_logs")
    suspend fun clearStrategyLogs()
}
