package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.local.StudioRepository
import com.example.data.network.OpenAiCompatibleClient
import com.example.domain.engine.ChatOrchestrator
import com.example.domain.model.ChatMessage
import com.example.domain.model.ChatMode
import com.example.domain.model.Conversation
import com.example.domain.model.ModelParameters
import com.example.domain.model.StrategyLogEntry
import com.example.domain.model.StudioSettings
import com.example.domain.model.WaitingGameType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class ChatUiState(
    val settings: StudioSettings = StudioSettings(),
    val conversations: List<Conversation> = emptyList(),
    val activeConversationId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val strategyLogs: List<StrategyLogEntry> = emptyList(),
    val isGenerating: Boolean = false,
    val activeStatus: String? = null,
    val searchQuery: String = "",
    val attachedImageUri: Uri? = null,
    val showSettingsSheet: Boolean = false,
    val showLogsDialog: Boolean = false,
    val showWaitingGame: Boolean = false,
    val localTestResult: String? = null,
    val isTestingLocal: Boolean = false,
    val discoveredLocalModels: List<String> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = StudioRepository(database.studioDao())
    private val settingsStore = SettingsDataStore(application)
    private val openAiClient = OpenAiCompatibleClient()
    private val orchestrator = ChatOrchestrator(openAiClient)

    private val _activeConversationId = MutableStateFlow<String?>(null)
    private val _streamingMessage = MutableStateFlow<ChatMessage?>(null)
    private val _isGenerating = MutableStateFlow(false)
    private val _activeStatus = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _attachedImageUri = MutableStateFlow<Uri?>(null)
    private val _showSettingsSheet = MutableStateFlow(false)
    private val _showLogsDialog = MutableStateFlow(false)
    private val _showWaitingGame = MutableStateFlow(false)
    private val _localTestResult = MutableStateFlow<String?>(null)
    private val _isTestingLocal = MutableStateFlow(false)
    private val _discoveredLocalModels = MutableStateFlow<List<String>>(emptyList())

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    private var activeGenerationJob: Job? = null

    private val persistedMessagesFlow = _activeConversationId.flatMapLatest { convId ->
        if (convId == null) flowOf(emptyList()) else repository.observeMessages(convId)
    }

    private val mergedMessagesFlow = combine(persistedMessagesFlow, _streamingMessage) { dbMsgs, liveMsg ->
        if (liveMsg != null && liveMsg.conversationId == _activeConversationId.value) {
            val withoutDuplicate = dbMsgs.filterNot { it.id == liveMsg.id }
            withoutDuplicate + liveMsg
        } else {
            dbMsgs
        }
    }

    private val coreDataFlow = combine(
        settingsStore.settingsFlow,
        repository.observeConversations(),
        _activeConversationId,
        mergedMessagesFlow,
        repository.observeStrategyLogs()
    ) { settings, convs, activeId, msgs, logs ->
        CoreBundle(settings, convs, activeId, msgs, logs)
    }

    private val transientUiFlow = combine(
        _isGenerating,
        _activeStatus,
        _searchQuery,
        _attachedImageUri,
        _showSettingsSheet
    ) { generating, status, search, imageUri, showSettings ->
        TransientBundle(generating, status, search, imageUri, showSettings)
    }

    private val overlayUiFlow = combine(
        _showLogsDialog,
        _showWaitingGame,
        _localTestResult,
        _isTestingLocal,
        _discoveredLocalModels
    ) { logsDlg, gameOverlay, testRes, testing, models ->
        OverlayBundle(logsDlg, gameOverlay, testRes, testing, models)
    }

    val uiState: StateFlow<ChatUiState> = combine(
        coreDataFlow,
        transientUiFlow,
        overlayUiFlow
    ) { core, ui, overlay ->
        ChatUiState(
            settings = core.settings,
            conversations = core.conversations,
            activeConversationId = core.activeId,
            messages = core.messages,
            strategyLogs = core.logs,
            isGenerating = ui.isGenerating,
            activeStatus = ui.activeStatus,
            searchQuery = ui.searchQuery,
            attachedImageUri = ui.attachedImageUri,
            showSettingsSheet = ui.showSettingsSheet,
            showLogsDialog = overlay.showLogsDialog,
            showWaitingGame = overlay.showWaitingGame,
            localTestResult = overlay.localTestResult,
            isTestingLocal = overlay.isTestingLocal,
            discoveredLocalModels = overlay.discoveredLocalModels
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChatUiState()
    )

    fun startNewChat() {
        stopGeneration()
        _activeConversationId.value = null
        _attachedImageUri.value = null
        _streamingMessage.value = null
    }

    fun selectConversation(id: String) {
        if (_activeConversationId.value != id) {
            stopGeneration()
            _activeConversationId.value = id
            _attachedImageUri.value = null
        }
    }

    fun renameConversation(id: String, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            repository.renameConversation(id, newTitle)
            _snackbarEvents.tryEmit("Conversation renamed")
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            if (_activeConversationId.value == id) {
                stopGeneration()
                _activeConversationId.value = null
            }
            repository.deleteConversation(id)
            _snackbarEvents.tryEmit("Conversation deleted")
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setAttachedImage(uri: Uri?) {
        _attachedImageUri.value = uri
    }

    fun openSettings(open: Boolean) {
        _showSettingsSheet.value = open
    }

    fun openLogsDialog(open: Boolean) {
        _showLogsDialog.value = open
    }

    fun toggleWaitingGame(show: Boolean) {
        _showWaitingGame.value = show
    }

    fun acceptDisclaimer() {
        updateSettings { it.copy(disclaimerAccepted = true) }
    }

    fun setMode(mode: ChatMode) {
        updateSettings { it.copy(activeMode = mode) }
    }

    fun toggleLiquidQuick() {
        updateSettings { it.copy(liquidEnabled = !it.liquidEnabled) }
    }

    fun updateSettings(transform: (StudioSettings) -> StudioSettings) {
        viewModelScope.launch {
            settingsStore.updateSettings(transform)
        }
    }

    fun applyParameterPreset(presetName: String) {
        val params = when (presetName) {
            "Precise" -> ModelParameters(0.25f, 0.85f, 2048, 0f, 0f, "Precise")
            "Balanced" -> ModelParameters(0.70f, 0.92f, 2048, 0f, 0f, "Balanced")
            "Creative" -> ModelParameters(1.00f, 0.96f, 2560, 0.1f, 0.1f, "Creative")
            "Wild" -> ModelParameters(1.30f, 0.98f, 2560, 0.2f, 0.2f, "Wild")
            "Long-form" -> ModelParameters(0.65f, 0.92f, 4096, 0f, 0f, "Long-form")
            else -> ModelParameters()
        }
        updateSettings { it.copy(modelParameters = params, autoTuneEnabled = false) }
    }

    fun sendMessage(rawPrompt: String) {
        val prompt = rawPrompt.trim()
        val imageUri = _attachedImageUri.value
        if (prompt.isEmpty() && imageUri == null) return
        if (_isGenerating.value) return

        val settings = uiState.value.settings
        val convId = _activeConversationId.value ?: UUID.randomUUID().toString()
        val isNewConv = _activeConversationId.value == null
        _activeConversationId.value = convId
        _attachedImageUri.value = null

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = convId,
            isUser = true,
            content = prompt.ifBlank { "Analyze the attached image." },
            imageUri = imageUri?.toString(),
            timestamp = System.currentTimeMillis()
        )

        val assistantId = UUID.randomUUID().toString()
        val assistantStartTs = userMessage.timestamp + 1

        activeGenerationJob = viewModelScope.launch {
            try {
                if (isNewConv) {
                    val autoTitle = prompt.ifBlank { "Image Analysis" }
                        .replace("\n", " ")
                        .take(44)
                        .let { if (prompt.length > 44) "$it…" else it }
                    repository.upsertConversation(convId, autoTitle)
                }

                val existingHistory = repository.getMessagesOnce(convId)
                    .filter { !it.isError }
                    .map { (if (it.isUser) "user" else "model") to it.displayedContent }

                repository.saveMessage(userMessage)
                settingsStore.incrementPromptCount()

                _isGenerating.value = true
                _activeStatus.value = "Preparing ${settings.activeMode.displayName}…"
                if (settings.waitingGame != WaitingGameType.NONE) {
                    _showWaitingGame.value = true
                }

                _streamingMessage.value = ChatMessage(
                    id = assistantId,
                    conversationId = convId,
                    isUser = false,
                    content = "",
                    timestamp = assistantStartTs,
                    isStreaming = true,
                    statusText = _activeStatus.value
                )

                val bitmap = imageUri?.let { loadScaledBitmap(it) }

                val finalResult = orchestrator.execute(
                    prompt = userMessage.content,
                    history = existingHistory,
                    settings = settings,
                    attachedBitmap = bitmap
                ) { update ->
                    _activeStatus.value = update.statusText
                    _streamingMessage.value = ChatMessage(
                        id = assistantId,
                        conversationId = convId,
                        isUser = false,
                        content = update.content,
                        timestamp = assistantStartTs,
                        isStreaming = !update.isFinal,
                        statusText = update.statusText,
                        raceCandidates = update.raceCandidates,
                        activeRaceIndex = update.activeRaceIndex,
                        liquidVersions = update.liquidVersions,
                        activeLiquidIndex = update.activeLiquidIndex,
                        magic = update.magic,
                        isError = update.isError
                    )
                }

                val persistedAssistant = ChatMessage(
                    id = assistantId,
                    conversationId = convId,
                    isUser = false,
                    content = finalResult.content,
                    timestamp = assistantStartTs,
                    isStreaming = false,
                    statusText = null,
                    raceCandidates = finalResult.raceCandidates,
                    activeRaceIndex = finalResult.activeRaceIndex,
                    liquidVersions = finalResult.liquidVersions,
                    activeLiquidIndex = finalResult.activeLiquidIndex,
                    magic = finalResult.magic,
                    isError = finalResult.isError
                )
                repository.saveMessage(persistedAssistant)

                if (!finalResult.isError && finalResult.magic != null) {
                    repository.addStrategyLog(
                        StrategyLogEntry(
                            id = UUID.randomUUID().toString(),
                            timestamp = System.currentTimeMillis(),
                            promptPreview = userMessage.content.take(70),
                            modeName = finalResult.magic.modeName,
                            winnerModel = finalResult.magic.winnerModel,
                            score = finalResult.magic.score?.totalScore ?: 0,
                            durationMs = finalResult.magic.totalDurationMs,
                            candidatesCount = finalResult.raceCandidates.size.coerceAtLeast(1),
                            liquidApplied = finalResult.liquidVersions.size > 1
                        )
                    )
                }
            } catch (e: CancellationException) {
                val partial = _streamingMessage.value
                if (partial != null && partial.displayedContent.isNotBlank()) {
                    repository.saveMessage(
                        partial.copy(
                            isStreaming = false,
                            statusText = null,
                            content = partial.displayedContent + "\n\n_(Stopped)_"
                        )
                    )
                }
            } catch (e: Exception) {
                val errMsg = ChatMessage(
                    id = assistantId,
                    conversationId = convId,
                    isUser = false,
                    content = e.localizedMessage ?: "Generation failed. Please retry.",
                    timestamp = assistantStartTs,
                    isStreaming = false,
                    isError = true
                )
                repository.saveMessage(errMsg)
            } finally {
                _streamingMessage.value = null
                _isGenerating.value = false
                _activeStatus.value = null
            }
        }
    }

    fun stopGeneration() {
        activeGenerationJob?.cancel()
        activeGenerationJob = null
        _isGenerating.value = false
        _activeStatus.value = null
    }

    fun retryAssistantMessage(assistantMessage: ChatMessage) {
        if (_isGenerating.value) return
        viewModelScope.launch {
            val allMsgs = repository.getMessagesOnce(assistantMessage.conversationId)
            val targetIdx = allMsgs.indexOfFirst { it.id == assistantMessage.id }
            if (targetIdx <= 0) return@launch
            val prevUser = allMsgs.subList(0, targetIdx).lastOrNull { it.isUser } ?: return@launch
            repository.deleteMessagesFrom(assistantMessage.conversationId, prevUser.timestamp)
            sendMessage(prevUser.content)
        }
    }

    fun editAndResendUserMessage(userMessage: ChatMessage, updatedPrompt: String) {
        val trimmed = updatedPrompt.trim()
        if (trimmed.isEmpty() || _isGenerating.value) return
        viewModelScope.launch {
            repository.deleteMessagesFrom(userMessage.conversationId, userMessage.timestamp)
            sendMessage(trimmed)
        }
    }

    fun selectRaceCandidate(messageId: String, index: Int) {
        viewModelScope.launch {
            repository.updateActiveRaceIndex(messageId, index)
        }
    }

    fun selectLiquidVersion(messageId: String, index: Int) {
        viewModelScope.launch {
            repository.updateActiveLiquidIndex(messageId, index)
        }
    }

    fun clearStrategyLogs() {
        viewModelScope.launch {
            repository.clearStrategyLogs()
            _snackbarEvents.tryEmit("Strategy logs cleared")
        }
    }

    fun clearAllLocalData() {
        viewModelScope.launch {
            stopGeneration()
            _activeConversationId.value = null
            repository.clearAllLocalData()
            _snackbarEvents.tryEmit("All local chats and logs cleared")
        }
    }

    fun testLocalEndpoint(url: String) {
        if (_isTestingLocal.value) return
        viewModelScope.launch {
            _isTestingLocal.value = true
            _localTestResult.value = null
            val result = openAiClient.testConnectionAndFetchModels(url)
            result.onSuccess { models ->
                _discoveredLocalModels.value = models
                _localTestResult.value = if (models.isEmpty()) {
                    "Connected! Endpoint reachable (no model list returned)."
                } else {
                    "Connected! Found ${models.size} model(s): ${models.take(3).joinToString(", ")}"
                }
                if (models.isNotEmpty()) {
                    updateSettings { it.copy(localModelName = models.first()) }
                }
            }.onFailure { err ->
                _localTestResult.value = "Connection failed: ${err.localizedMessage ?: "Unreachable"}"
            }
            _isTestingLocal.value = false
        }
    }

    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val json = repository.exportBackupJson()
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Cannot open output file")
                }
            }
            result.onSuccess {
                _snackbarEvents.tryEmit("Backup exported successfully")
            }.onFailure {
                _snackbarEvents.tryEmit("Export failed: ${it.localizedMessage}")
            }
        }
    }

    fun importBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val raw = getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader().readText()
                    } ?: error("Cannot read backup file")
                    repository.importBackupJson(raw).getOrThrow()
                }
            }
            result.onSuccess { (convs, msgs) ->
                _snackbarEvents.tryEmit("Imported $convs chats and $msgs messages")
            }.onFailure {
                _snackbarEvents.tryEmit("Import failed: ${it.localizedMessage}")
            }
        }
    }

    private suspend fun loadScaledBitmap(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = getApplication<Application>().contentResolver
            val original = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                ?: return@runCatching null
            val maxDim = 1024
            if (original.width <= maxDim && original.height <= maxDim) {
                original
            } else {
                val scale = maxDim.toFloat() / maxOf(original.width, original.height).toFloat()
                Bitmap.createScaledBitmap(
                    original,
                    (original.width * scale).toInt().coerceAtLeast(1),
                    (original.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            }
        }.getOrNull()
    }

    private data class CoreBundle(
        val settings: StudioSettings,
        val conversations: List<Conversation>,
        val activeId: String?,
        val messages: List<ChatMessage>,
        val logs: List<StrategyLogEntry>
    )

    private data class TransientBundle(
        val isGenerating: Boolean,
        val activeStatus: String?,
        val searchQuery: String,
        val attachedImageUri: Uri?,
        val showSettingsSheet: Boolean
    )

    private data class OverlayBundle(
        val showLogsDialog: Boolean,
        val showWaitingGame: Boolean,
        val localTestResult: String?,
        val isTestingLocal: Boolean,
        val discoveredLocalModels: List<String>
    )
}
