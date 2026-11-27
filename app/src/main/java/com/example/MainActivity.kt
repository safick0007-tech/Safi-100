package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.configureAppCheck
import com.example.ui.components.ChatHeader
import com.example.ui.components.ChatInputBar
import com.example.ui.components.DisclaimerDialog
import com.example.ui.components.MessageBubbleItem
import com.example.ui.components.SettingsDialog
import com.example.ui.components.SidebarDrawerContent
import com.example.ui.components.StrategyLogsDialog
import com.example.ui.components.WaitingGamesDialog
import com.example.ui.components.WelcomeView
import com.example.ui.theme.GodModeStudioTheme
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.viewmodel.ChatUiState
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check BEFORE UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)

        // STEP 2: Enable Edge-to-Edge display
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            GodModeStudioTheme(
                themePreset = uiState.settings.themePreset,
                dynamicColor = uiState.settings.useDynamicColor
            ) {
                GodModeStudioApp(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun GodModeStudioApp(
    uiState: ChatUiState,
    viewModel: ChatViewModel
) {
    val palette = LocalStudioPalette.current
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var inputText by rememberSaveable { mutableStateOf("") }
    var pendingSaveImagePath by remember { mutableStateOf<String?>(null) }

    // Zero-permission Android Photo Picker (Play Policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setAttachedImage(uri)
        }
    }

    // SAF JSON Backup Export & Import launchers
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.exportBackupToUri(uri)
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importBackupFromUri(uri)
    }

    val saveImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        val srcPath = pendingSaveImagePath
        pendingSaveImagePath = null
        if (uri != null && !srcPath.isNullOrBlank()) {
            viewModel.saveGeneratedImageToUri(srcPath, uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 840.dp

        val sidebarContent: @Composable () -> Unit = {
            SidebarDrawerContent(
                conversations = uiState.conversations,
                activeConversationId = uiState.activeConversationId,
                searchQuery = uiState.searchQuery,
                logsCount = uiState.strategyLogs.size,
                onSearchChange = viewModel::setSearchQuery,
                onNewChat = {
                    viewModel.startNewChat()
                    if (!isExpanded) scope.launch { drawerState.close() }
                },
                onSelectConversation = { id ->
                    viewModel.selectConversation(id)
                    if (!isExpanded) scope.launch { drawerState.close() }
                },
                onRenameConversation = viewModel::renameConversation,
                onDeleteConversation = viewModel::deleteConversation,
                onOpenLogs = {
                    viewModel.openLogsDialog(true)
                    if (!isExpanded) scope.launch { drawerState.close() }
                },
                onOpenSettings = {
                    viewModel.openSettings(true)
                    if (!isExpanded) scope.launch { drawerState.close() }
                },
                modifier = Modifier.width(304.dp)
            )
        }

        val chatPane: @Composable () -> Unit = {
            ChatWorkspacePane(
                uiState = uiState,
                inputText = inputText,
                showMenuButton = !isExpanded,
                snackbarHostState = snackbarHostState,
                onInputChange = { inputText = it },
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onModeSelected = viewModel::setMode,
                onToggleLiquid = viewModel::toggleLiquidQuick,
                onNewChat = viewModel::startNewChat,
                onOpenSettings = { viewModel.openSettings(true) },
                onOpenGame = { viewModel.toggleWaitingGame(true) },
                onPickImage = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRemoveImage = { viewModel.setAttachedImage(null) },
                onSend = {
                    val toSend = inputText
                    inputText = ""
                    viewModel.sendMessage(toSend)
                },
                onStarterPromptClick = { starter ->
                    viewModel.sendMessage(starter)
                },
                onStop = viewModel::stopGeneration,
                onRetry = viewModel::retryAssistantMessage,
                onEditAndResend = viewModel::editAndResendUserMessage,
                onSelectRaceIndex = viewModel::selectRaceCandidate,
                onSelectLiquidIndex = viewModel::selectLiquidVersion,
                onSaveImage = { path ->
                    pendingSaveImagePath = path
                    saveImageLauncher.launch("godmode-photo-${System.currentTimeMillis()}.png")
                }
            )
        }

        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.canvas)
            ) {
                sidebarContent()
                VerticalDivider(color = palette.border)
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    chatPane()
                }
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = palette.surface,
                        modifier = Modifier.width(310.dp)
                    ) {
                        sidebarContent()
                    }
                }
            ) {
                chatPane()
            }
        }
    }

    // First-launch Disclaimer Modal
    if (!uiState.settings.disclaimerAccepted) {
        DisclaimerDialog(onAccept = viewModel::acceptDisclaimer)
    }

    // Studio Settings Modal
    if (uiState.showSettingsSheet) {
        SettingsDialog(
            settings = uiState.settings,
            localTestResult = uiState.localTestResult,
            isTestingLocal = uiState.isTestingLocal,
            onUpdateSettings = viewModel::updateSettings,
            onApplyPreset = viewModel::applyParameterPreset,
            onTestLocalEndpoint = viewModel::testLocalEndpoint,
            onOpenLogs = {
                viewModel.openSettings(false)
                viewModel.openLogsDialog(true)
            },
            onExportBackup = { exportBackupLauncher.launch("godmode-studio-backup.json") },
            onImportBackup = { importBackupLauncher.launch(arrayOf("application/json", "*/*")) },
            onClearAllData = viewModel::clearAllLocalData,
            onDismiss = { viewModel.openSettings(false) }
        )
    }

    // Strategy & Race Logs Dialog
    if (uiState.showLogsDialog) {
        StrategyLogsDialog(
            logs = uiState.strategyLogs,
            onClearLogs = viewModel::clearStrategyLogs,
            onDismiss = { viewModel.openLogsDialog(false) }
        )
    }

    // Playable Waiting Mini-Game Overlay
    if (uiState.showWaitingGame) {
        WaitingGamesDialog(
            initialGame = uiState.settings.waitingGame,
            statusBanner = uiState.activeStatus,
            onDismiss = { viewModel.toggleWaitingGame(false) }
        )
    }
}

