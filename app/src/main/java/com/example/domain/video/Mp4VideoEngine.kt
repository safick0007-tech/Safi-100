package com.example.domain.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import com.example.domain.model.VideoCameraMotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val TAG = "Mp4VideoEngine"
private const val FPS = 24
private const val FRAME_US = 1_000_000L / FPS

data class RenderedMp4Output(
    val videoFile: File,
    val thumbFile: File,
    val lastFrameFile: File
)

object Mp4VideoEngine {

    suspend fun renderKeyframesToMp4(
        keyframes: List<Bitmap>,
        width: Int,
        height: Int,
        durationSec: Int,
        motion: VideoCameraMotion,
        outputVideoFile: File,
        thumbFile: File,
        lastFrameFile: File,
        onProgress: suspend (Float) -> Unit
    ): RenderedMp4Output = withContext(Dispatchers.Default) {
        require(keyframes.isNotEmpty()) { "At least one keyframe is required." }

        val prepared = keyframes.map { centerCropScale(it, width, height) }
        val totalFrames = durationSec.coerceIn(5, 15) * FPS

        try {
            savePng(prepared.first(), thumbFile)

            // Capture exact last frame PNG for instant Extend Video continuity
            val finalBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            drawFrame(Canvas(finalBmp), prepared, 1f, motion, width, height)
            savePng(finalBmp, lastFrameFile)
            finalBmp.recycle()

            // Primary path: Universal YUV420Flexible MediaCodec InputImage encoder (works on 100% of Android emulators & devices)
            val yuvResult = runCatching {
                encodeViaYuv420(
                    prepared = prepared,
                    width = width,
                    height = height,
                    totalFrames = totalFrames,
                    motion = motion,
                    outputVideoFile = outputVideoFile,
                    onProgress = onProgress
                )
            }

            if (yuvResult.isFailure || !outputVideoFile.exists() || outputVideoFile.length() < 1024L) {
                Log.w(TAG, "YUV420 encoder failed or produced empty file, falling back to Surface encoder", yuvResult.exceptionOrNull())
                encodeViaSurface(
                    prepared = prepared,
                    width = width,
                    height = height,
                    totalFrames = totalFrames,
                    motion = motion,
                    outputVideoFile = outputVideoFile,
                    onProgress = onProgress
                )
            }

            if (!outputVideoFile.exists() || outputVideoFile.length() < 1024L) {
                throw IllegalStateException("MP4 encoder produced an empty output file.")
            }

            RenderedMp4Output(outputVideoFile, thumbFile, lastFrameFile)
        } finally {
            prepared.forEach { if (!it.isRecycled) it.recycle() }
        }
    }

    private suspend fun encodeViaYuv420(
        prepared: List<Bitmap>,
        width: Int,
        height: Int,
        totalFrames: Int,
        motion: VideoCameraMotion,
        outputVideoFile: File,
        onProgress: suspend (Float) -> Unit
    ) {
        val codecName = selectAvcEncoderName(requireYuvFlexible = true)
        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var trackIdx = -1

        val frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val frameCanvas = Canvas(frameBitmap)
        val argbPixels = IntArray(width * height)
        val yuvByteSize = width * height * 3 / 2

        try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                setInteger(MediaFormat.KEY_BIT_RATE, 2_400_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, FPS)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            codec = if (codecName != null) {
                MediaCodec.createByCodecName(codecName)
            } else {
                MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            }
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            outputVideoFile.parentFile?.mkdirs()
            if (outputVideoFile.exists()) outputVideoFile.delete()
            muxer = MediaMuxer(outputVideoFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val bufferInfo = MediaCodec.BufferInfo()
            var outPtsUs = 0L

            fun drainOutputs(waitForEos: Boolean) {
                var idleTries = 0
                val maxIdleTries = if (waitForEos) 250 else 2
                while (true) {
                    val outIdx = codec.dequeueOutputBuffer(bufferInfo, if (waitForEos) 10_000L else 0L)
                    when {
                        outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            if (!waitForEos) break
                            idleTries++
                            if (idleTries >= maxIdleTries) {
                                Log.w(TAG, "EOS drain reached safety cap after $maxIdleTries polls")
                                break
                            }
                        }
                        outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            if (!muxerStarted) {
                                trackIdx = muxer.addTrack(codec.outputFormat)
                                muxer.start()
                                muxerStarted = true
                            }
                        }
                        outIdx >= 0 -> {
                            idleTries = 0
                            val encoded = codec.getOutputBuffer(outIdx)
                            if (encoded != null && bufferInfo.size > 0 && muxerStarted) {
                                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                    encoded.position(bufferInfo.offset)
                                    encoded.limit(bufferInfo.offset + bufferInfo.size)
                                    val writeInfo = MediaCodec.BufferInfo().apply {
                                        offset = bufferInfo.offset
                                        size = bufferInfo.size
                                        flags = bufferInfo.flags
                                        presentationTimeUs = if (bufferInfo.presentationTimeUs >= outPtsUs) {
                                            bufferInfo.presentationTimeUs
                                        } else {
                                            outPtsUs
                                        }
                                    }
                                    muxer.writeSampleData(trackIdx, encoded, writeInfo)
                                    outPtsUs = writeInfo.presentationTimeUs + FRAME_US
                                }
                            }
                            codec.releaseOutputBuffer(outIdx, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
                        }
                    }
                }
            }

