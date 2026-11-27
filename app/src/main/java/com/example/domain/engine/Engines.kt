package com.example.domain.engine

import com.example.domain.model.AutoTuneStrategy
import com.example.domain.model.ModelParameters
import com.example.domain.model.TastemakerScore
import kotlin.math.roundToInt

data class AutoTuneDecision(
    val detectedContext: String,
    val params: ModelParameters,
    val explanation: String
)

object AutoTuneEngine {
    private val codeKeywords = listOf(
        "code", "kotlin", "python", "java", "javascript", "typescript", "sql", "html", "css",
        "bug", "debug", "refactor", "function", "class", "api", "regex", "compile", "algorithm",
        "কোড", "প্রোগ্রাম", "অ্যাপ"
    )
    private val mathKeywords = listOf(
        "calculate", "equation", "solve", "proof", "integral", "derivative", "probability",
        "matrix", "formula", "statistics", "গণিত", "হিসাব", "সমীকরণ"
    )
    private val creativeKeywords = listOf(
        "story", "poem", "creative", "brainstorm", "slogan", "tagline", "script", "novel",
        "metaphor", "design idea", "কবিতা", "গল্প", "সৃজনশীল"
    )
    private val analysisKeywords = listOf(
        "compare", "analyze", "architecture", "trade-off", "evaluate", "strategy", "roadmap",
        "explain in detail", "deep dive", "বিশ্লেষণ", "তুলনা", "ব্যাখ্যা"
    )

    fun compute(
        prompt: String,
        enabled: Boolean,
        strategy: AutoTuneStrategy,
        manualParams: ModelParameters
    ): AutoTuneDecision {
        if (!enabled) {
            return AutoTuneDecision(
                detectedContext = "Manual",
                params = manualParams,
                explanation = "Using custom manual sampling (${manualParams.presetName})"
            )
        }

        val lower = prompt.lowercase()
        val detectedContext = when {
            codeKeywords.any { lower.contains(it) } -> "Code & Engineering"
            mathKeywords.any { lower.contains(it) } -> "Math & Logic"
            creativeKeywords.any { lower.contains(it) } -> "Creative & Ideation"
            analysisKeywords.any { lower.contains(it) } -> "Deep Analysis"
            prompt.length < 45 -> "Concise Q&A"
            else -> "General Synthesis"
        }

        val tuned = when (strategy) {
            AutoTuneStrategy.PRECISE -> ModelParameters(
                temperature = 0.25f,
                topP = 0.85f,
                maxTokens = 2048,
                presetName = "Precise"
            )
            AutoTuneStrategy.BALANCED -> ModelParameters(
                temperature = 0.65f,
                topP = 0.92f,
                maxTokens = 2048,
                presetName = "Balanced"
            )
            AutoTuneStrategy.CREATIVE -> ModelParameters(
                temperature = 1.05f,
                topP = 0.96f,
                maxTokens = 2560,
                presetName = "Creative"
            )
            AutoTuneStrategy.DEEP_ANALYTICAL -> ModelParameters(
                temperature = 0.45f,
                topP = 0.90f,
                maxTokens = 4096,
                presetName = "Deep Analytical"
            )
            AutoTuneStrategy.ADAPTIVE -> when (detectedContext) {
                "Code & Engineering" -> ModelParameters(
                    temperature = 0.28f,
                    topP = 0.88f,
                    maxTokens = 3072,
                    presetName = "Auto: Code"
                )
                "Math & Logic" -> ModelParameters(
                    temperature = 0.20f,
                    topP = 0.85f,
                    maxTokens = 2560,
                    presetName = "Auto: Logic"
                )
                "Creative & Ideation" -> ModelParameters(
                    temperature = 0.95f,
                    topP = 0.95f,
                    maxTokens = 2560,
                    presetName = "Auto: Creative"
                )
                "Deep Analysis" -> ModelParameters(
                    temperature = 0.50f,
                    topP = 0.90f,
                    maxTokens = 3584,
                    presetName = "Auto: Analysis"
                )
                "Concise Q&A" -> ModelParameters(
                    temperature = 0.55f,
                    topP = 0.90f,
                    maxTokens = 1536,
                    presetName = "Auto: Direct"
                )
                else -> ModelParameters(
                    temperature = 0.68f,
                    topP = 0.92f,
                    maxTokens = 2400,
                    presetName = "Auto: Balanced"
                )
            }
        }

        return AutoTuneDecision(
            detectedContext = detectedContext,
            params = tuned,
            explanation = "Context '$detectedContext' → temp ${tuned.temperature}, topP ${tuned.topP}, max ${tuned.maxTokens}"
        )
    }
}

