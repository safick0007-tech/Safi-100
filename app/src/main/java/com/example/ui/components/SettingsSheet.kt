package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import android.os.Build
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WifiFind
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ai.FirebaseGeminiProvider
import com.example.domain.engine.ChatOrchestrator
import com.example.domain.model.AutoTuneStrategy
import com.example.domain.model.ChatMode
import com.example.domain.model.ProviderSource
import com.example.domain.model.SpeedTier
import com.example.domain.model.StudioSettings
import com.example.domain.model.StudioThemePreset
import com.example.domain.model.WaitingGameType
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.previewSwatchesFor
import kotlin.math.roundToInt

@Composable
fun SettingsDialog(
    settings: StudioSettings,
    localTestResult: String?,
    isTestingLocal: Boolean,
    onUpdateSettings: ((StudioSettings) -> StudioSettings) -> Unit,
    onApplyPreset: (String) -> Unit,
    onTestLocalEndpoint: (String) -> Unit,
    onOpenLogs: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onClearAllData: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalStudioPalette.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var confirmClearAll by remember { mutableStateOf(false) }

    val tabs = listOf(
        stringResource(R.string.tab_general),
        stringResource(R.string.tab_engine),
        stringResource(R.string.tab_prompt),
        stringResource(R.string.tab_providers),
        stringResource(R.string.tab_data)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = palette.surface,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp)
                .border(1.dp, palette.border, RoundedCornerShape(24.dp))
                .testTag("settings_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 10.dp, top = 14.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.settings_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Configure modes, Liquid polish, AutoTune & AI providers",
                            style = MaterialTheme.typography.bodySmall.copy(color = palette.textMuted)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.action_close),
                            tint = palette.textSecondary
                        )
                    }
                }

                // Pill Tab Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEachIndexed { idx, title ->
                        val active = idx == selectedTab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) palette.emeraldSoft else palette.card)
                                .border(
                                    1.dp,
                                    if (active) palette.emerald else palette.border,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedTab = idx }
                                .padding(horizontal = 14.dp, vertical = 9.dp)
                                .testTag("settings_tab_$idx")
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = if (active) palette.emerald else palette.textSecondary,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(color = palette.border)

                // Tab Body
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> GeneralTab(settings, onUpdateSettings)
                        1 -> EngineTab(settings, onUpdateSettings, onApplyPreset)
                        2 -> PromptTab(settings, onUpdateSettings)
                        3 -> ProvidersTab(settings, localTestResult, isTestingLocal, onUpdateSettings, onTestLocalEndpoint)
                        4 -> DataTab(onOpenLogs, onExportBackup, onImportBackup) { confirmClearAll = true }
                    }
                }
            }
        }
    }

    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            containerColor = palette.card,
            title = {
                Text(
                    "Clear All Local Data?",
                    style = MaterialTheme.typography.titleMedium.copy(color = palette.textPrimary)
                )
            },
            text = {
                Text(
                    "This permanently deletes all saved conversations and strategy logs on this device. Consider exporting a JSON backup first.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = palette.textSecondary)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClearAll = false
                        onClearAllData()
                    }
                ) {
                    Text("Delete All", color = palette.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearAll = false }) {
                    Text(stringResource(R.string.action_cancel), color = palette.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun GeneralTab(
    settings: StudioSettings,
    onUpdateSettings: ((StudioSettings) -> StudioSettings) -> Unit
) {
    val palette = LocalStudioPalette.current

    SectionLabel("GODMODE STUDIO COLOR SCHEME")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StudioThemePreset.entries.forEach { preset ->
            val selected = settings.themePreset == preset
            val swatches = previewSwatchesFor(preset)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) palette.emeraldSoft else palette.card)
                    .border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) palette.emerald else palette.border,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable {
                        onUpdateSettings {
                            it.copy(
                                themePreset = preset,
                                useLightTheme = preset.isLight
                            )
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("theme_preset_${preset.name.lowercase()}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = preset.displayName,
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = if (selected) palette.emerald else palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        if (selected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Selected theme",
                                tint = palette.emerald,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = preset.description,
                        style = MaterialTheme.typography.bodySmall.copy(color = palette.textSecondary)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    swatches.forEach { swatchColor ->
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(1.dp, palette.border, CircleShape)
                        )
                    }
                }
            }
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ToggleCard(
            title = "Material You Dynamic Color",
            subtitle = "Blend system wallpaper tones with Material 3 tonal surfaces (Android 12+)",
            checked = settings.useDynamicColor,
            onCheckedChange = { dynamic ->
                onUpdateSettings { it.copy(useDynamicColor = dynamic) }
            }
        )
    }

    SectionLabel("ACTIVE CHAT MODE")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChatMode.entries.forEach { mode ->
            val selected = settings.activeMode == mode
            SelectableRowCard(
                title = mode.displayName,
                subtitle = mode.description,
                selected = selected,
                onClick = { onUpdateSettings { it.copy(activeMode = mode) } }
            )
        }
    }

    SectionLabel("WAITING MINI-GAME")
    Text(
        text = "Play a relaxing canvas game while parallel model races run.",
        style = MaterialTheme.typography.bodySmall.copy(color = palette.textSecondary)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WaitingGameType.entries.forEach { game ->
            val selected = settings.waitingGame == game
            ChoicePill(
                label = game.displayName,
                selected = selected,
                onClick = { onUpdateSettings { it.copy(waitingGame = game) } },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EngineTab(
    settings: StudioSettings,
    onUpdateSettings: ((StudioSettings) -> StudioSettings) -> Unit,
    onApplyPreset: (String) -> Unit
) {
    val palette = LocalStudioPalette.current

    SectionLabel("ULTRA ARENA · SPEED TIER")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SpeedTier.entries.forEach { tier ->
            ChoicePill(
                label = tier.displayName,
                selected = settings.speedTier == tier,
                onClick = { onUpdateSettings { it.copy(speedTier = tier) } }
            )
        }
    }

    ToggleCard(
        title = "Early-Stop Scoring Threshold",
        subtitle = "Flag an early leader once Tastemaker score hits ${settings.earlyStopThreshold}/100",
        checked = settings.earlyStopEnabled,
        onCheckedChange = { v -> onUpdateSettings { it.copy(earlyStopEnabled = v) } }
    )
    if (settings.earlyStopEnabled) {
        SliderRow(
            label = "Target Score",
            valueText = "${settings.earlyStopThreshold}/100",
            value = settings.earlyStopThreshold.toFloat(),
            range = 70f..98f,
            onValueChange = { v -> onUpdateSettings { it.copy(earlyStopThreshold = v.roundToInt()) } }
        )
    }

    ToggleCard(
        title = "AI Coaching Synthesis Pass",
        subtitle = "Synthesizes the top 2 race answers into a unified champion response",
        checked = settings.coachingEnabled,
        onCheckedChange = { v -> onUpdateSettings { it.copy(coachingEnabled = v) } }
    )

    HorizontalDivider(color = palette.border.copy(alpha = 0.6f))

    SectionLabel("CLASSIC ARENA & PROMPT LAB")
    ToggleCard(
        title = "Classic Full Combo (5 Personas)",
        subtitle = "Race all 5 curated expert personas in parallel instead of a single persona",
        checked = settings.classicFullCombo,
        onCheckedChange = { v -> onUpdateSettings { it.copy(classicFullCombo = v) } }
    )
    if (!settings.classicFullCombo) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChatOrchestrator.CLASSIC_PERSONAS.forEach { (name, _) ->
                ChoicePill(
                    label = name,
                    selected = settings.classicSelectedPersona == name,
                    onClick = { onUpdateSettings { it.copy(classicSelectedPersona = name) } }
                )
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            FirebaseGeminiProvider.MODEL_FLASH to "Gemini 3.8 Flash",
            FirebaseGeminiProvider.MODEL_LITE to "Gemini 3.5 Lite"
        ).forEach { (id, label) ->
            ChoicePill(
                label = label,
                selected = settings.promptLabModel == id,
                onClick = { onUpdateSettings { it.copy(promptLabModel = id) } },
                modifier = Modifier.weight(1f)
            )
        }
    }

    HorizontalDivider(color = palette.border.copy(alpha = 0.6f))

    SectionLabel("LIQUID PROGRESSIVE REFINEMENT")
    ToggleCard(
        title = "Enable Liquid Mode",
        subtitle = "Progressively refines initial drafts (v0 → v1 → v2) with interactive version switching",
        checked = settings.liquidEnabled,
        onCheckedChange = { v -> onUpdateSettings { it.copy(liquidEnabled = v) } }
    )
    if (settings.liquidEnabled) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChoicePill(
                label = "1 Pass (Fast Polish)",
                selected = settings.liquidPasses == 1,
                onClick = { onUpdateSettings { it.copy(liquidPasses = 1) } },
                modifier = Modifier.weight(1f)
            )
            ChoicePill(
                label = "2 Passes (Executive)",
                selected = settings.liquidPasses == 2,
                onClick = { onUpdateSettings { it.copy(liquidPasses = 2) } },
                modifier = Modifier.weight(1f)
            )
        }
    }

    HorizontalDivider(color = palette.border.copy(alpha = 0.6f))

    SectionLabel("AUTOTUNE & SAMPLING PARAMETERS")
    ToggleCard(
        title = "AutoTune Context Optimizer",
        subtitle = "Automatically adjusts temperature, top-P, and token budgets per prompt type",
        checked = settings.autoTuneEnabled,
        onCheckedChange = { v -> onUpdateSettings { it.copy(autoTuneEnabled = v) } }
    )

    if (settings.autoTuneEnabled) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AutoTuneStrategy.entries.forEach { strat ->
                ChoicePill(
                    label = strat.displayName,
                    selected = settings.autoTuneStrategy == strat,
                    onClick = { onUpdateSettings { it.copy(autoTuneStrategy = strat) } }
                )
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Precise", "Balanced", "Creative", "Wild", "Long-form").forEach { preset ->
                ChoicePill(
                    label = preset,
                    selected = settings.modelParameters.presetName == preset,
                    onClick = { onApplyPreset(preset) }
                )
            }
        }
        SliderRow(
            label = "Temperature",
            valueText = String.format("%.2f", settings.modelParameters.temperature),
            value = settings.modelParameters.temperature,
            range = 0.0f..1.5f,
            onValueChange = { v ->
                onUpdateSettings {
                    it.copy(modelParameters = it.modelParameters.copy(temperature = v, presetName = "Custom"))
                }
            }
        )
        SliderRow(
            label = "Top-P",
            valueText = String.format("%.2f", settings.modelParameters.topP),
            value = settings.modelParameters.topP,
            range = 0.1f..1.0f,
            onValueChange = { v ->
                onUpdateSettings {
                    it.copy(modelParameters = it.modelParameters.copy(topP = v, presetName = "Custom"))
                }
            }
        )
        SliderRow(
            label = "Max Output Tokens",
            valueText = "${settings.modelParameters.maxTokens}",
            value = settings.modelParameters.maxTokens.toFloat(),
            range = 512f..4096f,
            onValueChange = { v ->
                onUpdateSettings {
                    it.copy(
                        modelParameters = it.modelParameters.copy(
                            maxTokens = (v.roundToInt() / 128) * 128,
                            presetName = "Custom"
                        )
                    )
                }
            }
        )
    }
}

