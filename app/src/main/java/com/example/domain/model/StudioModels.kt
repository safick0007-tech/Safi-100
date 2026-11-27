package com.example.domain.model

import com.squareup.moshi.JsonClass

enum class ChatMode(
    val displayName: String,
    val shortLabel: String,
    val description: String
) {
    ULTRA(
        displayName = "Ultra Arena",
        shortLabel = "Ultra",
        description = "Parallel multi-model race with live quality judging and optional coaching pass"
    ),
    CLASSIC(
        displayName = "Classic Arena",
        shortLabel = "Classic",
        description = "Races curated expert personas side-by-side for versatile, structured answers"
    ),
    PROMPT_LAB(
        displayName = "Prompt Lab",
        shortLabel = "Prompt Lab",
        description = "Direct streaming single-model workbench with AutoTune parameter control"
    )
}

enum class SpeedTier(
    val displayName: String,
    val racerCount: Int,
    val description: String
) {
    FAST("Fast (2x)", 2, "2 rapid racers for quick comparisons"),
    STANDARD("Standard (3x)", 3, "3 balanced racers across speed and depth"),
    SMART("Smart (4x)", 4, "4 specialized racers with deeper synthesis"),
    POWER("Power (4x+)", 4, "4 high-detail racers with richer reasoning"),
    ULTRA("Ultra (5x)", 5, "5 parallel racers for maximum coverage")
}

enum class AutoTuneStrategy(val displayName: String, val summary: String) {
    ADAPTIVE("Adaptive (Auto)", "Automatically detects query intent and tunes sampling"),
    PRECISE("Precise", "Low temperature for deterministic code, math, and facts"),
    BALANCED("Balanced", "Versatile general-purpose clarity and fluency"),
    CREATIVE("Creative", "Higher diversity for writing, ideation, and storytelling"),
    DEEP_ANALYTICAL("Deep Analytical", "Expanded token budget for structured reasoning")
}

enum class WaitingGameType(val displayName: String) {
    NONE("Off"),
    SNAKE("Snake"),
    GAME_2048("2048"),
    PONG("Pong")
}

enum class ProviderSource(val displayName: String) {
    BUILTIN_GEMINI("Built-in Gemini"),
    OPENROUTER("OpenRouter"),
    VENICE("Venice AI"),
    LOCAL("Local Server")
}

@JsonClass(generateAdapter = true)
data class ModelParameters(
    val temperature: Float = 0.7f,
    val topP: Float = 0.92f,
    val maxTokens: Int = 2048,
    val frequencyPenalty: Float = 0.0f,
    val presencePenalty: Float = 0.0f,
    val presetName: String = "Balanced"
)

@JsonClass(generateAdapter = true)
data class TastemakerScore(
    val totalScore: Int,
    val qualityScore: Int,
    val clarityScore: Int,
    val speedScore: Int,
    val grade: String,
    val rationale: String
)

@JsonClass(generateAdapter = true)
data class RaceCandidate(
    val id: String,
    val label: String,
    val modelId: String,
    val provider: String,
    val personaTag: String,
    val content: String,
    val durationMs: Long,
    val score: TastemakerScore?,
    val isWinner: Boolean = false,
    val isError: Boolean = false
)

@JsonClass(generateAdapter = true)
data class LiquidVersion(
    val versionIndex: Int,
    val label: String,
    val content: String,
    val durationMs: Long,
    val modelLabel: String
)

@JsonClass(generateAdapter = true)
data class MagicMetadata(
    val modeName: String,
    val winnerModel: String,
    val totalDurationMs: Long,
    val score: TastemakerScore? = null,
    val autoTuneContext: String = "General",
    val appliedParams: ModelParameters = ModelParameters(),
    val processLogs: List<String> = emptyList(),
    val providerUsed: String = ProviderSource.BUILTIN_GEMINI.displayName,
    val coachingApplied: Boolean = false
)

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val isUser: Boolean,
    val content: String,
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val statusText: String? = null,
    val raceCandidates: List<RaceCandidate> = emptyList(),
    val activeRaceIndex: Int = 0,
    val liquidVersions: List<LiquidVersion> = emptyList(),
    val activeLiquidIndex: Int = 0,
    val magic: MagicMetadata? = null,
    val isError: Boolean = false
) {
    val displayedContent: String
        get() {
            if (liquidVersions.isNotEmpty() && activeLiquidIndex in liquidVersions.indices) {
                return liquidVersions[activeLiquidIndex].content
            }
            if (raceCandidates.isNotEmpty() && activeRaceIndex in raceCandidates.indices) {
                return raceCandidates[activeRaceIndex].content
            }
            return content
        }
}

data class Conversation(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val messageCount: Int = 0
)

data class StrategyLogEntry(
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

enum class StudioThemePreset(
    val displayName: String,
    val description: String,
    val isLight: Boolean
) {
    OBSIDIAN_EMERALD(
        displayName = "Obsidian Emerald",
        description = "Signature deep slate dark with crisp emerald & cyan accents",
        isLight = false
    ),
    MIDNIGHT_NORD(
        displayName = "Midnight Slate",
        description = "Low-glare blue-charcoal dark tuned for long reading sessions",
        isLight = false
    ),
    CRISP_PAPER(
        displayName = "Crisp Paper",
        description = "Warm high-contrast light surface with deep ink legibility",
        isLight = true
    )
}

data class StudioSettings(
    val activeMode: ChatMode = ChatMode.ULTRA,
    val speedTier: SpeedTier = SpeedTier.STANDARD,
    val earlyStopEnabled: Boolean = true,
    val earlyStopThreshold: Int = 86,
    val coachingEnabled: Boolean = false,
    val classicFullCombo: Boolean = true,
    val classicSelectedPersona: String = "Systems Architect",
    val promptLabModel: String = "gemini-3.8-flash",
    val liquidEnabled: Boolean = false,
    val liquidPasses: Int = 1,
    val autoTuneEnabled: Boolean = true,
    val autoTuneStrategy: AutoTuneStrategy = AutoTuneStrategy.ADAPTIVE,
    val modelParameters: ModelParameters = ModelParameters(),
    val customSystemPromptEnabled: Boolean = false,
    val customSystemPrompt: String = "",
    val waitingGame: WaitingGameType = WaitingGameType.NONE,
    val preferredProvider: ProviderSource = ProviderSource.BUILTIN_GEMINI,
    val openRouterApiKey: String = "",
    val openRouterModel: String = "openai/gpt-4o-mini",
    val veniceApiKey: String = "",
    val veniceModel: String = "llama-3.3-70b",
    val localEndpointUrl: String = "http://10.0.2.2:11434/v1",
    val localModelName: String = "llama3.2",
    val themePreset: StudioThemePreset = StudioThemePreset.OBSIDIAN_EMERALD,
    val useDynamicColor: Boolean = false,
    val useLightTheme: Boolean = false,
    val promptCount: Int = 0,
    val disclaimerAccepted: Boolean = false
)