object ScoringEngine {
    fun scoreResponse(
        prompt: String,
        response: String,
        durationMs: Long,
        isError: Boolean = false
    ): TastemakerScore {
        if (isError || response.isBlank()) {
            return TastemakerScore(
                totalScore = 10,
                qualityScore = 10,
                clarityScore = 10,
                speedScore = 10,
                grade = "POOR",
                rationale = "Empty or failed generation"
            )
        }

        val words = response.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val wordCount = words.size
        val hasCodeBlock = response.contains("```")
        val hasStructure = response.contains("\n-") ||
            response.contains("\n*") ||
            response.contains("\n1.") ||
            response.contains("##") ||
            response.contains("**")
        val promptNeedsCode = prompt.lowercase().let {
            it.contains("code") || it.contains("kotlin") || it.contains("python") ||
                it.contains("html") || it.contains("javascript") || it.contains("function")
        }

        // 1. Quality / Depth (0..100)
        var quality = when {
            wordCount < 12 -> 52
            wordCount < 40 -> 70
            wordCount in 40..380 -> 88
            wordCount in 381..900 -> 93
            else -> 89
        }
        if (hasStructure) quality += 5
        if (promptNeedsCode && hasCodeBlock) quality += 6
        if (promptNeedsCode && !hasCodeBlock && wordCount > 25) quality -= 10
        quality = quality.coerceIn(35, 100)

        // 2. Clarity & Directness (0..100)
        val fillerStarts = listOf(
            "sure, i can", "certainly!", "of course!", "as an ai", "i would be happy to"
        )
        val lowerStart = response.take(80).lowercase()
        var clarity = 84
        if (fillerStarts.any { lowerStart.contains(it) }) clarity -= 7
        val paragraphs = response.split("\n\n").count { it.isNotBlank() }
        if (paragraphs >= 2 || hasStructure) clarity += 8
        val distinctRatio = if (wordCount > 10) {
            words.map { it.lowercase() }.toSet().size.toFloat() / wordCount.toFloat()
        } else 0.7f
        if (distinctRatio > 0.52f) clarity += 5
        if (distinctRatio < 0.30f) clarity -= 14
        clarity = clarity.coerceIn(40, 100)

        // 3. Speed (0..100)
        val speed = when {
            durationMs <= 1800 -> 98
            durationMs <= 3500 -> 92
            durationMs <= 6000 -> 84
            durationMs <= 10000 -> 74
            durationMs <= 18000 -> 64
            else -> 54
        }

        // Composite weighted score (Quality 50%, Clarity 30%, Speed 20%)
        val total = (quality * 0.50f + clarity * 0.30f + speed * 0.20f)
            .roundToInt()
            .coerceIn(1, 100)

        val grade = when {
            total >= 91 -> "ELITE"
            total >= 83 -> "EXCELLENT"
            total >= 74 -> "GOOD"
            total >= 62 -> "ACCEPTABLE"
            else -> "POOR"
        }

        val rationaleParts = mutableListOf<String>()
        if (hasStructure) rationaleParts.add("well-structured")
        if (hasCodeBlock) rationaleParts.add("includes code")
        if (speed >= 90) rationaleParts.add("fast response")
        if (wordCount >= 120) rationaleParts.add("thorough depth")
        val rationale = if (rationaleParts.isEmpty()) {
            "Balanced response ($wordCount words)"
        } else {
            rationaleParts.joinToString(" · ") + " ($wordCount words)"
        }

        return TastemakerScore(
            totalScore = total,
            qualityScore = quality,
            clarityScore = clarity,
            speedScore = speed,
            grade = grade,
            rationale = rationale
        )
    }
}