@Composable
private fun PromptTab(
    settings: StudioSettings,
    onUpdateSettings: ((StudioSettings) -> StudioSettings) -> Unit
) {
    val palette = LocalStudioPalette.current

    SectionLabel("CUSTOM SYSTEM PROMPT")
    ToggleCard(
        title = "Apply Custom System Prompt",
        subtitle = "Inject persistent persona, language, or formatting rules into every request",
        checked = settings.customSystemPromptEnabled,
        onCheckedChange = { v -> onUpdateSettings { it.copy(customSystemPromptEnabled = v) } }
    )

    OutlinedTextField(
        value = settings.customSystemPrompt,
        onValueChange = { txt -> onUpdateSettings { it.copy(customSystemPrompt = txt) } },
        placeholder = {
            Text(
                "e.g. Always respond in clear, well-structured Bengali with practical code examples.",
                style = MaterialTheme.typography.bodySmall.copy(color = palette.textMuted)
            )
        },
        minLines = 5,
        maxLines = 9,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = palette.emerald,
            unfocusedBorderColor = palette.border,
            focusedContainerColor = palette.card,
            unfocusedContainerColor = palette.card,
            focusedTextColor = palette.textPrimary,
            unfocusedTextColor = palette.textPrimary
        ),
        modifier = Modifier.fillMaxWidth()
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${settings.customSystemPrompt.length} characters",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMono,
                color = palette.textMuted
            )
        )
        if (settings.customSystemPrompt.isNotBlank()) {
            TextButton(onClick = { onUpdateSettings { it.copy(customSystemPrompt = "") } }) {
                Text("Clear", color = palette.danger)
            }
        }
    }

    SectionLabel("QUICK SYSTEM TEMPLATES")
    val templates = listOf(
        "Concise Engineer" to "Be concise, technically rigorous, and lead with working code before brief explanations.",
        "Bengali Assistant" to "Always reply in natural, clear Bengali while keeping technical terms and code clean.",
        "Socratic Mentor" to "Explain concepts step by step with concrete analogies and end with one thought-provoking check question."
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        templates.forEach { (title, body) ->
            SelectableRowCard(
                title = title,
                subtitle = body,
                selected = settings.customSystemPrompt == body,
                onClick = {
                    onUpdateSettings {
                        it.copy(customSystemPromptEnabled = true, customSystemPrompt = body)
                    }
                }
            )
        }
    }
}

