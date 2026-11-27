package com.example.domain.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.domain.model.FreeVideoModel
import com.example.domain.model.VideoCameraMotion
import com.example.domain.model.VideoClipRecord
import com.example.domain.model.VideoRatio
import com.example.domain.model.VideoRenderProgress
import com.example.domain.model.VideoStudioMode
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext
import kotlin.random.Random

private const val TAG = "FreeVideoGenerator"
private const val AI_HORDE_API_BASE = "https://aihorde.net/api/v2"
private const val AI_HORDE_ANON_KEY = "0000000000"
private const val AI_HORDE_CLIENT_AGENT = "GodModeStudio:1.0:android"

private data class PlannedShotList(
    val beats: List<String>,
    val cameraMotion: VideoCameraMotion
)

class FreeVideoGenerator(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val shotSchema = Schema.obj(
        properties = mapOf(
            "cameraMotion" to Schema.enumeration(
                values = listOf("ZOOM_IN", "ZOOM_OUT", "PAN_RIGHT", "PAN_LEFT", "ORBIT_DRIFT"),
                description = "Recommended cinematic camera trajectory"
            ),
            "beats" to Schema.array(
                items = Schema.string(description = "Vivid English visual prompt for one sequential frame in the video"),
                description = "Consecutive visual keyframes with strict subject, lighting, and setting continuity"
            )
        )
    )

    private fun isFirebaseReady(): Boolean {
        return runCatching {
            if (FirebaseApp.getApps(context).isNotEmpty()) return true
            FirebaseApp.initializeApp(context) != null
        }.getOrDefault(false)
    }

    private fun buildDirectorModel(): GenerativeModel {
        return Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.65f
                responseMimeType = "application/json"
                responseSchema = shotSchema
            }
        )
    }

    private fun buildImageModel(modelName: String, ratio: VideoRatio): GenerativeModel {
        val aspect = when (ratio) {
            VideoRatio.LANDSCAPE_16_9 -> AspectRatio.LANDSCAPE_16x9
            VideoRatio.PORTRAIT_9_16 -> AspectRatio.PORTRAIT_9x16
        }
        return Firebase.ai.generativeModel(
            modelName = modelName,
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
                imageConfig = ImageConfig.builder()
                    .setAspectRatio(aspect)
                    .setImageSize(ImageSize.SIZE_1K)
                    .build()
            }
        )
    }

    suspend fun generate(
        prompt: String,
        mode: VideoStudioMode,
        model: FreeVideoModel,
        ratio: VideoRatio,
        durationSec: Int,
        cameraMotion: VideoCameraMotion,
        anchorBitmap: Bitmap?,
        parentClip: VideoClipRecord?,
        onProgress: suspend (VideoRenderProgress) -> Unit
    ): VideoClipRecord {
        val safeSec = durationSec.coerceIn(5, 15)
        val firebaseAvailable = isFirebaseReady()

        // Keep beat count responsive: 2-4 beats with Firebase AI, 2-3 beats on free community AI Horde cluster
        val totalBeats = if (firebaseAvailable) {
            when {
                safeSec <= 7 -> 2
                safeSec <= 11 -> 3
                else -> 4
            }
        } else {
            if (safeSec <= 10) 2 else 3
        }

        // Step 1: Shot Choreography (Firebase Gemini 3.8 Flash if initialized, else structured cinematic planner)
        onProgress(
            VideoRenderProgress(
                stageTitle = "${model.displayName} · Directing",
                detail = "Planning $totalBeats continuous beats (${safeSec}s · ${ratio.label})…",
                progress = 0.08f
            )
        )

        val plan = planShots(
            prompt = prompt,
            mode = mode,
            model = model,
            ratio = ratio,
            beatCount = totalBeats,
            anchorBitmap = anchorBitmap,
            firebaseAvailable = firebaseAvailable
        )

        val effectiveMotion = if (cameraMotion == VideoCameraMotion.AUTO) {
            plan.cameraMotion
        } else {
            cameraMotion
        }

        // Step 2: Synthesize continuous AI keyframes
        // Tier 1: Firebase AI Logic (Nano Banana 2.1 / Flash-Lite Image) when Firebase is initialized
        // Tier 2: Free keyless open-source AI Horde cluster (works on standalone APKs without Firebase!)
        // Tier 3: Free keyless Pollinations fallback
        val keyframes = mutableListOf<Bitmap>()
        if (anchorBitmap != null) {
            keyframes.add(anchorBitmap.copy(Bitmap.Config.ARGB_8888, false))
        }

        val toGenerate = if (anchorBitmap != null) {
            plan.beats.take((totalBeats - 1).coerceAtLeast(1))
        } else {
            plan.beats.take(totalBeats)
        }

        val baseSeed = Random.nextInt(10_000, 999_999)
        var firstError: Exception? = null

        toGenerate.forEachIndexed { index, beatPrompt ->
            val basePct = 0.14f + (0.58f * (index.toFloat() / toGenerate.size.coerceAtLeast(1)))
            onProgress(
                VideoRenderProgress(
                    stageTitle = "Synthesizing Frame ${index + 1}/${toGenerate.size}",
                    detail = "${model.displayName} · ${ratio.label} (${if (keyframes.isNotEmpty()) "Continuing previous frame" else "Opening shot"})…",
                    progress = basePct
                )
            )

            val styledPrompt = "$beatPrompt, ${model.styleDirective}, cinematic film still, sharp focus, no watermark"
            val previousFrame = keyframes.lastOrNull()

            try {
                val bmp = synthesizeKeyframe(
                    prompt = styledPrompt,
                    previousFrame = previousFrame,
                    model = model,
                    ratio = ratio,
                    seed = baseSeed + index,
                    firebaseAvailable = firebaseAvailable,
                    onStatus = { statusText ->
                        onProgress(
                            VideoRenderProgress(
                                stageTitle = "Synthesizing Frame ${index + 1}/${toGenerate.size}",
                                detail = statusText,
                                progress = (basePct + 0.08f).coerceAtMost(0.72f)
                            )
                        )
                    }
                )
                keyframes.add(bmp)
                if (index < toGenerate.lastIndex) delay(350)
            } catch (e: CancellationException) {
                keyframes.forEach { if (!it.isRecycled) it.recycle() }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Frame ${index + 1} synthesis failed", e)
                if (firstError == null) firstError = e
                if (keyframes.isEmpty()) {
                    throw RuntimeException(
                        e.localizedMessage ?: "AI video frame generation failed. Please check your internet connection and retry.",
                        e
                    )
                }
            }
        }

        if (keyframes.isEmpty()) {
            throw RuntimeException(
                firstError?.localizedMessage ?: "No video frames were generated. Please retry.",
                firstError
            )
        }

        // Step 3: Encode smooth 24fps H.264 MP4 + capture Last Frame PNG
        val clipId = UUID.randomUUID().toString()
        val videoDir = File(context.filesDir, "studio_videos").apply { mkdirs() }
        val segFile = File(videoDir, "clip_${clipId}_seg.mp4")
        val finalFile = File(videoDir, "clip_${clipId}.mp4")
        val thumbFile = File(videoDir, "thumb_${clipId}.png")
        val lastFrameFile = File(videoDir, "last_${clipId}.png")

        onProgress(
            VideoRenderProgress(
                stageTitle = "Encoding H.264 MP4",
                detail = "Rendering ${safeSec}s (${ratio.width}×${ratio.height} · 24fps)…",
                progress = 0.74f
            )
        )

        val rendered = try {
            Mp4VideoEngine.renderKeyframesToMp4(
                keyframes = keyframes,
                width = ratio.width,
                height = ratio.height,
                durationSec = safeSec,
                motion = effectiveMotion,
                outputVideoFile = if (mode == VideoStudioMode.EXTEND_VIDEO && parentClip != null) segFile else finalFile,
                thumbFile = thumbFile,
                lastFrameFile = lastFrameFile
            ) { pct ->
                onProgress(
                    VideoRenderProgress(
                        stageTitle = "Encoding H.264 MP4",
                        detail = "Rendering ${safeSec}s (${(pct * 100).toInt()}%)…",
                        progress = 0.74f + (pct * 0.20f)
                    )
                )
            }
        } finally {
            keyframes.forEach { if (!it.isRecycled) it.recycle() }
        }

        // Step 4: Seamless MP4 stitching when extending from a History clip
        var outputFile = rendered.videoFile
        var totalDuration = safeSec
        var extendStep = if (mode == VideoStudioMode.EXTEND_VIDEO) 1 else 0

        if (mode == VideoStudioMode.EXTEND_VIDEO && parentClip != null) {
            onProgress(
                VideoRenderProgress(
                    stageTitle = "Stitching Extended Video",
                    detail = "Combining ${parentClip.totalDurationSec}s + ${safeSec}s seamlessly…",
                    progress = 0.96f
                )
            )
            val ok = Mp4VideoEngine.stitchMp4Clips(
                parentPath = parentClip.videoPath,
                segmentPath = segFile.absolutePath,
                outputPath = finalFile.absolutePath
            )
            if (ok && finalFile.exists() && finalFile.length() > 1024L) {
                segFile.delete()
                outputFile = finalFile
                totalDuration = parentClip.totalDurationSec + safeSec
                extendStep = parentClip.extendStep + 1
            } else {
                segFile.renameTo(finalFile)
                outputFile = finalFile
                totalDuration = safeSec
                extendStep = parentClip.extendStep + 1
            }
        }

        Mp4VideoEngine.extractLastFrame(context, filePath = outputFile.absolutePath)?.let { exactLast ->
            Mp4VideoEngine.savePng(exactLast, lastFrameFile)
            exactLast.recycle()
        }

        onProgress(
            VideoRenderProgress(
                stageTitle = "Video Complete",
                detail = "${totalDuration}s MP4 (${ratio.label}) ready to play & extend",
                progress = 1.0f
            )
        )

        return VideoClipRecord(
            id = clipId,
            prompt = prompt,
            mode = mode,
            model = model,
            ratio = ratio,
            segmentDurationSec = safeSec,
            totalDurationSec = totalDuration,
            videoPath = outputFile.absolutePath,
            thumbPath = thumbFile.absolutePath,
            lastFramePath = lastFrameFile.absolutePath,
            parentClipId = parentClip?.id,
            extendStep = extendStep,
            createdAt = System.currentTimeMillis()
        )
    }

    private suspend fun synthesizeKeyframe(
        prompt: String,
        previousFrame: Bitmap?,
        model: FreeVideoModel,
        ratio: VideoRatio,
        seed: Int,
        firebaseAvailable: Boolean,
        onStatus: suspend (String) -> Unit
    ): Bitmap {
        val errors = mutableListOf<String>()

        // Tier 1: Built-in Firebase AI Logic image synthesis (only if FirebaseApp is initialized)
        if (firebaseAvailable) {
            val candidates = listOf(model.primaryImageModel, model.fallbackImageModel).distinct()
            for (modelName in candidates) {
                try {
                    onStatus("Built-in Gemini ($modelName) generating frame…")
                    val bmp = generateViaFirebaseAi(
                        modelName = modelName,
                        prompt = prompt,
                        previousFrame = previousFrame,
                        ratio = ratio
                    )
                    if (bmp != null) return bmp
                    errors.add("$modelName returned empty image")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: TimeoutCancellationException) {
                    Log.w(TAG, "$modelName timed out", e)
                    errors.add("$modelName timed out")
                } catch (e: Exception) {
                    Log.w(TAG, "$modelName failed: ${e.message}", e)
                    errors.add("$modelName failed")
                }
            }
        }

        // Tier 2: Free Open-Source AI Horde Cluster (100% keyless, works on standalone APKs everywhere!)
        try {
            return generateViaAiHorde(
                prompt = prompt,
                previousFrame = previousFrame,
                ratio = ratio,
                seed = seed,
                onStatus = onStatus
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "AI Horde failed: ${e.message}", e)
            errors.add("Free AI Cluster: ${e.localizedMessage ?: "busy"}")
        }

        // Tier 3: Keyless Pollinations fallback
        try {
            onStatus("Trying backup open image renderer…")
            return fetchPollinationsFallback(
                prompt = prompt,
                width = ratio.width,
                height = ratio.height,
                seed = seed
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Pollinations fallback failed: ${e.message}", e)
            errors.add("Backup renderer: ${e.localizedMessage ?: "unavailable"}")
        }

        throw RuntimeException(
            "Video frame synthesis failed (${errors.joinToString(" · ")}). Please check your internet connection and retry."
        )
    }

    private suspend fun generateViaFirebaseAi(
        modelName: String,
        prompt: String,
        previousFrame: Bitmap?,
        ratio: VideoRatio
    ): Bitmap? {
        val imageModel = buildImageModel(modelName, ratio)
        val response = withTimeout(60_000L) {
            if (previousFrame != null) {
                val ref = downscaleForPayload(previousFrame, 768)
                try {
                    imageModel.generateContent(
                        content {
                            image(ref)
                            text(
                                "Generate the next continuous cinematic video frame immediately following this frame. " +
                                    "Preserve the exact same subject, identity, environment, and lighting while advancing motion smoothly: $prompt"
                            )
                        }
                    )
                } finally {
                    if (ref !== previousFrame && !ref.isRecycled) ref.recycle()
                }
            } else {
                imageModel.generateContent(prompt)
            }
        }
        return response.candidates.firstOrNull()
            ?.content?.parts?.filterIsInstance<ImagePart>()
            ?.firstOrNull()?.image
    }

    /**
     * 100% Free, keyless distributed open-source AI synthesis via AI Horde (aihorde.net).
     * Uses anonymous free-tier compliant dimensions (576x320 for 16:9, 320x576 for 9:16; <= 512x512 pixels, 20 steps).
     */
    private suspend fun generateViaAiHorde(
        prompt: String,
        previousFrame: Bitmap?,
        ratio: VideoRatio,
        seed: Int,
        onStatus: suspend (String) -> Unit
    ): Bitmap {
        val hordeW = if (ratio == VideoRatio.LANDSCAPE_16_9) 576 else 320
        val hordeH = if (ratio == VideoRatio.LANDSCAPE_16_9) 320 else 576

        onStatus("Free AI Cluster · Submitting job (${ratio.label})…")

        // Try img2img when continuing from a previous frame; cleanly fall back to txt2img if rejected
        val jobId = submitAiHordeJob(
            prompt = prompt,
            width = hordeW,
            height = hordeH,
            seed = seed,
            previousFrame = previousFrame
        )

        // Poll lightweight check endpoint up to ~110s with cooperative cancellation
        val maxPolls = 44
        for (attempt in 1..maxPolls) {
            coroutineContext.ensureActive()
            delay(2_500L)

            val checkJson = withContext(Dispatchers.IO) {
                val req = Request.Builder()
                    .url("$AI_HORDE_API_BASE/generate/check/$jobId")
                    .header("Client-Agent", AI_HORDE_CLIENT_AGENT)
                    .get()
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw RuntimeException("Status HTTP ${resp.code}")
                    JSONObject(resp.body?.string().orEmpty())
                }
            }

            if (checkJson.optBoolean("faulted", false)) {
                throw RuntimeException("Worker faulted on this frame")
            }
            if (!checkJson.optBoolean("is_possible", true)) {
                throw RuntimeException("No free worker available for these settings")
            }
            if (checkJson.optBoolean("done", false)) {
                break
            }

            val waitSec = checkJson.optInt("wait_time", 0)
            val queuePos = checkJson.optInt("queue_position", 0)
            val msg = when {
                checkJson.optInt("processing", 0) > 0 -> "Free AI Cluster · Rendering frame…"
                waitSec > 0 -> "Free AI Cluster · Queue #$queuePos (~${waitSec}s)…"
                else -> "Free AI Cluster · Waiting for worker…"
            }
            onStatus(msg)

            if (attempt == maxPolls) {
                throw RuntimeException("Free AI Cluster queue timed out")
            }
        }

        // Fetch completed generation payload & decode image
        onStatus("Free AI Cluster · Downloading generated frame…")
        return withContext(Dispatchers.IO) {
            val statusReq = Request.Builder()
                .url("$AI_HORDE_API_BASE/generate/status/$jobId")
                .header("Client-Agent", AI_HORDE_CLIENT_AGENT)
                .get()
                .build()
            val imgField = httpClient.newCall(statusReq).execute().use { resp ->
                if (!resp.isSuccessful) throw RuntimeException("Result HTTP ${resp.code}")
                val json = JSONObject(resp.body?.string().orEmpty())
                val gens = json.optJSONArray("generations")
                if (gens == null || gens.length() == 0) {
                    throw RuntimeException("No frame returned by worker")
                }
                gens.getJSONObject(0).optString("img", "")
            }

            if (imgField.isBlank()) throw RuntimeException("Empty frame image data")

            val bytes = if (imgField.startsWith("http://") || imgField.startsWith("https://")) {
                val imgReq = Request.Builder()
                    .url(imgField)
                    .header("User-Agent", "GodModeStudio/1.0 Android")
                    .get()
                    .build()
                httpClient.newCall(imgReq).execute().use { r ->
                    if (!r.isSuccessful) throw RuntimeException("Frame download HTTP ${r.code}")
                    r.body?.bytes() ?: throw RuntimeException("Empty downloaded frame")
                }
            } else {
                Base64.decode(imgField, Base64.DEFAULT)
            }

            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: throw RuntimeException("Could not decode generated frame")
        }
    }

    private suspend fun submitAiHordeJob(
        prompt: String,
        width: Int,
        height: Int,
        seed: Int,
        previousFrame: Bitmap?
    ): String = withContext(Dispatchers.IO) {
        fun buildBody(includeSource: Boolean): String {
            val params = JSONObject().apply {
                put("width", width)
                put("height", height)
                put("steps", 20)
                put("cfg_scale", 7.0)
                put("sampler_name", "k_euler_a")
                put("seed", seed.toString())
                put("n", 1)
                if (includeSource && previousFrame != null) {
                    put("denoising_strength", 0.55)
                }
            }
            return JSONObject().apply {
                put("prompt", prompt.take(450))
                put("params", params)
                put("nsfw", false)
                put("censor_nsfw", true)
                put("trusted_workers", false)
                put("slow_workers", true)
                put("r2", true)
                if (includeSource && previousFrame != null) {
                    val scaled = Bitmap.createScaledBitmap(previousFrame, width, height, true)
                    val baos = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 82, baos)
                    if (scaled !== previousFrame && !scaled.isRecycled) scaled.recycle()
                    put("source_image", Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP))
                    put("source_processing", "img2img")
                }
            }.toString()
        }

        fun post(bodyJson: String): Pair<Int, String> {
            val req = Request.Builder()
                .url("$AI_HORDE_API_BASE/generate/async")
                .header("apikey", AI_HORDE_ANON_KEY)
                .header("Client-Agent", AI_HORDE_CLIENT_AGENT)
                .header("Content-Type", "application/json")
                .post(bodyJson.toRequestBody(jsonMediaType))
                .build()
            httpClient.newCall(req).execute().use { resp ->
                return resp.code to resp.body?.string().orEmpty()
            }
        }

        // If img2img is rejected by anonymous limits, cleanly fall back to txt2img
        var (code, text) = post(buildBody(includeSource = previousFrame != null))
        if (code !in 200..299 && previousFrame != null) {
            Log.w(TAG, "AI Horde img2img returned HTTP $code, retrying as txt2img")
            val retry = post(buildBody(includeSource = false))
            code = retry.first
            text = retry.second
        }

        if (code !in 200..299) {
            val detail = runCatching { JSONObject(text).optString("message") }.getOrNull()
            throw RuntimeException("HTTP $code ${detail.orEmpty()}".trim())
        }

        val id = JSONObject(text).optString("id", "")
        if (id.isBlank()) throw RuntimeException("Missing job id from AI Horde")
        id
    }

    private suspend fun fetchPollinationsFallback(
        prompt: String,
        width: Int,
        height: Int,
        seed: Int
    ): Bitmap = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(prompt.take(260), "UTF-8").replace("+", "%20")
        val url = "https://image.pollinations.ai/prompt/$encoded?width=$width&height=$height&model=flux&seed=$seed&nologo=true"
        withTimeout(45_000L) {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "GodModeStudio/1.0 Android")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw RuntimeException("HTTP ${resp.code}")
                }
                val bytes = resp.body?.bytes()
                    ?: throw RuntimeException("Empty frame payload")
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: throw RuntimeException("Invalid image format")
            }
        }
    }

    private suspend fun planShots(
        prompt: String,
        mode: VideoStudioMode,
        model: FreeVideoModel,
        ratio: VideoRatio,
        beatCount: Int,
        anchorBitmap: Bitmap?,
        firebaseAvailable: Boolean
    ): PlannedShotList {
        if (!firebaseAvailable) {
            return fallbackPlan(prompt, beatCount, mode)
        }

        val instruction = when (mode) {
            VideoStudioMode.TEXT_TO_VIDEO ->
                "Design $beatCount sequential cinematic keyframes for this video prompt: \"$prompt\"."
            VideoStudioMode.IMAGE_TO_VIDEO ->
                "Inspect the attached starting image carefully. Design $beatCount sequential continuation frames animating this exact subject, palette, and environment according to: \"$prompt\"."
            VideoStudioMode.EXTEND_VIDEO ->
                "Inspect the attached LAST FRAME from the previous video clip carefully. Design $beatCount seamless continuation shots extending the scene forward for: \"$prompt\". Maintain exact subject, lighting, and location."
        }

        return try {
            val director = buildDirectorModel()
            val response = withTimeout(30_000L) {
                val ref = anchorBitmap?.let { downscaleForPayload(it, 768) }
                try {
                    director.generateContent(
                        content {
                            if (ref != null) image(ref)
                            text(
                                """
                                $instruction
                                Visual style target: ${model.displayName} (${model.styleDirective}).
                                Aspect ratio: ${ratio.label} (${ratio.subtitle}).
                                Return strictly JSON matching the schema with vivid English visual prompts for each beat.
                                """.trimIndent()
                            )
                        }
                    )
                } finally {
                    if (ref != null && ref !== anchorBitmap && !ref.isRecycled) ref.recycle()
                }
            }
            val raw = response.text ?: error("Empty director response")
            val json = JSONObject(raw)
            val motion = runCatching {
                VideoCameraMotion.valueOf(json.optString("cameraMotion", "ZOOM_IN"))
            }.getOrDefault(VideoCameraMotion.ZOOM_IN)

            val arr = json.optJSONArray("beats")
            val list = mutableListOf<String>()
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val s = arr.optString(i).trim()
                    if (s.isNotBlank()) list.add(s)
                }
            }
            if (list.isEmpty()) fallbackPlan(prompt, beatCount, mode) else PlannedShotList(list, motion)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Director JSON fallback used", e)
            fallbackPlan(prompt, beatCount, mode)
        }
    }

    private fun fallbackPlan(prompt: String, count: Int, mode: VideoStudioMode): PlannedShotList {
        val shots = when (mode) {
            VideoStudioMode.EXTEND_VIDEO -> listOf(
                "Seamless next moment continuing $prompt, same subject and lighting, dynamic forward motion",
                "Further progression of $prompt, rich cinematic depth and atmospheric movement",
                "Grand cinematic continuation climax of $prompt, wide balanced framing"
            )
            VideoStudioMode.IMAGE_TO_VIDEO -> listOf(
                "Smooth motion progression animating $prompt, preserving exact scene lighting and subject",
                "Cinematic camera movement through $prompt, atmospheric depth and vivid detail",
                "Closing expressive shot of $prompt, harmonious lighting"
            )
            VideoStudioMode.TEXT_TO_VIDEO -> listOf(
                "Opening establishing cinematic shot of $prompt, crisp lighting, fluid composition",
                "Mid-scene dynamic motion progression of $prompt, consistent subject and color palette",
                "Dramatic camera advance continuing $prompt, rich atmospheric depth and detail",
                "Closing cinematic climax frame of $prompt, balanced wide composition"
            )
        }
        return PlannedShotList(shots.take(count), VideoCameraMotion.ZOOM_IN)
    }

    private fun downscaleForPayload(bmp: Bitmap, maxDim: Int): Bitmap {
        if (bmp.width <= maxDim && bmp.height <= maxDim) return bmp
        val scale = maxDim.toFloat() / maxOf(bmp.width, bmp.height).toFloat()
        return Bitmap.createScaledBitmap(
            bmp,
            (bmp.width * scale).toInt().coerceAtLeast(1),
            (bmp.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }
}
