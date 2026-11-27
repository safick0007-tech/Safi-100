package com.example.domain.engine

import android.graphics.Bitmap
import com.example.ai.FirebaseGeminiProvider
import com.example.data.network.OpenAiCompatibleClient
import com.example.domain.model.ChatMode
import com.example.domain.model.LiquidVersion
import com.example.domain.model.MagicMetadata
import com.example.domain.model.ModelParameters
import com.example.domain.model.ProviderSource
import com.example.domain.model.RaceCandidate
import com.example.domain.model.StudioSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

data class OrchestratorUpdate(
    val content: String,
    val statusText: String?,
    val raceCandidates: List<RaceCandidate> = emptyList(),
    val activeRaceIndex: Int = 0,
    val liquidVersions: List<LiquidVersion> = emptyList(),
    val activeLiquidIndex: Int = 0,
    val magic: MagicMetadata? = null,
    val isFinal: Boolean = false,
    val isError: Boolean = false
)

private data class RacerBlueprint(
    val label: String,
    val modelId: String,
    val provider: ProviderSource,
    val personaTag: String,
    val systemDirective: String,
    val tempDelta: Float = 0f
)

class ChatOrchestrator(
    private val openAiClient: OpenAiCompatibleClient
) {
    companion object {
        val CLASSIC_PERSONAS = listOf(
            "Fast Direct" to "Deliver an immediate, crisp, high-signal answer with zero filler. Lead with the exact takeaway.",
            "Systems Architect" to "Analyze the request structurally. Break down trade-offs, edge cases, and clear step-by-step architecture.",
            "Deep Analyst" to "Provide a thorough, nuanced breakdown with clear headings, concrete examples, and verified reasoning.",
            "Creative Strategist" to "Offer fresh, insightful angles and practical innovations while keeping the response structured and actionable.",
            "Code Specialist" to "Focus on production-grade technical precision, clean idiomatic examples, and defensive best practices."
        )
    }

    suspend fun execute(
        prompt: String,
        history: List<Pair<String, String>>,
        settings: StudioSettings,
        attachedBitmap: Bitmap?,
        onUpdate: suspend (OrchestratorUpdate) -> Unit
    ): OrchestratorUpdate {
        val startWallMs = System.currentTimeMillis()
        val autoTune = AutoTuneEngine.compute(
            prompt = prompt,
            enabled = settings.autoTuneEnabled,
            strategy = settings.autoTuneStrategy,
            manualParams = settings.modelParameters
        )
        val processLogs = mutableListOf(
            "Mode: ${settings.activeMode.displayName}",
            "AutoTune: ${autoTune.explanation}"
        )

        val customSys = if (settings.customSystemPromptEnabled && settings.customSystemPrompt.isNotBlank()) {
            processLogs.add("Custom system prompt applied (${settings.customSystemPrompt.length} chars)")
            settings.customSystemPrompt.trim()
        } else null

        return when (settings.activeMode) {
            ChatMode.PROMPT_LAB -> runPromptLab(
                prompt = prompt,
                history = history,
                settings = settings,
                autoTune = autoTune,
                customSys = customSys,
                attachedBitmap = attachedBitmap,
                processLogs = processLogs,
                startWallMs = startWallMs,
                onUpdate = onUpdate
            )
            ChatMode.ULTRA, ChatMode.CLASSIC -> runArenaRace(
                prompt = prompt,
                history = history,
                settings = settings,
                autoTune = autoTune,
                customSys = customSys,
                attachedBitmap = attachedBitmap,
                processLogs = processLogs,
                startWallMs = startWallMs,
                onUpdate = onUpdate
            )
        }
    }

    private suspend fun runPromptLab(
        prompt: String,
        history: List<Pair<String, String>>,
        settings: StudioSettings,
        autoTune: AutoTuneDecision,
        customSys: String?,
        attachedBitmap: Bitmap?,
        processLogs: MutableList<String>,
        startWallMs: Long,
        onUpdate: suspend (OrchestratorUpdate) -> Unit
    ): OrchestratorUpdate {
        val activeProvider = resolveProvider(settings)
        val modelId = resolveSingleModelId(settings, activeProvider)
        processLogs.add("Provider: ${activeProvider.displayName} · Model: $modelId")

        onUpdate(
            OrchestratorUpdate(
                content = "",
                statusText = "Streaming from $modelId…"
            )
        )

        var streamedText = ""
        if (activeProvider == ProviderSource.BUILTIN_GEMINI) {
            FirebaseGeminiProvider.generateStream(
                modelName = modelId,
                systemInstruction = customSys,
                history = history,
                userPrompt = prompt,
                params = autoTune.params,
                attachedBitmap = attachedBitmap
            ).collect { partial ->
                streamedText = partial
                onUpdate(
                    OrchestratorUpdate(
                        content = partial,
                        statusText = "Streaming · $modelId"
                    )
                )
            }
        } else {
            streamedText = callProviderOnce(
                provider = activeProvider,
                modelId = modelId,
                systemPrompt = customSys,
                history = history,
                userPrompt = prompt,
                params = autoTune.params,
                settings = settings,
                attachedBitmap = attachedBitmap
            )
            onUpdate(
                OrchestratorUpdate(
                    content = streamedText,
                    statusText = "Scoring response…"
                )
            )
        }

        val initialDuration = System.currentTimeMillis() - startWallMs
        val score = ScoringEngine.scoreResponse(prompt, streamedText, initialDuration)
        processLogs.add("Tastemaker score: ${score.totalScore}/100 (${score.grade}) in ${formatSec(initialDuration)}")

        val liquidVersions = if (settings.liquidEnabled) {
            runLiquidRefinement(
                prompt = prompt,
                initialDraft = streamedText,
                initialDurationMs = initialDuration,
                initialLabel = modelId,
                settings = settings,
                activeProvider = activeProvider,
                customSys = customSys,
                processLogs = processLogs,
                onProgress = { versions, status ->
                    onUpdate(
                        OrchestratorUpdate(
                            content = versions.last().content,
                            statusText = status,
                            liquidVersions = versions,
                            activeLiquidIndex = versions.lastIndex
                        )
                    )
                }
            )
        } else emptyList()

        val totalMs = System.currentTimeMillis() - startWallMs
        val finalContent = liquidVersions.lastOrNull()?.content ?: streamedText
        val finalScore = if (liquidVersions.size > 1) {
            ScoringEngine.scoreResponse(prompt, finalContent, totalMs)
        } else score

        val finalUpdate = OrchestratorUpdate(
            content = finalContent,
            statusText = null,
            liquidVersions = liquidVersions,
            activeLiquidIndex = (liquidVersions.size - 1).coerceAtLeast(0),
            magic = MagicMetadata(
                modeName = settings.activeMode.displayName,
                winnerModel = modelId,
                totalDurationMs = totalMs,
                score = finalScore,
                autoTuneContext = autoTune.detectedContext,
                appliedParams = autoTune.params,
                processLogs = processLogs,
                providerUsed = activeProvider.displayName
            ),
            isFinal = true
        )
        onUpdate(finalUpdate)
        return finalUpdate
    }

    private suspend fun runArenaRace(
        prompt: String,
        history: List<Pair<String, String>>,
        settings: StudioSettings,
        autoTune: AutoTuneDecision,
        customSys: String?,
        attachedBitmap: Bitmap?,
        processLogs: MutableList<String>,
        startWallMs: Long,
        onUpdate: suspend (OrchestratorUpdate) -> Unit
    ): OrchestratorUpdate {
        val activeProvider = resolveProvider(settings)
        val racers = buildRacerBlueprints(settings, activeProvider)
        processLogs.add("Launching ${racers.size} parallel racers via ${activeProvider.displayName}")

        onUpdate(
            OrchestratorUpdate(
                content = "",
                statusText = "Racing ${racers.size} models in parallel…"
            )
        )

        val mutex = Mutex()
        val completed = mutableListOf<RaceCandidate>()
        var earlyWinnerTriggered = false

        coroutineScope {
            val jobs: List<Deferred<RaceCandidate>> = racers.map { racer ->
                async {
                    val t0 = System.currentTimeMillis()
                    val mergedSystem = listOfNotNull(customSys, racer.systemDirective)
                        .joinToString("\n\n")
                    val racerParams = autoTune.params.copy(
                        temperature = (autoTune.params.temperature + racer.tempDelta).coerceIn(0.1f, 1.5f)
                    )
                    try {
                        val text = callProviderOnce(
                            provider = racer.provider,
                            modelId = racer.modelId,
                            systemPrompt = mergedSystem,
                            history = history,
                            userPrompt = prompt,
                            params = racerParams,
                            settings = settings,
                            attachedBitmap = attachedBitmap
                        )
                        val elapsed = System.currentTimeMillis() - t0
                        val scored = ScoringEngine.scoreResponse(prompt, text, elapsed)
                        val candidate = RaceCandidate(
                            id = UUID.randomUUID().toString(),
                            label = racer.label,
                            modelId = racer.modelId,
                            provider = racer.provider.displayName,
                            personaTag = racer.personaTag,
                            content = text,
                            durationMs = elapsed,
                            score = scored
                        )

                        mutex.withLock {
                            completed.add(candidate)
                            val sortedNow = completed.sortedByDescending { it.score?.totalScore ?: 0 }
                            val leader = sortedNow.first()
                            processLogs.add(
                                "Finished ${candidate.label} (${candidate.score?.totalScore ?: 0}/100 · ${formatSec(elapsed)})"
                            )
                            onUpdate(
                                OrchestratorUpdate(
                                    content = leader.content,
                                    statusText = "Finished ${completed.size}/${racers.size} · Leader: ${leader.label} (${leader.score?.totalScore ?: 0}/100)",
                                    raceCandidates = sortedNow,
                                    activeRaceIndex = 0
                                )
                            )
                            if (settings.activeMode == ChatMode.ULTRA &&
                                settings.earlyStopEnabled &&
                                !earlyWinnerTriggered &&
                                completed.size >= 2 &&
                                (leader.score?.totalScore ?: 0) >= settings.earlyStopThreshold
                            ) {
                                earlyWinnerTriggered = true
                                processLogs.add("Early-stop threshold (${settings.earlyStopThreshold}) reached by ${leader.label}")
                            }
                        }
                        candidate
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        val elapsed = System.currentTimeMillis() - t0
                        val errCandidate = RaceCandidate(
                            id = UUID.randomUUID().toString(),
                            label = racer.label,
                            modelId = racer.modelId,
                            provider = racer.provider.displayName,
                            personaTag = racer.personaTag,
                            content = "Error from ${racer.label}: ${e.localizedMessage ?: "Request failed"}",
                            durationMs = elapsed,
                            score = ScoringEngine.scoreResponse(prompt, "", elapsed, isError = true),
                            isError = true
                        )
                        mutex.withLock {
                            completed.add(errCandidate)
                            processLogs.add("Racer ${racer.label} failed: ${e.localizedMessage}")
                        }
                        errCandidate
                    }
                }
            }
            jobs.awaitAll()
        }

        val validCandidates = completed.filter { !it.isError }
        if (validCandidates.isEmpty()) {
            val errMsg = completed.firstOrNull()?.content ?: "All model racers failed. Check connection or provider settings."
            val errUpdate = OrchestratorUpdate(
                content = errMsg,
                statusText = null,
                raceCandidates = completed,
                activeRaceIndex = 0,
                isFinal = true,
                isError = true
            )
            onUpdate(errUpdate)
            return errUpdate
        }

        var ranked = completed
            .sortedWith(compareBy<RaceCandidate> { it.isError }.thenByDescending { it.score?.totalScore ?: 0 })
            .mapIndexed { idx, c -> c.copy(isWinner = idx == 0) }

        var winner = ranked.first()
        var coachingApplied = false

        // Optional AI Coaching Pass in Ultra Arena: synthesize the top 2 valid answers into a champion
        if (settings.activeMode == ChatMode.ULTRA && settings.coachingEnabled && validCandidates.size >= 2) {
            onUpdate(
                OrchestratorUpdate(
                    content = winner.content,
                    statusText = "Running AI Coaching synthesis pass…",
                    raceCandidates = ranked,
                    activeRaceIndex = 0
                )
            )
            val top2 = validCandidates.sortedByDescending { it.score?.totalScore ?: 0 }.take(2)
            val coachStart = System.currentTimeMillis()
            val coachPrompt = buildString {
                appendLine("You are an expert synthesis editor. Combine the best accuracy, clarity, and structure from these two answers to the user's question into one superior, cohesive final response. Output only the final answer.")
                appendLine()
                appendLine("User question: $prompt")
                appendLine()
                appendLine("--- Candidate A (${top2[0].label}) ---")
                appendLine(top2[0].content)
                appendLine()
                appendLine("--- Candidate B (${top2[1].label}) ---")
                appendLine(top2[1].content)
            }
            val coachedResult = runCatching {
                callProviderOnce(
                    provider = activeProvider,
                    modelId = resolveCoachModelId(settings, activeProvider),
                    systemPrompt = customSys,
                    history = emptyList(),
                    userPrompt = coachPrompt,
                    params = autoTune.params.copy(temperature = 0.4f),
                    settings = settings,
                    attachedBitmap = null
                )
            }.getOrNull()

            if (!coachedResult.isNullOrBlank()) {
                val coachMs = System.currentTimeMillis() - coachStart
                val coachScore = ScoringEngine.scoreResponse(prompt, coachedResult, coachMs)
                val coachedCandidate = RaceCandidate(
                    id = UUID.randomUUID().toString(),
                    label = "Coached Champion",
                    modelId = resolveCoachModelId(settings, activeProvider),
                    provider = activeProvider.displayName,
                    personaTag = "AI Synthesis",
                    content = coachedResult,
                    durationMs = coachMs,
                    score = coachScore,
                    isWinner = true
                )
                ranked = listOf(coachedCandidate) + ranked.map { it.copy(isWinner = false) }
                winner = coachedCandidate
                coachingApplied = true
                processLogs.add("Coaching synthesis finished (${coachScore.totalScore}/100 in ${formatSec(coachMs)})")
            }
        }

        // Optional Liquid Refinement Pass
        val liquidVersions = if (settings.liquidEnabled) {
            runLiquidRefinement(
                prompt = prompt,
                initialDraft = winner.content,
                initialDurationMs = winner.durationMs,
                initialLabel = winner.label,
                settings = settings,
                activeProvider = activeProvider,
                customSys = customSys,
                processLogs = processLogs,
                onProgress = { versions, status ->
                    onUpdate(
                        OrchestratorUpdate(
                            content = versions.last().content,
                            statusText = status,
                            raceCandidates = ranked,
                            activeRaceIndex = 0,
                            liquidVersions = versions,
                            activeLiquidIndex = versions.lastIndex
                        )
                    )
                }
            )
        } else emptyList()

        val totalMs = System.currentTimeMillis() - startWallMs
        val finalContent = liquidVersions.lastOrNull()?.content ?: winner.content
        val finalScore = if (liquidVersions.size > 1) {
            ScoringEngine.scoreResponse(prompt, finalContent, totalMs)
        } else winner.score

        val finalUpdate = OrchestratorUpdate(
            content = finalContent,
            statusText = null,
            raceCandidates = ranked,
            activeRaceIndex = 0,
            liquidVersions = liquidVersions,
            activeLiquidIndex = (liquidVersions.size - 1).coerceAtLeast(0),
            magic = MagicMetadata(
                modeName = settings.activeMode.displayName,
                winnerModel = winner.label,
                totalDurationMs = totalMs,
                score = finalScore,
                autoTuneContext = autoTune.detectedContext,
                appliedParams = autoTune.params,
                processLogs = processLogs,
                providerUsed = activeProvider.displayName,
                coachingApplied = coachingApplied
            ),
            isFinal = true
        )
        onUpdate(finalUpdate)
        return finalUpdate
    }

    private suspend fun runLiquidRefinement(
        prompt: String,
        initialDraft: String,
        initialDurationMs: Long,
        initialLabel: String,
        settings: StudioSettings,
        activeProvider: ProviderSource,
        customSys: String?,
        processLogs: MutableList<String>,
        onProgress: suspend (List<LiquidVersion>, String) -> Unit
    ): List<LiquidVersion> {
        val versions = mutableListOf(
            LiquidVersion(
                versionIndex = 0,
                label = "v0 · Initial",
                content = initialDraft,
                durationMs = initialDurationMs,
                modelLabel = initialLabel
            )
        )
        onProgress(versions.toList(), "Liquid Mode: Refining draft (v1)…")

        val passes = settings.liquidPasses.coerceIn(1, 2)
        var currentDraft = initialDraft
        for (pass in 1..passes) {
            val t0 = System.currentTimeMillis()
            val instruction = if (pass == 1) {
                "Refine and elevate this draft response to the user's question. Improve structure, factual sharpness, readability, and remove any fluff while preserving every helpful detail. Output only the improved answer."
            } else {
                "Perform a final executive polish on this response: ensure crisp formatting, effortless scannability, zero redundancy, and complete accuracy. Output only the final polished answer."
            }
            val refinePrompt = """
                $instruction

                Original User Question:
                $prompt

                Current Draft:
                $currentDraft
            """.trimIndent()

            val refined = runCatching {
                callProviderOnce(
                    provider = activeProvider,
                    modelId = resolveCoachModelId(settings, activeProvider),
                    systemPrompt = customSys,
                    history = emptyList(),
                    userPrompt = refinePrompt,
                    params = ModelParameters(temperature = 0.35f, topP = 0.9f, maxTokens = 3072),
                    settings = settings,
                    attachedBitmap = null
                )
            }.getOrNull()

            if (!refined.isNullOrBlank()) {
                val dt = System.currentTimeMillis() - t0
                currentDraft = refined
                val passLabel = if (pass == 1) "v1 · Refined" else "v2 · Polished"
                versions.add(
                    LiquidVersion(
                        versionIndex = pass,
                        label = passLabel,
                        content = refined,
                        durationMs = dt,
                        modelLabel = "Liquid Pass $pass"
                    )
                )
                processLogs.add("Liquid $passLabel completed in ${formatSec(dt)}")
                onProgress(
                    versions.toList(),
                    if (pass < passes) "Liquid Mode: Executive polish (v${pass + 1})…" else "Liquid refinement complete"
                )
            }
        }
        return versions
    }

    private fun buildRacerBlueprints(
        settings: StudioSettings,
        provider: ProviderSource
    ): List<RacerBlueprint> {
        if (settings.activeMode == ChatMode.CLASSIC) {
            val chosen = if (settings.classicFullCombo) {
                CLASSIC_PERSONAS
            } else {
                listOf(
                    CLASSIC_PERSONAS.find { it.first == settings.classicSelectedPersona }
                        ?: CLASSIC_PERSONAS.first()
                )
            }
            return chosen.mapIndexed { idx, (name, directive) ->
                val modelId = when (provider) {
                    ProviderSource.BUILTIN_GEMINI -> if (idx % 2 == 0) {
                        FirebaseGeminiProvider.MODEL_FLASH
                    } else {
                        FirebaseGeminiProvider.MODEL_LITE
                    }
                    ProviderSource.OPENROUTER -> settings.openRouterModel.ifBlank { "openai/gpt-4o-mini" }
                    ProviderSource.VENICE -> settings.veniceModel.ifBlank { "llama-3.3-70b" }
                    ProviderSource.LOCAL -> settings.localModelName.ifBlank { "llama3.2" }
                }
                RacerBlueprint(
                    label = name,
                    modelId = modelId,
                    provider = provider,
                    personaTag = "Classic Persona",
                    systemDirective = directive,
                    tempDelta = when (idx) {
                        0 -> -0.15f
                        3 -> 0.18f
                        else -> 0f
                    }
                )
            }
        }

        // Ultra Arena blueprints scaled by SpeedTier
        val allUltraRacers = when (provider) {
            ProviderSource.BUILTIN_GEMINI -> listOf(
                RacerBlueprint(
                    label = "Gemini 3.8 Flash · Core",
                    modelId = FirebaseGeminiProvider.MODEL_FLASH,
                    provider = ProviderSource.BUILTIN_GEMINI,
                    personaTag = "Balanced Precision",
                    systemDirective = "Provide a clear, thorough, well-structured answer directly addressing the user's goal.",
                    tempDelta = 0f
                ),
                RacerBlueprint(
                    label = "Gemini 3.5 Lite · Turbo",
                    modelId = FirebaseGeminiProvider.MODEL_LITE,
                    provider = ProviderSource.BUILTIN_GEMINI,
                    personaTag = "Speed First",
                    systemDirective = "Deliver a rapid, concise, high-impact answer with crisp takeaways and zero verbosity.",
                    tempDelta = -0.12f
                ),
                RacerBlueprint(
                    label = "Gemini 3.8 Flash · Deep",
                    modelId = FirebaseGeminiProvider.MODEL_FLASH,
                    provider = ProviderSource.BUILTIN_GEMINI,
                    personaTag = "Deep Reasoning",
                    systemDirective = "Reason step-by-step, verify edge cases, and present an authoritative, structured breakdown.",
                    tempDelta = -0.20f
                ),
                RacerBlueprint(
                    label = "Gemini 3.8 Flash · Creative",
                    modelId = FirebaseGeminiProvider.MODEL_FLASH,
                    provider = ProviderSource.BUILTIN_GEMINI,
                    personaTag = "Lateral Synthesis",
                    systemDirective = "Offer insightful, engaging, and practical perspectives with clear examples and clean formatting.",
                    tempDelta = 0.22f
                ),
                RacerBlueprint(
                    label = "Gemini 3.5 Lite · Concise",
                    modelId = FirebaseGeminiProvider.MODEL_LITE,
                    provider = ProviderSource.BUILTIN_GEMINI,
                    personaTag = "Executive Summary",
                    systemDirective = "Format the answer as an executive brief with scannable bullet points and actionable next steps.",
                    tempDelta = -0.05f
                )
            )
            ProviderSource.OPENROUTER -> {
                val primary = settings.openRouterModel.ifBlank { "openai/gpt-4o-mini" }
                listOf(
                    RacerBlueprint("OpenRouter · Primary", primary, provider, "Primary", "Provide a clear, accurate, well-structured answer.", 0f),
                    RacerBlueprint("OpenRouter · Claude", "anthropic/claude-3.5-haiku", provider, "Clarity", "Be lucid, precise, and thoughtfully structured.", -0.1f),
                    RacerBlueprint("OpenRouter · DeepSeek", "deepseek/deepseek-chat", provider, "Analytical", "Analyze systematically with step-by-step clarity.", -0.15f),
                    RacerBlueprint("OpenRouter · Gemini", "google/gemini-2.0-flash-001", provider, "Speed", "Deliver a fast, clean, comprehensive response.", 0.05f),
                    RacerBlueprint("OpenRouter · Llama", "meta-llama/llama-3.3-70b-instruct", provider, "Creative", "Give an insightful, well-illustrated answer.", 0.15f)
                )
            }
            ProviderSource.VENICE -> {
                val primary = settings.veniceModel.ifBlank { "llama-3.3-70b" }
                listOf(
                    RacerBlueprint("Venice · Direct", primary, provider, "Direct", "Be concise, clear, and directly useful.", -0.1f),
                    RacerBlueprint("Venice · Deep", primary, provider, "Deep Analysis", "Provide thorough, structured analysis.", -0.15f),
                    RacerBlueprint("Venice · Creative", primary, provider, "Creative", "Offer fresh, vivid, practical angles.", 0.2f),
                    RacerBlueprint("Venice · Technical", primary, provider, "Technical", "Prioritize technical precision and examples.", -0.2f),
                    RacerBlueprint("Venice · Executive", primary, provider, "Brief", "Use crisp headings and bullet points.", 0f)
                )
            }
            ProviderSource.LOCAL -> {
                val localModel = settings.localModelName.ifBlank { "llama3.2" }
                listOf(
                    RacerBlueprint("Local · Direct", localModel, provider, "Direct", "Be clear and direct.", -0.1f),
                    RacerBlueprint("Local · Structured", localModel, provider, "Structured", "Organize with clean headings and steps.", -0.15f),
                    RacerBlueprint("Local · Creative", localModel, provider, "Creative", "Give an imaginative yet practical answer.", 0.2f),
                    RacerBlueprint("Local · Precise", localModel, provider, "Precise", "Verify every detail carefully.", -0.2f),
                    RacerBlueprint("Local · Concise", localModel, provider, "Summary", "Keep it concise and high-signal.", 0f)
                )
            }
        }

        return allUltraRacers.take(settings.speedTier.racerCount.coerceIn(1, allUltraRacers.size))
    }

    private fun resolveProvider(settings: StudioSettings): ProviderSource {
        return when (settings.preferredProvider) {
            ProviderSource.OPENROUTER -> if (settings.openRouterApiKey.isNotBlank()) ProviderSource.OPENROUTER else ProviderSource.BUILTIN_GEMINI
            ProviderSource.VENICE -> if (settings.veniceApiKey.isNotBlank()) ProviderSource.VENICE else ProviderSource.BUILTIN_GEMINI
            ProviderSource.LOCAL -> if (settings.localEndpointUrl.isNotBlank()) ProviderSource.LOCAL else ProviderSource.BUILTIN_GEMINI
            ProviderSource.BUILTIN_GEMINI -> ProviderSource.BUILTIN_GEMINI
        }
    }

    private fun resolveSingleModelId(settings: StudioSettings, provider: ProviderSource): String {
        return when (provider) {
            ProviderSource.BUILTIN_GEMINI -> when (settings.promptLabModel) {
                FirebaseGeminiProvider.MODEL_LITE -> FirebaseGeminiProvider.MODEL_LITE
                else -> FirebaseGeminiProvider.MODEL_FLASH
            }
            ProviderSource.OPENROUTER -> settings.openRouterModel.ifBlank { "openai/gpt-4o-mini" }
            ProviderSource.VENICE -> settings.veniceModel.ifBlank { "llama-3.3-70b" }
            ProviderSource.LOCAL -> settings.localModelName.ifBlank { "llama3.2" }
        }
    }

    private fun resolveCoachModelId(settings: StudioSettings, provider: ProviderSource): String {
        return when (provider) {
            ProviderSource.BUILTIN_GEMINI -> FirebaseGeminiProvider.MODEL_FLASH
            ProviderSource.OPENROUTER -> settings.openRouterModel.ifBlank { "openai/gpt-4o-mini" }
            ProviderSource.VENICE -> settings.veniceModel.ifBlank { "llama-3.3-70b" }
            ProviderSource.LOCAL -> settings.localModelName.ifBlank { "llama3.2" }
        }
    }

    private suspend fun callProviderOnce(
        provider: ProviderSource,
        modelId: String,
        systemPrompt: String?,
        history: List<Pair<String, String>>,
        userPrompt: String,
        params: ModelParameters,
        settings: StudioSettings,
        attachedBitmap: Bitmap?
    ): String {
        return when (provider) {
            ProviderSource.BUILTIN_GEMINI -> FirebaseGeminiProvider.generateOnce(
                modelName = modelId,
                systemInstruction = systemPrompt,
                history = history,
                userPrompt = userPrompt,
                params = params,
                attachedBitmap = attachedBitmap
            )
            ProviderSource.OPENROUTER -> openAiClient.chatCompletion(
                baseUrl = OpenAiCompatibleClient.OPENROUTER_BASE_URL,
                apiKey = settings.openRouterApiKey,
                model = modelId,
                systemPrompt = systemPrompt,
                history = history,
                userPrompt = userPrompt,
                params = params,
                attachedBitmap = attachedBitmap,
                extraHeaders = mapOf(
                    "HTTP-Referer" to "https://godmode.studio.app",
                    "X-Title" to "GodMode Studio"
                )
            )
            ProviderSource.VENICE -> openAiClient.chatCompletion(
                baseUrl = OpenAiCompatibleClient.VENICE_BASE_URL,
                apiKey = settings.veniceApiKey,
                model = modelId,
                systemPrompt = systemPrompt,
                history = history,
                userPrompt = userPrompt,
                params = params,
                attachedBitmap = attachedBitmap
            )
            ProviderSource.LOCAL -> openAiClient.chatCompletion(
                baseUrl = settings.localEndpointUrl,
                apiKey = null,
                model = modelId,
                systemPrompt = systemPrompt,
                history = history,
                userPrompt = userPrompt,
                params = params,
                attachedBitmap = attachedBitmap
            )
        }
    }

    private fun formatSec(ms: Long): String = String.format("%.1fs", ms / 1000.0)
}