@Composable
private fun ProvidersTab(
    settings: StudioSettings,
    localTestResult: String?,
    isTestingLocal: Boolean,
    onUpdateSettings: ((StudioSettings) -> StudioSettings) -> Unit,
    onTestLocalEndpoint: (String) -> Unit
) {
    val palette = LocalStudioPalette.current
    var showOpenRouterKey by remember { mutableStateOf(false) }
    var showVeniceKey by remember { mutableStateOf(false) }

    SectionLabel("ACTIVE AI PROVIDER")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ProviderSource.entries.forEach { src ->
            val subtitle = when (src) {
                ProviderSource.BUILTIN_GEMINI -> "Zero-config Firebase AI Logic (Gemini 3.8 Flash & 3.5 Lite)"
                ProviderSource.OPENROUTER -> "Access 200+ frontier models using your own OpenRouter key"
                ProviderSource.VENICE -> "Privacy-first inference via your Venice AI API key"
                ProviderSource.LOCAL -> "Connect local Ollama / LM Studio OpenAI-compatible servers"
            }
            SelectableRowCard(
                title = src.displayName,
                subtitle = subtitle,
                selected = settings.preferredProvider == src,
                onClick = { onUpdateSettings { it.copy(preferredProvider = src) } }
            )
        }
    }

    HorizontalDivider(color = palette.border.copy(alpha = 0.6f))

    SectionLabel("OPENROUTER CONFIGURATION")
    SecretInputField(
        label = "OpenRouter API Key",
        value = settings.openRouterApiKey,
        visible = showOpenRouterKey,
        onToggleVisibility = { showOpenRouterKey = !showOpenRouterKey },
        onValueChange = { v -> onUpdateSettings { it.copy(openRouterApiKey = v) } }
    )
    PlainInputField(
        label = "OpenRouter Primary Model Slug",
        value = settings.openRouterModel,
        onValueChange = { v -> onUpdateSettings { it.copy(openRouterModel = v) } }
    )

    SectionLabel("VENICE AI CONFIGURATION")
    SecretInputField(
        label = "Venice API Key",
        value = settings.veniceApiKey,
        visible = showVeniceKey,
        onToggleVisibility = { showVeniceKey = !showVeniceKey },
        onValueChange = { v -> onUpdateSettings { it.copy(veniceApiKey = v) } }
    )
    PlainInputField(
        label = "Venice Model ID",
        value = settings.veniceModel,
        onValueChange = { v -> onUpdateSettings { it.copy(veniceModel = v) } }
    )

    SectionLabel("LOCAL SERVER (OLLAMA / LM STUDIO)")
    PlainInputField(
        label = "OpenAI-Compatible Base URL",
        value = settings.localEndpointUrl,
        onValueChange = { v -> onUpdateSettings { it.copy(localEndpointUrl = v) } }
    )
    PlainInputField(
        label = "Local Model Name",
        value = settings.localModelName,
        onValueChange = { v -> onUpdateSettings { it.copy(localModelName = v) } }
    )

    OutlinedButton(
        onClick = { onTestLocalEndpoint(settings.localEndpointUrl) },
        enabled = !isTestingLocal,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        if (isTestingLocal) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Rounded.WifiFind, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text("Test Local Connection & Discover Models")
    }

    if (!localTestResult.isNullOrBlank()) {
        Text(
            text = localTestResult,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = JetBrainsMono,
                color = if (localTestResult.startsWith("Connected")) palette.emerald else palette.danger
            )
        )
    }
}

