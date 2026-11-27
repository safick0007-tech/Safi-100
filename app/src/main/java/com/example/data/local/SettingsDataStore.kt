package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AutoTuneStrategy
import com.example.domain.model.ChatMode
import com.example.domain.model.ModelParameters
import com.example.domain.model.ProviderSource
import com.example.domain.model.SpeedTier
import com.example.domain.model.StudioSettings
import com.example.domain.model.StudioThemePreset
import com.example.domain.model.WaitingGameType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studioDataStore: DataStore<Preferences> by preferencesDataStore(name = "godmode_studio_prefs")

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val ACTIVE_MODE = stringPreferencesKey("active_mode")
        val SPEED_TIER = stringPreferencesKey("speed_tier")
        val EARLY_STOP_ENABLED = booleanPreferencesKey("early_stop_enabled")
        val EARLY_STOP_THRESHOLD = intPreferencesKey("early_stop_threshold")
        val COACHING_ENABLED = booleanPreferencesKey("coaching_enabled")
        val CLASSIC_FULL_COMBO = booleanPreferencesKey("classic_full_combo")
        val CLASSIC_SELECTED_PERSONA = stringPreferencesKey("classic_selected_persona")
        val PROMPT_LAB_MODEL = stringPreferencesKey("prompt_lab_model")
        val LIQUID_ENABLED = booleanPreferencesKey("liquid_enabled")
        val LIQUID_PASSES = intPreferencesKey("liquid_passes")
        val AUTOTUNE_ENABLED = booleanPreferencesKey("autotune_enabled")
        val AUTOTUNE_STRATEGY = stringPreferencesKey("autotune_strategy")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val TOP_P = floatPreferencesKey("top_p")
        val MAX_TOKENS = intPreferencesKey("max_tokens")
        val FREQ_PENALTY = floatPreferencesKey("freq_penalty")
        val PRES_PENALTY = floatPreferencesKey("pres_penalty")
        val PRESET_NAME = stringPreferencesKey("preset_name")
        val CUSTOM_PROMPT_ENABLED = booleanPreferencesKey("custom_prompt_enabled")
        val CUSTOM_PROMPT = stringPreferencesKey("custom_prompt")
        val WAITING_GAME = stringPreferencesKey("waiting_game")
        val PREFERRED_PROVIDER = stringPreferencesKey("preferred_provider")
        val OPENROUTER_KEY = stringPreferencesKey("openrouter_key")
        val OPENROUTER_MODEL = stringPreferencesKey("openrouter_model")
        val VENICE_KEY = stringPreferencesKey("venice_key")
        val VENICE_MODEL = stringPreferencesKey("venice_model")
        val LOCAL_URL = stringPreferencesKey("local_url")
        val LOCAL_MODEL = stringPreferencesKey("local_model")
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val USE_LIGHT_THEME = booleanPreferencesKey("use_light_theme")
        val PROMPT_COUNT = intPreferencesKey("prompt_count")
        val DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")
    }

    val settingsFlow: Flow<StudioSettings> = context.studioDataStore.data.map { prefs ->
        StudioSettings(
            activeMode = prefs[Keys.ACTIVE_MODE]?.let { runCatching { ChatMode.valueOf(it) }.getOrNull() }
                ?: ChatMode.ULTRA,
            speedTier = prefs[Keys.SPEED_TIER]?.let { runCatching { SpeedTier.valueOf(it) }.getOrNull() }
                ?: SpeedTier.STANDARD,
            earlyStopEnabled = prefs[Keys.EARLY_STOP_ENABLED] ?: true,
            earlyStopThreshold = prefs[Keys.EARLY_STOP_THRESHOLD] ?: 86,
            coachingEnabled = prefs[Keys.COACHING_ENABLED] ?: false,
            classicFullCombo = prefs[Keys.CLASSIC_FULL_COMBO] ?: true,
            classicSelectedPersona = prefs[Keys.CLASSIC_SELECTED_PERSONA] ?: "Systems Architect",
            promptLabModel = prefs[Keys.PROMPT_LAB_MODEL] ?: "gemini-3.8-flash",
            liquidEnabled = prefs[Keys.LIQUID_ENABLED] ?: false,
            liquidPasses = (prefs[Keys.LIQUID_PASSES] ?: 1).coerceIn(1, 2),
            autoTuneEnabled = prefs[Keys.AUTOTUNE_ENABLED] ?: true,
            autoTuneStrategy = prefs[Keys.AUTOTUNE_STRATEGY]
                ?.let { runCatching { AutoTuneStrategy.valueOf(it) }.getOrNull() }
                ?: AutoTuneStrategy.ADAPTIVE,
            modelParameters = ModelParameters(
                temperature = prefs[Keys.TEMPERATURE] ?: 0.7f,
                topP = prefs[Keys.TOP_P] ?: 0.92f,
                maxTokens = prefs[Keys.MAX_TOKENS] ?: 2048,
                frequencyPenalty = prefs[Keys.FREQ_PENALTY] ?: 0f,
                presencePenalty = prefs[Keys.PRES_PENALTY] ?: 0f,
                presetName = prefs[Keys.PRESET_NAME] ?: "Balanced"
            ),
            customSystemPromptEnabled = prefs[Keys.CUSTOM_PROMPT_ENABLED] ?: false,
            customSystemPrompt = prefs[Keys.CUSTOM_PROMPT] ?: "",
            waitingGame = prefs[Keys.WAITING_GAME]
                ?.let { runCatching { WaitingGameType.valueOf(it) }.getOrNull() }
                ?: WaitingGameType.NONE,
            preferredProvider = prefs[Keys.PREFERRED_PROVIDER]
                ?.let { runCatching { ProviderSource.valueOf(it) }.getOrNull() }
                ?: ProviderSource.BUILTIN_GEMINI,
            openRouterApiKey = prefs[Keys.OPENROUTER_KEY] ?: "",
            openRouterModel = prefs[Keys.OPENROUTER_MODEL] ?: "openai/gpt-4o-mini",
            veniceApiKey = prefs[Keys.VENICE_KEY] ?: "",
            veniceModel = prefs[Keys.VENICE_MODEL] ?: "llama-3.3-70b",
            localEndpointUrl = prefs[Keys.LOCAL_URL] ?: "http://10.0.2.2:11434/v1",
            localModelName = prefs[Keys.LOCAL_MODEL] ?: "llama3.2",
            themePreset = prefs[Keys.THEME_PRESET]
                ?.let { runCatching { StudioThemePreset.valueOf(it) }.getOrNull() }
                ?: if (prefs[Keys.USE_LIGHT_THEME] == true) StudioThemePreset.CRISP_PAPER else StudioThemePreset.OBSIDIAN_EMERALD,
            useDynamicColor = prefs[Keys.USE_DYNAMIC_COLOR] ?: false,
            useLightTheme = prefs[Keys.USE_LIGHT_THEME] ?: false,
            promptCount = prefs[Keys.PROMPT_COUNT] ?: 0,
            disclaimerAccepted = prefs[Keys.DISCLAIMER_ACCEPTED] ?: false
        )
    }

    suspend fun updateSettings(transform: (StudioSettings) -> StudioSettings) {
        context.studioDataStore.edit { prefs ->
            val current = StudioSettings(
                activeMode = prefs[Keys.ACTIVE_MODE]?.let { runCatching { ChatMode.valueOf(it) }.getOrNull() } ?: ChatMode.ULTRA,
                speedTier = prefs[Keys.SPEED_TIER]?.let { runCatching { SpeedTier.valueOf(it) }.getOrNull() } ?: SpeedTier.STANDARD,
                earlyStopEnabled = prefs[Keys.EARLY_STOP_ENABLED] ?: true,
                earlyStopThreshold = prefs[Keys.EARLY_STOP_THRESHOLD] ?: 86,
                coachingEnabled = prefs[Keys.COACHING_ENABLED] ?: false,
                classicFullCombo = prefs[Keys.CLASSIC_FULL_COMBO] ?: true,
                classicSelectedPersona = prefs[Keys.CLASSIC_SELECTED_PERSONA] ?: "Systems Architect",
                promptLabModel = prefs[Keys.PROMPT_LAB_MODEL] ?: "gemini-3.8-flash",
                liquidEnabled = prefs[Keys.LIQUID_ENABLED] ?: false,
                liquidPasses = prefs[Keys.LIQUID_PASSES] ?: 1,
                autoTuneEnabled = prefs[Keys.AUTOTUNE_ENABLED] ?: true,
                autoTuneStrategy = prefs[Keys.AUTOTUNE_STRATEGY]?.let { runCatching { AutoTuneStrategy.valueOf(it) }.getOrNull() } ?: AutoTuneStrategy.ADAPTIVE,
                modelParameters = ModelParameters(
                    temperature = prefs[Keys.TEMPERATURE] ?: 0.7f,
                    topP = prefs[Keys.TOP_P] ?: 0.92f,
                    maxTokens = prefs[Keys.MAX_TOKENS] ?: 2048,
                    frequencyPenalty = prefs[Keys.FREQ_PENALTY] ?: 0f,
                    presencePenalty = prefs[Keys.PRES_PENALTY] ?: 0f,
                    presetName = prefs[Keys.PRESET_NAME] ?: "Balanced"
                ),
                customSystemPromptEnabled = prefs[Keys.CUSTOM_PROMPT_ENABLED] ?: false,
                customSystemPrompt = prefs[Keys.CUSTOM_PROMPT] ?: "",
                waitingGame = prefs[Keys.WAITING_GAME]?.let { runCatching { WaitingGameType.valueOf(it) }.getOrNull() } ?: WaitingGameType.NONE,
                preferredProvider = prefs[Keys.PREFERRED_PROVIDER]?.let { runCatching { ProviderSource.valueOf(it) }.getOrNull() } ?: ProviderSource.BUILTIN_GEMINI,
                openRouterApiKey = prefs[Keys.OPENROUTER_KEY] ?: "",
                openRouterModel = prefs[Keys.OPENROUTER_MODEL] ?: "openai/gpt-4o-mini",
                veniceApiKey = prefs[Keys.VENICE_KEY] ?: "",
                veniceModel = prefs[Keys.VENICE_MODEL] ?: "llama-3.3-70b",
                localEndpointUrl = prefs[Keys.LOCAL_URL] ?: "http://10.0.2.2:11434/v1",
                localModelName = prefs[Keys.LOCAL_MODEL] ?: "llama3.2",
                themePreset = prefs[Keys.THEME_PRESET]
                    ?.let { runCatching { StudioThemePreset.valueOf(it) }.getOrNull() }
                    ?: if (prefs[Keys.USE_LIGHT_THEME] == true) StudioThemePreset.CRISP_PAPER else StudioThemePreset.OBSIDIAN_EMERALD,
                useDynamicColor = prefs[Keys.USE_DYNAMIC_COLOR] ?: false,
                useLightTheme = prefs[Keys.USE_LIGHT_THEME] ?: false,
                promptCount = prefs[Keys.PROMPT_COUNT] ?: 0,
                disclaimerAccepted = prefs[Keys.DISCLAIMER_ACCEPTED] ?: false
            )
            val updated = transform(current)
            prefs[Keys.ACTIVE_MODE] = updated.activeMode.name
            prefs[Keys.SPEED_TIER] = updated.speedTier.name
            prefs[Keys.EARLY_STOP_ENABLED] = updated.earlyStopEnabled
            prefs[Keys.EARLY_STOP_THRESHOLD] = updated.earlyStopThreshold
            prefs[Keys.COACHING_ENABLED] = updated.coachingEnabled
            prefs[Keys.CLASSIC_FULL_COMBO] = updated.classicFullCombo
            prefs[Keys.CLASSIC_SELECTED_PERSONA] = updated.classicSelectedPersona
            prefs[Keys.PROMPT_LAB_MODEL] = updated.promptLabModel
            prefs[Keys.LIQUID_ENABLED] = updated.liquidEnabled
            prefs[Keys.LIQUID_PASSES] = updated.liquidPasses
            prefs[Keys.AUTOTUNE_ENABLED] = updated.autoTuneEnabled
            prefs[Keys.AUTOTUNE_STRATEGY] = updated.autoTuneStrategy.name
            prefs[Keys.TEMPERATURE] = updated.modelParameters.temperature
            prefs[Keys.TOP_P] = updated.modelParameters.topP
            prefs[Keys.MAX_TOKENS] = updated.modelParameters.maxTokens
            prefs[Keys.FREQ_PENALTY] = updated.modelParameters.frequencyPenalty
            prefs[Keys.PRES_PENALTY] = updated.modelParameters.presencePenalty
            prefs[Keys.PRESET_NAME] = updated.modelParameters.presetName
            prefs[Keys.CUSTOM_PROMPT_ENABLED] = updated.customSystemPromptEnabled
            prefs[Keys.CUSTOM_PROMPT] = updated.customSystemPrompt
            prefs[Keys.WAITING_GAME] = updated.waitingGame.name
            prefs[Keys.PREFERRED_PROVIDER] = updated.preferredProvider.name
            prefs[Keys.OPENROUTER_KEY] = updated.openRouterApiKey
            prefs[Keys.OPENROUTER_MODEL] = updated.openRouterModel
            prefs[Keys.VENICE_KEY] = updated.veniceApiKey
            prefs[Keys.VENICE_MODEL] = updated.veniceModel
            prefs[Keys.LOCAL_URL] = updated.localEndpointUrl
            prefs[Keys.LOCAL_MODEL] = updated.localModelName
            prefs[Keys.THEME_PRESET] = updated.themePreset.name
            prefs[Keys.USE_DYNAMIC_COLOR] = updated.useDynamicColor
            prefs[Keys.USE_LIGHT_THEME] = updated.themePreset.isLight
            prefs[Keys.PROMPT_COUNT] = updated.promptCount
            prefs[Keys.DISCLAIMER_ACCEPTED] = updated.disclaimerAccepted
        }
    }

    suspend fun incrementPromptCount() {
        context.studioDataStore.edit { prefs ->
            prefs[Keys.PROMPT_COUNT] = (prefs[Keys.PROMPT_COUNT] ?: 0) + 1
        }
    }
}
