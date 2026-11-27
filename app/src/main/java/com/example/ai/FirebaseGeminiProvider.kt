package com.example.ai

import android.graphics.Bitmap
import com.example.domain.model.ModelParameters
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

object FirebaseGeminiProvider {
    const val MODEL_FLASH = "gemini-3.8-flash"
    const val MODEL_LITE = "gemini-3.5-flash-lite"

    private const val TIMEOUT_MS = 60_000L

    private fun buildModel(
        modelName: String,
        systemInstruction: String?,
        params: ModelParameters
    ): GenerativeModel {
        val config = generationConfig {
            temperature = params.temperature.coerceIn(0f, 2f)
            topP = params.topP.coerceIn(0.05f, 1f)
            maxOutputTokens = params.maxTokens.coerceIn(128, 8192)
        }
        val sysContent = systemInstruction?.takeIf { it.isNotBlank() }?.let {
            content { text(it) }
        }
        return Firebase.ai.generativeModel(
            modelName = modelName,
            generationConfig = config,
            systemInstruction = sysContent
        )
    }

    private fun buildHistoryAndPrompt(
        history: List<Pair<String, String>>,
        userPrompt: String,
        attachedBitmap: Bitmap?
    ): List<Content> {
        val contents = mutableListOf<Content>()
        // Keep recent context bounded for fast latency and clean token budgets
        history.takeLast(12).forEach { (role, text) ->
            if (text.isNotBlank()) {
                contents.add(
                    content(role = if (role == "user") "user" else "model") {
                        text(text)
                    }
                )
            }
        }
        contents.add(
            content(role = "user") {
                if (attachedBitmap != null) {
                    image(attachedBitmap)
                }
                text(userPrompt.ifBlank { "Please analyze this image in detail." })
            }
        )
        return contents
    }

    suspend fun generateOnce(
        modelName: String,
        systemInstruction: String?,
        history: List<Pair<String, String>>,
        userPrompt: String,
        params: ModelParameters,
        attachedBitmap: Bitmap? = null
    ): String {
        return try {
            withTimeout(TIMEOUT_MS) {
                val model = buildModel(modelName, systemInstruction, params)
                val inputs = buildHistoryAndPrompt(history, userPrompt, attachedBitmap)
                val response = model.generateContent(inputs)
                response.text?.trim().orEmpty()
                    .ifBlank { "The model returned an empty response." }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw RuntimeException("Gemini ($modelName): ${e.localizedMessage ?: "Request failed"}", e)
        }
    }

    fun generateStream(
        modelName: String,
        systemInstruction: String?,
        history: List<Pair<String, String>>,
        userPrompt: String,
        params: ModelParameters,
        attachedBitmap: Bitmap? = null
    ): Flow<String> = flow {
        val model = buildModel(modelName, systemInstruction, params)
        val inputs = buildHistoryAndPrompt(history, userPrompt, attachedBitmap)
        val buffer = StringBuilder()
        try {
            withTimeout(TIMEOUT_MS) {
                model.generateContentStream(inputs).collect { chunk ->
                    val piece = chunk.text
                    if (!piece.isNullOrEmpty()) {
                        buffer.append(piece)
                        emit(buffer.toString())
                    }
                }
            }
            if (buffer.isEmpty()) {
                emit("No content received from $modelName.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw RuntimeException("Gemini stream ($modelName): ${e.localizedMessage ?: "Stream failed"}", e)
        }
    }
}