@Composable
private fun DataTab(
    onOpenLogs: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onRequestClearAll: () -> Unit
) {
    val palette = LocalStudioPalette.current

    SectionLabel("LOCAL SELF-CUSTODY STORAGE")
    Text(
        text = "All chats, race scores, and optional keys stay strictly in local device storage. Export a portable JSON backup anytime.",
        style = MaterialTheme.typography.bodySmall.copy(color = palette.textSecondary)
    )

    Button(
        onClick = onOpenLogs,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.card,
            contentColor = palette.textPrimary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
    ) {
        Icon(Icons.Rounded.Analytics, contentDescription = null, tint = palette.liquid)
        Spacer(modifier = Modifier.width(10.dp))
        Text("Inspect Strategy & Race Logs")
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onExportBackup,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.emeraldSoft,
                contentColor = palette.emerald
            ),
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .border(1.dp, palette.emerald.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
        ) {
            Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Export JSON")
        }

        Button(
            onClick = onImportBackup,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.liquidSoft,
                contentColor = palette.liquid
            ),
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .border(1.dp, palette.liquid.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
        ) {
            Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Import JSON")
        }
    }

    HorizontalDivider(color = palette.border.copy(alpha = 0.6f))

    Button(
        onClick = onRequestClearAll,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.dangerSoft,
            contentColor = palette.danger
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .border(1.dp, palette.danger.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
    ) {
        Icon(Icons.Rounded.DeleteForever, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Clear All Local Chats & Logs")
    }
}

@Composable
private fun SectionLabel(text: String) {
    val palette = LocalStudioPalette.current
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = JetBrainsMono,
            color = palette.emerald,
            fontWeight = FontWeight.Bold
        )
    )
}