            for (i in 0 until totalFrames) {
                coroutineContext.ensureActive()
                val progress = if (totalFrames <= 1) 1f else i.toFloat() / (totalFrames - 1).toFloat()
                drawFrame(frameCanvas, prepared, progress, motion, width, height)
                frameBitmap.getPixels(argbPixels, 0, width, 0, 0, width, height)

                var inputIdx = -1
                var waitCycles = 0
                while (inputIdx < 0 && waitCycles < 200) {
                    inputIdx = codec.dequeueInputBuffer(10_000L)
                    if (inputIdx < 0) {
                        drainOutputs(waitForEos = false)
                        waitCycles++
                    }
                }
                if (inputIdx < 0) throw IllegalStateException("MediaCodec input buffer timeout at frame $i")

                val inputImage = codec.getInputImage(inputIdx)
                    ?: throw IllegalStateException("MediaCodec returned null InputImage")
                writeArgbToYuvImage(argbPixels, width, height, inputImage)

                codec.queueInputBuffer(inputIdx, 0, yuvByteSize, i * FRAME_US, 0)
                drainOutputs(waitForEos = false)

                if (i % 6 == 0 || i == totalFrames - 1) onProgress(progress)
            }

            // Signal End-Of-Stream
            var eosIdx = -1
            var eosWait = 0
            while (eosIdx < 0 && eosWait < 100) {
                eosIdx = codec.dequeueInputBuffer(10_000L)
                if (eosIdx < 0) {
                    drainOutputs(waitForEos = false)
                    eosWait++
                }
            }
            if (eosIdx >= 0) {
                codec.queueInputBuffer(eosIdx, 0, 0, totalFrames * FRAME_US, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }
            drainOutputs(waitForEos = true)
        } finally {
            if (!frameBitmap.isRecycled) frameBitmap.recycle()
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            if (muxerStarted) runCatching { muxer?.stop() }
            runCatching { muxer?.release() }
        }
    }

    private suspend fun encodeViaSurface(
        prepared: List<Bitmap>,
        width: Int,
        height: Int,
        totalFrames: Int,
        motion: VideoCameraMotion,
        outputVideoFile: File,
        onProgress: suspend (Float) -> Unit
    ) {
        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var trackIdx = -1

        try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, 2_400_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, FPS)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val surface = codec.createInputSurface()
            codec.start()

            outputVideoFile.parentFile?.mkdirs()
            if (outputVideoFile.exists()) outputVideoFile.delete()
            muxer = MediaMuxer(outputVideoFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val info = MediaCodec.BufferInfo()
            var ptsUs = 0L

            fun drain(eos: Boolean) {
                if (eos) codec.signalEndOfInputStream()
                var idleTries = 0
                val maxIdleTries = if (eos) 200 else 2
                while (true) {
                    val idx = codec.dequeueOutputBuffer(info, if (eos) 10_000L else 0L)
                    when {
                        idx == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            if (!eos) break
                            idleTries++
                            if (idleTries >= maxIdleTries) break
                        }
                        idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            if (!muxerStarted) {
                                trackIdx = muxer.addTrack(codec.outputFormat)
                                muxer.start()
                                muxerStarted = true
                            }
                        }
                        idx >= 0 -> {
                            idleTries = 0
                            val buf = codec.getOutputBuffer(idx)
                            if (buf != null && info.size > 0 && muxerStarted) {
                                if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                    buf.position(info.offset)
                                    buf.limit(info.offset + info.size)
                                    val writeInfo = MediaCodec.BufferInfo().apply {
                                        offset = info.offset
                                        size = info.size
                                        flags = info.flags
                                        presentationTimeUs = ptsUs
                                    }
                                    muxer.writeSampleData(trackIdx, buf, writeInfo)
                                    ptsUs += FRAME_US
                                }
                            }
                            codec.releaseOutputBuffer(idx, false)
                            if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
                        }
                    }
                }
            }

            for (i in 0 until totalFrames) {
                coroutineContext.ensureActive()
                val progress = if (totalFrames <= 1) 1f else i.toFloat() / (totalFrames - 1).toFloat()
                val canvas = surface.lockCanvas(null)
                try {
                    drawFrame(canvas, prepared, progress, motion, width, height)
                } finally {
                    surface.unlockCanvasAndPost(canvas)
                }
                drain(eos = false)
                if (i % 6 == 0 || i == totalFrames - 1) onProgress(progress)
            }

            drain(eos = true)
            surface.release()
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            if (muxerStarted) runCatching { muxer?.stop() }
            runCatching { muxer?.release() }
        }
    }

    private fun selectAvcEncoderName(requireYuvFlexible: Boolean): String? {
        val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        var fallbackCandidate: String? = null
        for (info in list.codecInfos) {
            if (!info.isEncoder) continue
            if (!info.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true) }) continue
            val caps = runCatching { info.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC) }.getOrNull() ?: continue
            val supportsYuv = caps.colorFormats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            if (requireYuvFlexible && !supportsYuv) continue
            // Prefer standard reliable codecs; remember first match
            if (fallbackCandidate == null) fallbackCandidate = info.name
            if (info.name.contains("c2.android.avc", ignoreCase = true) ||
                info.name.contains("OMX.google.h264", ignoreCase = true)
            ) {
                return info.name
            }
        }
        return fallbackCandidate
    }

    private fun writeArgbToYuvImage(argb: IntArray, width: Int, height: Int, image: Image) {
        val planes = image.planes
        val yBuf = planes[0].buffer
        val uBuf = planes[1].buffer
        val vBuf = planes[2].buffer

        val yRowStride = planes[0].rowStride
        val yPixStride = planes[0].pixelStride
        val uRowStride = planes[1].rowStride
        val uPixStride = planes[1].pixelStride
        val vRowStride = planes[2].rowStride
        val vPixStride = planes[2].pixelStride

        var idx = 0
        for (y in 0 until height) {
            val yRowOffset = y * yRowStride
            val uvRow = y shr 1
            val uRowOffset = uvRow * uRowStride
            val vRowOffset = uvRow * vRowStride
            val evenRow = (y and 1) == 0

            for (x in 0 until width) {
                val pixel = argb[idx++]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                val yVal = (((66 * r + 129 * g + 25 * b + 128) shr 8) + 16).coerceIn(16, 235)
                yBuf.put(yRowOffset + x * yPixStride, yVal.toByte())

                if (evenRow && (x and 1) == 0) {
                    val uvCol = x shr 1
                    val uVal = (((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128).coerceIn(16, 240)
                    val vVal = (((112 * r - 94 * g - 18 * b + 128) shr 8) + 128).coerceIn(16, 240)
                    uBuf.put(uRowOffset + uvCol * uPixStride, uVal.toByte())
                    vBuf.put(vRowOffset + uvCol * vPixStride, vVal.toByte())
                }
            }
        }
    }

    suspend fun stitchMp4Clips(
        parentPath: String,
        segmentPath: String,
        outputPath: String
    ): Boolean = withContext(Dispatchers.IO) {
        val parentFile = File(parentPath)
        val segFile = File(segmentPath)
        if (!parentFile.exists() || !segFile.exists()) return@withContext false

        var muxer: MediaMuxer? = null
        var started = false
        try {
            val e1 = MediaExtractor().apply { setDataSource(parentFile.absolutePath) }
            val e2 = MediaExtractor().apply { setDataSource(segFile.absolutePath) }
            val t1 = findVideoTrack(e1)
            val t2 = findVideoTrack(e2)
            if (t1 < 0 || t2 < 0) {
                e1.release(); e2.release()
                return@withContext false
            }
            e1.selectTrack(t1); e2.selectTrack(t2)
            val f1 = e1.getTrackFormat(t1)
            val f2 = e2.getTrackFormat(t2)
            if (f1.getInteger(MediaFormat.KEY_WIDTH) != f2.getInteger(MediaFormat.KEY_WIDTH) ||
                f1.getInteger(MediaFormat.KEY_HEIGHT) != f2.getInteger(MediaFormat.KEY_HEIGHT)
            ) {
                e1.release(); e2.release()
                return@withContext false
            }

            val out = File(outputPath).apply { parentFile?.mkdirs(); if (exists()) delete() }
            muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val outTrack = muxer.addTrack(f1)
            muxer.start()
            started = true

            val buffer = ByteBuffer.allocate(1024 * 1024)
            val info = MediaCodec.BufferInfo()
            var lastPts = 0L

            while (true) {
                val sz = e1.readSampleData(buffer, 0)
                if (sz < 0) break
                info.set(0, sz, e1.sampleTime.coerceAtLeast(0L), mapFlags(e1.sampleFlags))
                muxer.writeSampleData(outTrack, buffer, info)
                lastPts = info.presentationTimeUs
                e1.advance()
            }

            val offsetUs = lastPts + FRAME_US
            while (true) {
                val sz = e2.readSampleData(buffer, 0)
                if (sz < 0) break
                info.set(0, sz, offsetUs + e2.sampleTime.coerceAtLeast(0L), mapFlags(e2.sampleFlags))
                muxer.writeSampleData(outTrack, buffer, info)
                e2.advance()
            }

            e1.release(); e2.release()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Video stitching skipped; keeping standalone continuation segment", e)
            false
        } finally {
            if (started) runCatching { muxer?.stop() }
            runCatching { muxer?.release() }
        }
    }

    suspend fun extractLastFrame(
        context: Context,
        filePath: String? = null,
        uri: Uri? = null
    ): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            when {
                !filePath.isNullOrBlank() -> retriever.setDataSource(filePath)
                uri != null -> retriever.setDataSource(context, uri)
                else -> return@withContext null
            }
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val targetUs = ((durationMs - 80L).coerceAtLeast(0L)) * 1000L
            retriever.getFrameAtTime(targetUs, MediaMetadataRetriever.OPTION_CLOSEST)
                ?: retriever.getFrameAtTime(targetUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
        } catch (e: Exception) {
            Log.e(TAG, "Failed extracting last frame", e)
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun savePng(bitmap: Bitmap, dest: File) {
        dest.parentFile?.mkdirs()
        FileOutputStream(dest).use { bitmap.compress(Bitmap.CompressFormat.PNG, 92, it) }
    }

    private fun drawFrame(
        canvas: Canvas,
        frames: List<Bitmap>,
        progress: Float,
        motion: VideoCameraMotion,
        w: Int,
        h: Int
    ) {
        canvas.drawColor(Color.BLACK)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val segments = (frames.size - 1).coerceAtLeast(1)
        val scaled = progress.coerceIn(0f, 1f) * segments
        val a = scaled.toInt().coerceIn(0, frames.lastIndex)
        val b = (a + 1).coerceIn(0, frames.lastIndex)
        val localT = smoothStep((scaled - a).coerceIn(0f, 1f))
        val eased = smoothStep(progress.coerceIn(0f, 1f))

        paint.alpha = 255
        canvas.drawBitmap(frames[a], motionMatrix(w, h, eased, motion, 0f), paint)
        if (b != a && localT > 0.005f) {
            paint.alpha = (localT * 255f).toInt().coerceIn(0, 255)
            canvas.drawBitmap(frames[b], motionMatrix(w, h, eased, motion, 0.02f), paint)
        }
    }

    private fun motionMatrix(w: Int, h: Int, t: Float, style: VideoCameraMotion, offset: Float): Matrix {
        val m = Matrix()
        val u = (t + offset).coerceIn(0f, 1f)
        val scale: Float
        val tx: Float
        val ty: Float
        when (style) {
            VideoCameraMotion.ZOOM_IN, VideoCameraMotion.AUTO -> {
                scale = 1.0f + 0.14f * u
                tx = w * 0.02f * sin(u * PI).toFloat()
                ty = -h * 0.015f * u
            }
            VideoCameraMotion.ZOOM_OUT -> {
                scale = 1.15f - 0.14f * u
                tx = -w * 0.015f * sin(u * PI).toFloat()
                ty = h * 0.01f * (1f - u)
            }
            VideoCameraMotion.PAN_RIGHT -> {
                scale = 1.14f
                tx = w * (0.06f - 0.12f * u)
                ty = 0f
            }
            VideoCameraMotion.PAN_LEFT -> {
                scale = 1.14f
                tx = w * (-0.06f + 0.12f * u)
                ty = 0f
            }
            VideoCameraMotion.ORBIT_DRIFT -> {
                scale = 1.08f + 0.06f * sin(u * PI).toFloat()
                tx = w * 0.035f * sin(u * 2.0 * PI).toFloat()
                ty = h * 0.025f * (1f - cos(u * PI).toFloat()) * 0.5f
            }
        }
        m.postScale(scale, scale, w / 2f, h / 2f)
        m.postTranslate(tx, ty)
        return m
    }

    private fun smoothStep(t: Float): Float = t * t * (3f - 2f * t)

    private fun centerCropScale(src: Bitmap, targetW: Int, targetH: Int): Bitmap {
        val out = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val scale = maxOf(targetW.toFloat() / src.width, targetH.toFloat() / src.height)
        val sw = src.width * scale
        val sh = src.height * scale
        val left = (targetW - sw) / 2f
        val top = (targetH - sh) / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(src, null, RectF(left, top, left + sw, top + sh), paint)
        return out
    }

    private fun findVideoTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            if (extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME).orEmpty().startsWith("video/")) {
                return i
            }
        }
        return -1
    }

    private fun mapFlags(extractorFlags: Int): Int {
        return if ((extractorFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
            MediaCodec.BUFFER_FLAG_KEY_FRAME
        } else 0
    }
}
