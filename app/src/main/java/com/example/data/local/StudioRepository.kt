package com.example.data.local

import com.example.domain.model.ChatMessage
import com.example.domain.model.Conversation
import com.example.domain.model.LiquidVersion
import com.example.domain.model.MagicMetadata
import com.example.domain.model.RaceCandidate
import com.example.domain.model.StrategyLogEntry
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@JsonClass(generateAdapter = true)
data class BackupConversation(
    val id: String,
    val title: String,
    val updatedAt: Long
)

@JsonClass(generateAdapter = true)
data class BackupMessage(
    val id: String,
    val conversationId: String,
    val isUser: Boolean,
    val content: String,
    val timestamp: Long,
    val raceCandidatesJson: String? = null,
    val activeRaceIndex: Int = 0,
    val liquidVersionsJson: String? = null,
    val activeLiquidIndex: Int = 0,
    val magicJson: String? = null,
    val isError: Boolean = false
)

@JsonClass(generateAdapter = true)
data class BackupStrategyLog(
    val id: String,
    val timestamp: Long,
    val promptPreview: String,
    val modeName: String,
    val winnerModel: String,
    val score: Int,
    val durationMs: Long,
    val candidatesCount: Int,
    val liquidApplied: Boolean
)

@JsonClass(generateAdapter = true)
data class StudioBackupPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val conversations: List<BackupConversation>,
    val messages: List<BackupMessage>,
    val strategyLogs: List<BackupStrategyLog>
)