@Composable
private fun ChatWorkspacePane(
    uiState: ChatUiState,
    inputText: String,
    showMenuButton: Boolean,
    snackbarHostState: SnackbarHostState,
    onInputChange: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    onModeSelected: (com.example.domain.model.ChatMode) -> Unit,
    onToggleLiquid: () -> Unit,
    onNewChat: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGame: () -> Unit,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
    onSend: () -> Unit,
    onStarterPromptClick: (String) -> Unit,
    onStop: () -> Unit,
    onRetry: (com.example.domain.model.ChatMessage) -> Unit,
    onEditAndResend: (com.example.domain.model.ChatMessage, String) -> Unit,
    onSelectRaceIndex: (String, Int) -> Unit,
    onSelectLiquidIndex: (String, Int) -> Unit,
    onSaveImage: (String) -> Unit
) {
    val palette = LocalStudioPalette.current
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.displayedContent?.length) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = palette.canvas,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ChatHeader(
                settings = uiState.settings,
                showMenuButton = showMenuButton,
                onOpenDrawer = onOpenDrawer,
                onModeSelected = onModeSelected,
                onToggleLiquid = onToggleLiquid,
                onNewChat = onNewChat,
                onOpenSettings = onOpenSettings,
                onOpenGame = onOpenGame
            )
        },
        bottomBar = {
            ChatInputBar(
                inputText = inputText,
                onInputChange = onInputChange,
                attachedImageUri = uiState.attachedImageUri,
                onPickImage = onPickImage,
                onRemoveImage = onRemoveImage,
                isGenerating = uiState.isGenerating,
                settings = uiState.settings,
                onSend = onSend,
                onStop = onStop
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (uiState.messages.isEmpty()) {
                WelcomeView(
                    settings = uiState.settings,
                    onPromptClick = onStarterPromptClick
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 820.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { msg ->
                        MessageBubbleItem(
                            message = msg,
                            isGenerating = uiState.isGenerating,
                            onRetry = onRetry,
                            onEditAndResend = onEditAndResend,
                            onSelectRaceIndex = onSelectRaceIndex,
                            onSelectLiquidIndex = onSelectLiquidIndex,
                            onSaveImage = onSaveImage
                        )
                    }
                }
            }
        }
    }
}
