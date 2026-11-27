package com.example.domain.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.domain.model.FreeVideoModel
import com.example.domain.model.VideoCameraMotion
import com.example.domain.model.VideoClipRecord
import com.example.domain.model.VideoRatio
import com.example.domain.model.VideoRenderProgress
import com.example.domain.model.VideoStudioMode
import com.google.firebase.Firebase
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.random.Random

private const val TAG = "FreeVideoGenerator"

private data class PlannedShotList(
    val beats: List<String>,
    val cameraMotion: VideoCameraMotion
)

class FreeVideoGenerator(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

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

    private val directorModel by lazy {
        Firebase.ai.generativeModel(
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
        val totalBeats = when {
            safeSec <= 7 -> 2
            safeSec <= 11 -> 3
            else -> 4
        }

        // Step 1: AI Cinematography Director via built-in Firebase AI Logic (gemini-3.8-flash)
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
            anchorBitmap = anchorBitmap
        )

        val effectiveMotion = if (cameraMotion == VideoCameraMotion.AUTO) {
            plan.cameraMotion
        } else {
            cameraMotion
        }

        // Step 2: Synthesize continuous AI keyframes using Built-in Firebase AI (Nano Banana 2.1 / Flash-Lite Image)
        // with image-to-image chaining from previous frame for visual continuity
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
            val pct = 0.14f + (0.58f * (index.toFloat() / toGenerate.size.coerceAtLeast(1)))
            onProgress(
                VideoRenderProgress(
                    stageTitle = "Synthesizing Frame ${index + 1}/${toGenerate.size}",
                    detail = "${model.displayName} · ${ratio.label} (${if (keyframes.isNotEmpty()) "Continuing previous frame" else "Opening shot"})…",
                    progress = pct
                )
            )

            val styledPrompt = "$beatPrompt. Style: ${model.styleDirective}. No text overlays or watermarks."
            val previousFrame = keyframes.lastOrNull()

            try {
                val bmp = synthesizeKeyframe(
                    prompt = styledPrompt,
                    previousFrame = previousFrame,
                    model = model,
                    ratio = ratio,
                    seed = baseSeed + index
                )
                keyframes.add(bmp)
                if (index < toGenerate.lastIndex) delay(400)
            } catch (e: CancellationException) {
                keyframes.forEach { if (!it.isRecycled) it.recycle() }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Frame ${index + 1} synthesis failed", e)
                if (firstError == null) firstError = e
                // If we already have at least one real frame (e.g., anchor image or Frame 1), continue encoding
                // what we successfully generated rather than failing the whole render.
                if (keyframes.isEmpty()) {
                    throw RuntimeException(
                        e.localizedMessage ?: "AI video frame generation failed. Please check your connection and try again.",
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
        seed: Int
    ): Bitmap {
        val errors = mutableListOf<String>()

        // 1. Primary built-in Firebase AI Logic image model (Nano Banana 2.1 or Flash-Lite Image)
        val candidates = listOf(model.primaryImageModel, model.fallbackImageModel).distinct()
        for (modelName in candidates) {
            try {
                val bmp = generateViaFirebaseAi(
                    modelName = modelName,
                    prompt = prompt,
                    previousFrame = previousFrame,
                    ratio = ratio
                )
                if (bmp != null) return bmp
                errors.add("$modelName returned no image")
            } catch (e: CancellationException) {
                throw e
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "$modelName timed out", e)
                errors.add("$modelName timed out")
            } catch (e: Exception) {
                Log.w(TAG, "$modelName failed: ${e.message}", e)
                errors.add("$modelName: ${e.localizedMessage ?: e.javaClass.simpleName}")
            }
        }

        // 2. Secondary open free fallback if Firebase image models are unavailable on the project's billing tier
        try {
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
            errors.add("Open render fallback: ${e.localizedMessage ?: "unavailable"}")
        }

        throw RuntimeException(
            "Video frame synthesis failed (${errors.firstOrNull() ?: "service unavailable"}). Please retry."
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
        anchorBitmap: Bitmap?
    ): PlannedShotList {
        val instruction = when (mode) {
            VideoStudioMode.TEXT_TO_VIDEO ->
                "Design $beatCount sequential cinematic keyframes for this video prompt: \"$prompt\"."
            VideoStudioMode.IMAGE_TO_VIDEO ->
                "Inspect the attached starting image carefully. Design $beatCount sequential continuation frames animating this exact subject, palette, and environment according to: \"$prompt\"."
            VideoStudioMode.EXTEND_VIDEO ->
                "Inspect the attached LAST FRAME from the previous video clip carefully. Design $beatCount seamless continuation shots extending the scene forward for: \"$prompt\". Maintain exact subject, lighting, and location."
        }

        return try {
            val response = withTimeout(30_000L) {
                val ref = anchorBitmap?.let { downscaleForPayload(it, 768) }
                try {
                    directorModel.generateContent(
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
            if (list.isEmpty()) fallbackPlan(prompt, beatCount) else PlannedShotList(list, motion)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Director JSON fallback used", e)
            fallbackPlan(prompt, beatCount)
        }
    }

    private fun fallbackPlan(prompt: String, count: Int): PlannedShotList {
        val shots = listOf(
            "Opening establishing cinematic shot of $prompt, crisp natural lighting, fluid motion",
            "Mid-scene dynamic motion progression of $prompt, identical subject and color palette",
            "Dramatic camera advance continuing $prompt, rich atmospheric depth and detail",
            "Closing cinematic climax frame of $prompt, balanced composition"
        )
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
