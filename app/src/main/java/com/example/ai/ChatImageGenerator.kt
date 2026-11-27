package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext
import kotlin.random.Random

private const val TAG = "ChatImageGenerator"
private const val AI_HORDE_API = "https://aihorde.net/api/v2"
private const val AI_HORDE_ANON_KEY = "0000000000"
private const val AI_HORDE_AGENT = "GodModeStudio:1.0:android"

data class GeneratedChatImage(
    val filePath: String,
    val resolvedPrompt: String,
    val engineLabel: String,
    val durationMs: Long
)

class ChatImageGenerator(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    companion object {
        private val BENGALI_IMAGE_TRIGGERS = listOf(
            "ফটো তৈরি", "ফটো বানা", "ফটো দাও", "ফটো আঁক", "ফটো জেনারেট",
            "ছবি তৈরি", "ছবি বানা", "ছবি দাও", "ছবি আঁক", "ছবি জেনারেট",
            "ইমেজ তৈরি", "ইমেজ বানা", "পিকচার তৈরি", "পিকচার বানা",
            "একটা ফটো", "একটি ফটো", "একটা ছবি", "একটি ছবি"
        )

        private val ENGLISH_IMAGE_TRIGGERS = listOf(
            "/image", "/img", "/photo", "/draw",
            "generate image", "generate a image", "generate an image",
            "generate photo", "generate a photo", "generate picture", "generate a picture",
            "create image", "create an image", "create a photo", "create photo", "create a picture",
            "make a photo", "make an image", "make a picture",
            "draw a ", "draw an ", "draw me ", "paint a ", "paint me ",
            "photo of ", "picture of ", "image of "
        )

        private val BENGALI_VISUAL_DICT = linkedMapOf(
            "রয়েল বেঙ্গল টাইগার" to "majestic Royal Bengal tiger",
            "বাঘের" to "majestic Bengal tiger",
            "বাঘ" to "majestic Bengal tiger",
            "সিংহের" to "majestic lion",
            "সিংহ" to "majestic lion",
            "বিড়ালের" to "cute fluffy cat",
            "বিড়াল" to "cute fluffy cat",
            "কুকুরের" to "loyal dog",
            "কুকুর" to "loyal dog",
            "ঘোড়ার" to "running horse",
            "ঘোড়া" to "running horse",
            "হাতির" to "majestic elephant",
            "হাতি" to "majestic elephant",
            "পাখির" to "colorful bird",
            "পাখি" to "colorful bird",
            "ময়ূর" to "vibrant peacock",
            "হরিণ" to "graceful deer",
            "মাছ" to "colorful fish",
            "গোলাপ" to "red rose flower",
            "ফুলের" to "blooming flowers",
            "ফুল" to "blooming flower",
            "সূর্যাস্ত" to "dramatic golden sunset",
            "সূর্যোদয়" to "glowing sunrise",
            "নদীর" to "scenic river",
            "নদী" to "scenic river",
            "সমুদ্রের" to "ocean waves beach",
            "সমুদ্র" to "ocean waves beach",
            "পাহাড়ের" to "majestic mountains",
            "পাহাড়" to "majestic mountains",
            "জঙ্গলের" to "lush green jungle forest",
            "জঙ্গল" to "lush green jungle forest",
            "বনের" to "deep forest",
            "বন" to "deep forest",
            "আকাশের" to "dramatic sky with clouds",
            "আকাশ" to "dramatic sky",
            "বৃষ্টি" to "atmospheric rain",
            "চাঁদ" to "glowing full moon night",
            "রাতের" to "starry night",
            "গাড়ির" to "sleek modern sports car",
            "গাড়ি" to "sleek modern sports car",
            "বাইক" to "modern sport motorcycle",
            "বাড়ির" to "modern luxury house",
            "বাড়ি" to "modern luxury house",
            "শহরের" to "futuristic glowing city skyline",
            "শহর" to "futuristic city skyline",
            "গ্রামের" to "peaceful green Bangladesh village",
            "গ্রাম" to "peaceful green village",
            "মানুষ" to "portrait of a person",
            "মেয়ে" to "portrait of a young woman",
            "ছেলে" to "portrait of a young man",
            "রাজা" to "royal king",
            "যোদ্ধা" to "cinematic warrior",
            "রোবট" to "futuristic sci-fi robot",
            "মহাকাশ" to "deep outer space nebula galaxy",
            "ড্রাগন" to "epic fantasy dragon",
            "প্রকৃতি" to "breathtaking nature landscape",
            "সুন্দর" to "beautiful",
            "বাস্তব" to "photorealistic",
            "কার্টুন" to "3d animated cartoon style",
            "অ্যানিমে" to "anime illustration style"
        )

        fun isImageGenerationRequest(prompt: String): Boolean {
            val trimmed = prompt.trim()
            if (trimmed.isEmpty()) return false
            val lower = trimmed.lowercase()
            if (BENGALI_IMAGE_TRIGGERS.any { trimmed.contains(it) }) return true
            if (ENGLISH_IMAGE_TRIGGERS.any { lower.contains(it) }) return true
            // Also detect Bengali "<noun> ফটো/ছবি" combined with action words
            val hasBengaliPhotoWord = trimmed.contains("ফটো") || trimmed.contains("ছবি") || trimmed.contains("ইমেজ")
            val hasBengaliAction = trimmed.contains("তৈরি") || trimmed.contains("বানা") ||
                trimmed.contains("দাও") || trimmed.contains("আঁক") || trimmed.contains("করে")
            return hasBengaliPhotoWord && hasBengaliAction
        }
    }

    suspend fun generateImage(
        userPrompt: String,
        attachedBitmap: Bitmap?,
        onStatus: suspend (String) -> Unit
    ): GeneratedChatImage {
        val startMs = System.currentTimeMillis()
        val firebaseReady = isFirebaseReady()

        onStatus("Understanding visual prompt…")
        val englishPrompt = resolveVisualPrompt(userPrompt, firebaseReady)

        var bitmap: Bitmap? = null
        var engineLabel = ""
        val errors = mutableListOf<String>()

        // Tier 1: Built-in Firebase AI Logic (Nano Banana 2.1 / Flash-Lite Image) when Firebase is initialized
        if (firebaseReady) {
            val models = listOf("gemini-nano-banana-2.1", "gemini-3.1-flash-lite-image")
            for (modelName in models) {
                try {
                    onStatus("Generating photo via $modelName…")
                    val generated = generateViaFirebaseAi(modelName, englishPrompt, attachedBitmap)
                    if (generated != null) {
                        bitmap = generated
                        engineLabel = "Gemini Image ($modelName)"
                        break
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "$modelName image generation failed: ${e.message}", e)
                    errors.add("$modelName: ${e.localizedMessage ?: "unavailable"}")
                }
            }
        }

        // Tier 2: Free keyless open-source AI Horde cluster (works on standalone Codemagic APKs without Firebase!)
        if (bitmap == null) {
            try {
                bitmap = generateViaAiHorde(englishPrompt, onStatus)
                engineLabel = "Free Open AI Image Engine"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "AI Horde image failed: ${e.message}", e)
                errors.add("Free AI Cluster: ${e.localizedMessage ?: "busy"}")
            }
        }

        // Tier 3: Keyless Pollinations fallback
        if (bitmap == null) {
            try {
                onStatus("Trying backup free image renderer…")
                bitmap = fetchPollinationsImage(englishPrompt)
                engineLabel = "Free Backup Image Engine"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Pollinations fallback failed: ${e.message}", e)
                errors.add("Backup renderer: ${e.localizedMessage ?: "unavailable"}")
            }
        }

        val finalBitmap = bitmap ?: throw RuntimeException(
            "Photo generation failed (${errors.joinToString(" · ")}). Please check internet and retry."
        )

        val outFile = withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "generated_images").apply { mkdirs() }
            val file = File(dir, "img_${UUID.randomUUID()}.png")
            FileOutputStream(file).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
            }
            if (!finalBitmap.isRecycled) finalBitmap.recycle()
            file
        }

        return GeneratedChatImage(
            filePath = outFile.absolutePath,
            resolvedPrompt = englishPrompt,
            engineLabel = engineLabel,
            durationMs = System.currentTimeMillis() - startMs
        )
    }

    private fun isFirebaseReady(): Boolean {
        return runCatching {
            if (FirebaseApp.getApps(context).isNotEmpty()) return true
            FirebaseApp.initializeApp(context) != null
        }.getOrDefault(false)
    }

    private suspend fun resolveVisualPrompt(rawPrompt: String, firebaseReady: Boolean): String {
        val cleaned = rawPrompt
            .replace(Regex("^/(image|img|photo|draw)\\s*", RegexOption.IGNORE_CASE), "")
            .trim()

        if (firebaseReady) {
            val refined = runCatching {
                withTimeout(20_000L) {
                    val model = Firebase.ai.generativeModel("gemini-3.8-flash")
                    val res = model.generateContent(
                        "Translate and expand this user image request into ONE vivid, detailed English photo generation prompt (under 40 words, no quotes, no preamble): $cleaned"
                    )
                    res.text?.trim()?.takeIf { it.isNotBlank() }
                }
            }.getOrNull()
            if (!refined.isNullOrBlank()) return refined
        }

        // Offline Bengali -> English visual subject mapper for standalone APKs
        val matchedConcepts = mutableListOf<String>()
        BENGALI_VISUAL_DICT.forEach { (bn, en) ->
            if (cleaned.contains(bn) && !matchedConcepts.contains(en)) {
                matchedConcepts.add(en)
            }
        }

        val hasNonAscii = cleaned.any { it.code > 127 }
        return when {
            matchedConcepts.isNotEmpty() ->
                "${matchedConcepts.joinToString(", ")}, ultra detailed photorealistic photography, crisp natural lighting, vivid colors, 8k"
            !hasNonAscii ->
                "$cleaned, ultra detailed photorealistic photography, dramatic natural lighting, sharp focus"
            else ->
                "$cleaned, high detail cinematic photorealistic illustration, vibrant natural lighting"
        }
    }

    private suspend fun generateViaFirebaseAi(
        modelName: String,
        prompt: String,
        attachedBitmap: Bitmap?
    ): Bitmap? {
        val model = Firebase.ai.generativeModel(
            modelName = modelName,
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
                imageConfig = ImageConfig.builder()
                    .setImageSize(ImageSize.SIZE_1K)
                    .build()
            }
        )
        val response = withTimeout(60_000L) {
            if (attachedBitmap != null) {
                model.generateContent(
                    content {
                        image(attachedBitmap)
                        text(prompt)
                    }
                )
            } else {
                model.generateContent(prompt)
            }
        }
        return response.candidates.firstOrNull()
            ?.content?.parts?.filterIsInstance<ImagePart>()
            ?.firstOrNull()?.image
    }

    private suspend fun generateViaAiHorde(
        prompt: String,
        onStatus: suspend (String) -> Unit
    ): Bitmap {
        onStatus("Free AI Photo Engine · Starting generation…")
        val seed = Random.nextInt(10_000, 999_999)

        val jobId = withContext(Dispatchers.IO) {
            val body = JSONObject().apply {
                put("prompt", prompt.take(450))
                put(
                    "params",
                    JSONObject().apply {
                        put("width", 512)
                        put("height", 512)
                        put("steps", 20)
                        put("cfg_scale", 7.0)
                        put("sampler_name", "k_euler_a")
                        put("seed", seed.toString())
                        put("n", 1)
                    }
                )
                put("nsfw", false)
                put("censor_nsfw", true)
                put("trusted_workers", false)
                put("slow_workers", true)
                put("r2", true)
            }.toString()

            val req = Request.Builder()
                .url("$AI_HORDE_API/generate/async")
                .header("apikey", AI_HORDE_ANON_KEY)
                .header("Client-Agent", AI_HORDE_AGENT)
                .header("Content-Type", "application/json")
                .post(body.toRequestBody(jsonMedia))
                .build()

            httpClient.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val msg = runCatching { JSONObject(text).optString("message") }.getOrNull()
                    throw RuntimeException("HTTP ${resp.code} ${msg.orEmpty()}".trim())
                }
                JSONObject(text).optString("id").takeIf { it.isNotBlank() }
                    ?: throw RuntimeException("Missing job id")
            }
        }

        // Poll lightweight check endpoint up to ~100s
        val maxPolls = 40
        for (attempt in 1..maxPolls) {
            coroutineContext.ensureActive()
            delay(2_500L)

            val check = withContext(Dispatchers.IO) {
                val req = Request.Builder()
                    .url("$AI_HORDE_API/generate/check/$jobId")
                    .header("Client-Agent", AI_HORDE_AGENT)
                    .get()
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw RuntimeException("Status HTTP ${resp.code}")
                    JSONObject(resp.body?.string().orEmpty())
                }
            }

            if (check.optBoolean("faulted", false)) throw RuntimeException("Worker faulted")
            if (!check.optBoolean("is_possible", true)) throw RuntimeException("No worker available")
            if (check.optBoolean("done", false)) break

            val wait = check.optInt("wait_time", 0)
            val pos = check.optInt("queue_position", 0)
            val statusMsg = when {
                check.optInt("processing", 0) > 0 -> "Free AI Photo Engine · Painting photo…"
                wait > 0 -> "Free AI Photo Engine · Queue #$pos (~${wait}s)…"
                else -> "Free AI Photo Engine · Generating photo…"
            }
            onStatus(statusMsg)

            if (attempt == maxPolls) throw RuntimeException("Image generation timed out")
        }

        onStatus("Downloading generated photo…")
        return withContext(Dispatchers.IO) {
            val statusReq = Request.Builder()
                .url("$AI_HORDE_API/generate/status/$jobId")
                .header("Client-Agent", AI_HORDE_AGENT)
                .get()
                .build()
            val imgField = httpClient.newCall(statusReq).execute().use { resp ->
                if (!resp.isSuccessful) throw RuntimeException("Result HTTP ${resp.code}")
                val json = JSONObject(resp.body?.string().orEmpty())
                val arr = json.optJSONArray("generations")
                if (arr == null || arr.length() == 0) throw RuntimeException("No image returned")
                arr.getJSONObject(0).optString("img", "")
            }

            if (imgField.isBlank()) throw RuntimeException("Empty image data")

            val bytes = if (imgField.startsWith("http://") || imgField.startsWith("https://")) {
                val dlReq = Request.Builder()
                    .url(imgField)
                    .header("User-Agent", "GodModeStudio/1.0 Android")
                    .get()
                    .build()
                httpClient.newCall(dlReq).execute().use { r ->
                    if (!r.isSuccessful) throw RuntimeException("Download HTTP ${r.code}")
                    r.body?.bytes() ?: throw RuntimeException("Empty image bytes")
                }
            } else {
                Base64.decode(imgField, Base64.DEFAULT)
            }

            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: throw RuntimeException("Unable to decode generated photo")
        }
    }

    private suspend fun fetchPollinationsImage(prompt: String): Bitmap = withContext(Dispatchers.IO) {
        val seed = Random.nextInt(10_000, 999_999)
        val encoded = URLEncoder.encode(prompt.take(260), "UTF-8").replace("+", "%20")
        val url = "https://image.pollinations.ai/prompt/$encoded?width=768&height=768&model=flux&seed=$seed&nologo=true"
        withTimeout(45_000L) {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "GodModeStudio/1.0 Android")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}")
                val bytes = resp.body?.bytes() ?: throw RuntimeException("Empty image payload")
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: throw RuntimeException("Invalid image format")
            }
        }
    }
}
