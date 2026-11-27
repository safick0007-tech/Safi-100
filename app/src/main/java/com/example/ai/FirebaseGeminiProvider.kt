package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.domain.model.ModelParameters
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

private const val TAG = "FirebaseGeminiProvider"
private const val HORDE_TEXT_BASE = "https://aihorde.net/api/v2/generate/text"
private const val HORDE_ANON_KEY = "0000000000"
private const val HORDE_AGENT = "GodModeStudio:1.0:android"

object FirebaseGeminiProvider {
    const val MODEL_FLASH = "gemini-3.8-flash"
    const val MODEL_LITE = "gemini-3.5-flash-lite"

    private const val TIMEOUT_MS = 60_000L

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    fun isFirebaseInitialized(): Boolean {
        return runCatching {
            FirebaseApp.getInstance() != null
        }.getOrDefault(false)
    }

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
        if (!isFirebaseInitialized()) {
            return generateViaFreeTextFallback(systemInstruction, history, userPrompt, params)
        }
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
            Log.e(TAG, "Gemini ($modelName) failed", e)
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
        if (!isFirebaseInitialized()) {
            val reply = generateViaFreeTextFallback(systemInstruction, history, userPrompt, params)
            emit(reply)
            return@flow
        }
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
            Log.e(TAG, "Gemini stream ($modelName) failed", e)
            throw RuntimeException("Gemini stream ($modelName): ${e.localizedMessage ?: "Stream failed"}", e)
        }
    }

    /**
     * Keyless free fallback for standalone APKs built without google-services.json (e.g., via GitHub/Codemagic).
     * Uses AI Horde's free open-source text generation network.
     */
    private suspend fun generateViaFreeTextFallback(
        systemInstruction: String?,
        history: List<Pair<String, String>>,
        userPrompt: String,
        params: ModelParameters
    ): String {
        val promptBlock = buildString {
            val sys = systemInstruction?.takeIf { it.isNotBlank() }
                ?: "You are GodMode Studio, a helpful, clear AI assistant. Reply concisely in the same language as the user."
            appendLine("System: $sys")
            appendLine()
            history.takeLast(4).forEach { (role, text) ->
                if (text.isNotBlank()) {
                    appendLine(if (role == "user") "User: ${text.take(300)}" else "Assistant: ${text.take(300)}")
                }
            }
            appendLine("User: $userPrompt")
            append("Assistant:")
        }

        val jobId = withContext(Dispatchers.IO) {
            val body = JSONObject().apply {
                put("prompt", promptBlock)
                put(
                    "params",
                    JSONObject().apply {
                        put("max_context_length", 2048)
                        put("max_length", 256)
                        put("temperature", params.temperature.coerceIn(0.2f, 1.2f).toDouble())
                        put("top_p", params.topP.coerceIn(0.5f, 0.98f).toDouble())
                    }
                )
                put("trusted_workers", false)
                put("slow_workers", true)
            }.toString()

            val req = Request.Builder()
                .url("$HORDE_TEXT_BASE/async")
                .header("apikey", HORDE_ANON_KEY)
                .header("Client-Agent", HORDE_AGENT)
                .header("Content-Type", "application/json")
                .post(body.toRequestBody(jsonMedia))
                .build()

            httpClient.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw RuntimeException(
                        "Standalone APK has no Firebase config and Free AI Cluster returned HTTP ${resp.code}. " +
                            "Configure OpenRouter/Venice in Settings → Providers or download the APK from AI Studio."
                    )
                }
                JSONObject(raw).optString("id").takeIf { it.isNotBlank() }
                    ?: throw RuntimeException("Free AI Cluster did not return a request ID.")
            }
        }

        // Poll up to 60 seconds
        for (attempt in 1..30) {
            coroutineContext.ensureActive()
            delay(2_000L)
            val statusJson = withContext(Dispatchers.IO) {
                val req = Request.Builder()
                    .url("$HORDE_TEXT_BASE/status/$jobId")
                    .header("Client-Agent", HORDE_AGENT)
                    .get()
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw RuntimeException("Free AI Cluster HTTP ${resp.code}")
                    JSONObject(resp.body?.string().orEmpty())
                }
            }
            if (statusJson.optBoolean("faulted", false)) {
                throw RuntimeException("Free AI worker faulted. Please retry.")
            }
            if (statusJson.optBoolean("done", false)) {
                val gens = statusJson.optJSONArray("generations")
                val reply = gens?.optJSONObject(0)?.optString("text", "")?.trim().orEmpty()
                if (reply.isNotBlank()) return reply
                throw RuntimeException("Free AI Cluster returned an empty response.")
            }
        }

        throw RuntimeException("Free AI Cluster timed out. Please retry or add a provider in Settings → Providers.")
    }
}
