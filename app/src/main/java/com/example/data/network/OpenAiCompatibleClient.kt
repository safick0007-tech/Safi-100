package com.example.data.network

import android.graphics.Bitmap
import android.util.Base64
import com.example.domain.model.ModelParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class OpenAiCompatibleClient {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun chatCompletion(
        baseUrl: String,
        apiKey: String?,
        model: String,
        systemPrompt: String?,
        history: List<Pair<String, String>>,
        userPrompt: String,
        params: ModelParameters,
        attachedBitmap: Bitmap? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): String = withContext(Dispatchers.IO) {
        try {
            val cleanBase = baseUrl.trim().trimEnd('/')
            val endpoint = if (cleanBase.endsWith("/chat/completions")) {
                cleanBase
            } else {
                "$cleanBase/chat/completions"
            }

            val messagesArray = JSONArray()
            if (!systemPrompt.isNullOrBlank()) {
                messagesArray.put(
                    JSONObject()
                        .put("role", "system")
                        .put("content", systemPrompt)
                )
            }
            history.takeLast(10).forEach { (role, text) ->
                if (text.isNotBlank()) {
                    messagesArray.put(
                        JSONObject()
                            .put("role", if (role == "user") "user" else "assistant")
                            .put("content", text)
                    )
                }
            }

            if (attachedBitmap != null) {
                val base64 = bitmapToBase64Jpeg(attachedBitmap)
                val contentParts = JSONArray()
                    .put(
                        JSONObject()
                            .put("type", "text")
                            .put("text", userPrompt.ifBlank { "Please analyze this image." })
                    )
                    .put(
                        JSONObject()
                            .put("type", "image_url")
                            .put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$base64"))
                    )
                messagesArray.put(
                    JSONObject()
                        .put("role", "user")
                        .put("content", contentParts)
                )
            } else {
                messagesArray.put(
                    JSONObject()
                        .put("role", "user")
                        .put("content", userPrompt)
                )
            }

            val payload = JSONObject()
                .put("model", model)
                .put("messages", messagesArray)
                .put("temperature", params.temperature.toDouble())
                .put("top_p", params.topP.toDouble())
                .put("max_tokens", params.maxTokens)

            if (params.frequencyPenalty != 0f) {
                payload.put("frequency_penalty", params.frequencyPenalty.toDouble())
            }
            if (params.presencePenalty != 0f) {
                payload.put("presence_penalty", params.presencePenalty.toDouble())
            }

            val reqBuilder = Request.Builder()
                .url(endpoint)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")

            if (!apiKey.isNullOrBlank()) {
                reqBuilder.header("Authorization", "Bearer ${apiKey.trim()}")
            }
            extraHeaders.forEach { (k, v) -> reqBuilder.header(k, v) }

            httpClient.newCall(reqBuilder.build()).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errMsg = runCatching {
                        JSONObject(rawBody).optJSONObject("error")?.optString("message")
                    }.getOrNull()?.takeIf { it.isNotBlank() } ?: "HTTP ${response.code}"
                    throw RuntimeException("Provider error ($model): $errMsg")
                }

                val json = JSONObject(rawBody)
                val choices = json.optJSONArray("choices")
                val firstChoice = choices?.optJSONObject(0)
                val content = firstChoice?.optJSONObject("message")?.optString("content").orEmpty()
                content.trim().ifBlank { "The provider returned an empty response." }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw RuntimeException(e.message ?: "Network request failed", e)
        }
    }

    suspend fun testConnectionAndFetchModels(
        baseUrl: String,
        apiKey: String? = null
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanBase = baseUrl.trim().trimEnd('/')
            val endpoint = "$cleanBase/models"
            val reqBuilder = Request.Builder().url(endpoint).get()
            if (!apiKey.isNullOrBlank()) {
                reqBuilder.header("Authorization", "Bearer ${apiKey.trim()}")
            }
            httpClient.newCall(reqBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    error("HTTP ${response.code}: Unable to reach $endpoint")
                }
                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val data = json.optJSONArray("data") ?: JSONArray()
                val models = mutableListOf<String>()
                for (i in 0 until data.length()) {
                    val id = data.optJSONObject(i)?.optString("id").orEmpty()
                    if (id.isNotBlank()) models.add(id)
                }
                models
            }
        }
    }

    private fun bitmapToBase64Jpeg(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 82, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    companion object {
        const val OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1"
        const val VENICE_BASE_URL = "https://api.venice.ai/api/v1"
    }
}