class StudioRepository(private val dao: StudioDao) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val raceListAdapter = moshi.adapter<List<RaceCandidate>>(
        Types.newParameterizedType(List::class.java, RaceCandidate::class.java)
    )
    private val liquidListAdapter = moshi.adapter<List<LiquidVersion>>(
        Types.newParameterizedType(List::class.java, LiquidVersion::class.java)
    )
    private val magicAdapter = moshi.adapter(MagicMetadata::class.java)
    private val backupAdapter = moshi.adapter(StudioBackupPayload::class.java).indent("  ")

    fun observeConversations(): Flow<List<Conversation>> =
        dao.observeConversations().map { list ->
            list.map {
                Conversation(
                    id = it.id,
                    title = it.title,
                    updatedAt = it.updatedAt,
                    messageCount = it.messageCount
                )
            }
        }

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        dao.observeMessages(conversationId).map { list ->
            list.map { entity -> entity.toDomain() }
        }

    fun observeStrategyLogs(): Flow<List<StrategyLogEntry>> =
        dao.observeStrategyLogs().map { list ->
            list.map {
                StrategyLogEntry(
                    id = it.id,
                    timestamp = it.timestamp,
                    promptPreview = it.promptPreview,
                    modeName = it.modeName,
                    winnerModel = it.winnerModel,
                    score = it.score,
                    durationMs = it.durationMs,
                    candidatesCount = it.candidatesCount,
                    liquidApplied = it.liquidApplied
                )
            }
        }

    suspend fun getMessagesOnce(conversationId: String): List<ChatMessage> =
        dao.getMessagesForConversation(conversationId).map { it.toDomain() }

    suspend fun upsertConversation(id: String, title: String, updatedAt: Long = System.currentTimeMillis()) {
        dao.upsertConversation(ConversationEntity(id = id, title = title, updatedAt = updatedAt))
    }

    suspend fun renameConversation(id: String, newTitle: String) {
        dao.renameConversation(id, newTitle.trim(), System.currentTimeMillis())
    }

    suspend fun deleteConversation(id: String) {
        dao.deleteConversation(id)
    }

    suspend fun saveMessage(message: ChatMessage) {
        dao.upsertMessage(message.toEntity())
        dao.touchConversation(message.conversationId, System.currentTimeMillis())
    }

    suspend fun updateActiveRaceIndex(messageId: String, index: Int) {
        dao.updateActiveRaceIndex(messageId, index)
    }

    suspend fun updateActiveLiquidIndex(messageId: String, index: Int) {
        dao.updateActiveLiquidIndex(messageId, index)
    }

    suspend fun deleteMessagesFrom(conversationId: String, fromTimestamp: Long) {
        dao.deleteMessagesFrom(conversationId, fromTimestamp)
    }

    suspend fun addStrategyLog(entry: StrategyLogEntry) {
        dao.insertStrategyLog(
            StrategyLogEntity(
                id = entry.id,
                timestamp = entry.timestamp,
                promptPreview = entry.promptPreview,
                modeName = entry.modeName,
                winnerModel = entry.winnerModel,
                score = entry.score,
                durationMs = entry.durationMs,
                candidatesCount = entry.candidatesCount,
                liquidApplied = entry.liquidApplied
            )
        )
    }

    suspend fun clearStrategyLogs() {
        dao.clearStrategyLogs()
    }

    suspend fun clearAllLocalData() {
        dao.deleteAllConversations()
        dao.clearStrategyLogs()
    }

    suspend fun exportBackupJson(): String {
        val convs = dao.getAllConversations().map {
            BackupConversation(it.id, it.title, it.updatedAt)
        }
        val msgs = dao.getAllMessages().map {
            BackupMessage(
                id = it.id,
                conversationId = it.conversationId,
                isUser = it.isUser,
                content = it.content,
                timestamp = it.timestamp,
                raceCandidatesJson = it.raceCandidatesJson,
                activeRaceIndex = it.activeRaceIndex,
                liquidVersionsJson = it.liquidVersionsJson,
                activeLiquidIndex = it.activeLiquidIndex,
                magicJson = it.magicJson,
                isError = it.isError
            )
        }
        val logs = dao.getAllStrategyLogs().map {
            BackupStrategyLog(
                id = it.id,
                timestamp = it.timestamp,
                promptPreview = it.promptPreview,
                modeName = it.modeName,
                winnerModel = it.winnerModel,
                score = it.score,
                durationMs = it.durationMs,
                candidatesCount = it.candidatesCount,
                liquidApplied = it.liquidApplied
            )
        }
        return backupAdapter.toJson(
            StudioBackupPayload(
                conversations = convs,
                messages = msgs,
                strategyLogs = logs
            )
        )
    }

    suspend fun importBackupJson(rawJson: String): Result<Pair<Int, Int>> = runCatching {
        val parsed = backupAdapter.fromJson(rawJson)
            ?: error("Invalid backup JSON format")
        parsed.conversations.forEach { c ->
            dao.upsertConversation(ConversationEntity(c.id, c.title, c.updatedAt))
        }
        parsed.messages.forEach { m ->
            dao.upsertMessage(
                MessageEntity(
                    id = m.id,
                    conversationId = m.conversationId,
                    isUser = m.isUser,
                    content = m.content,
                    imageUri = null,
                    timestamp = m.timestamp,
                    raceCandidatesJson = m.raceCandidatesJson,
                    activeRaceIndex = m.activeRaceIndex,
                    liquidVersionsJson = m.liquidVersionsJson,
                    activeLiquidIndex = m.activeLiquidIndex,
                    magicJson = m.magicJson,
                    isError = m.isError
                )
            )
        }
        parsed.strategyLogs.forEach { l ->
            dao.insertStrategyLog(
                StrategyLogEntity(
                    id = l.id,
                    timestamp = l.timestamp,
                    promptPreview = l.promptPreview,
                    modeName = l.modeName,
                    winnerModel = l.winnerModel,
                    score = l.score,
                    durationMs = l.durationMs,
                    candidatesCount = l.candidatesCount,
                    liquidApplied = l.liquidApplied
                )
            )
        }
        parsed.conversations.size to parsed.messages.size
    }

    private fun ChatMessage.toEntity(): MessageEntity = MessageEntity(
        id = id,
        conversationId = conversationId,
        isUser = isUser,
        content = content,
        imageUri = imageUri,
        timestamp = timestamp,
        raceCandidatesJson = raceCandidates.takeIf { it.isNotEmpty() }?.let { raceListAdapter.toJson(it) },
        activeRaceIndex = activeRaceIndex,
        liquidVersionsJson = liquidVersions.takeIf { it.isNotEmpty() }?.let { liquidListAdapter.toJson(it) },
        activeLiquidIndex = activeLiquidIndex,
        magicJson = magic?.let { magicAdapter.toJson(it) },
        isError = isError
    )

    private fun MessageEntity.toDomain(): ChatMessage {
        val candidates = raceCandidatesJson?.let {
            runCatching { raceListAdapter.fromJson(it) }.getOrNull()
        }.orEmpty()
        val versions = liquidVersionsJson?.let {
            runCatching { liquidListAdapter.fromJson(it) }.getOrNull()
        }.orEmpty()
        val magicMeta = magicJson?.let {
            runCatching { magicAdapter.fromJson(it) }.getOrNull()
        }
        return ChatMessage(
            id = id,
            conversationId = conversationId,
            isUser = isUser,
            content = content,
            imageUri = imageUri,
            timestamp = timestamp,
            isStreaming = false,
            statusText = null,
            raceCandidates = candidates,
            activeRaceIndex = activeRaceIndex.coerceIn(0, (candidates.size - 1).coerceAtLeast(0)),
            liquidVersions = versions,
            activeLiquidIndex = activeLiquidIndex.coerceIn(0, (versions.size - 1).coerceAtLeast(0)),
            magic = magicMeta,
            isError = isError
        )
    }
}