@Composable
private fun ToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.card)
            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(color = palette.textPrimary)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = palette.textSecondary)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.surface,
                checkedTrackColor = palette.emerald
            )
        )
    }
}

@Composable
private fun SelectableRowCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalStudioPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) palette.emeraldSoft else palette.card)
            .border(
                1.dp,
                if (selected) palette.emerald else palette.border,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                color = if (selected) palette.emerald else palette.textPrimary,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(color = palette.textSecondary)
        )
    }
}

@Composable
private fun ChoicePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) palette.emeraldSoft else palette.card)
            .border(
                1.dp,
                if (selected) palette.emerald else palette.border,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                color = if (selected) palette.emerald else palette.textSecondary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun SliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    val palette = LocalStudioPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.card)
            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall.copy(color = palette.textPrimary))
            Text(
                valueText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = JetBrainsMono,
                    color = palette.emerald,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = palette.emerald,
                activeTrackColor = palette.emerald
            )
        )
    }
}

@Composable
private fun PlainInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    val palette = LocalStudioPalette.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = palette.emerald,
            unfocusedBorderColor = palette.border,
            focusedContainerColor = palette.card,
            unfocusedContainerColor = palette.card,
            focusedTextColor = palette.textPrimary,
            unfocusedTextColor = palette.textPrimary
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SecretInputField(
    label: String,
    value: String,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
    onValueChange: (String) -> Unit
) {
    val palette = LocalStudioPalette.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (visible) "Hide key" else "Show key",
                    tint = palette.textMuted
                )
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = palette.emerald,
            unfocusedBorderColor = palette.border,
            focusedContainerColor = palette.card,
            unfocusedContainerColor = palette.card,
            focusedTextColor = palette.textPrimary,
            unfocusedTextColor = palette.textPrimary
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
