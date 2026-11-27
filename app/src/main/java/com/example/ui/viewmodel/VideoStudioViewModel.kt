package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.VideoClipEntity
import com.example.domain.model.FreeVideoModel
import com.example.domain.model.VideoCameraMotion
import com.example.domain.model.VideoClipRecord
import com.example.domain.model.VideoRatio
import com.example.domain.model.VideoRenderProgress
import com.example.domain.model.VideoStudioMode
import com.example.domain.video.FreeVideoGenerator
import com.example.domain.video.Mp4VideoEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

enum class ClipHistoryFilter(val label: String) {
    ALL("All"),
    EXTENDED("Extended"),
    RATIO_16_9("16:9"),
    RATIO_9_16("9:16")
}

data class VideoStudioState(
    val activeTab: Int = 0, // 0 = Create & Extend, 1 = Extend History
    val mode: VideoStudioMode = VideoStudioMode.TEXT_TO_VIDEO,
    val prompt: String = "",
    val ratio: VideoRatio = VideoRatio.LANDSCAPE_16_9,
    val durationSec: Int = 8,
    val selectedModel: FreeVideoModel = FreeVideoModel.VEO3_FAST,
    val cameraMotion: VideoCameraMotion = VideoCameraMotion.AUTO,
    val sourceImageUri: Uri? = null,
    val extendParentClip: VideoClipRecord? = null,
    val selectedLastFramePath: String? = null,
    val selectedLastFrameLabel: String? = null,
    val isGenerating: Boolean = false,
    val progress: VideoRenderProgress? = null,
    val errorMessage: String? = null,
    val clips: List<VideoClipRecord> = emptyList(),
    val filter: ClipHistoryFilter = ClipHistoryFilter.ALL,
    val playingClip: VideoClipRecord? = null
)

class VideoStudioViewModel(application: Application) : AndroidViewModel(application) {
    private val clipDao = AppDatabase.getInstance(application).videoClipDao()
    private val generator = FreeVideoGenerator(application)

    private val _activeTab = MutableStateFlow(0)
    private val _mode = MutableStateFlow(VideoStudioMode.TEXT_TO_VIDEO)
    private val _prompt = MutableStateFlow("")
    private val _ratio = MutableStateFlow(VideoRatio.LANDSCAPE_16_9)
    private val _durationSec = MutableStateFlow(8)
    private val _selectedModel = MutableStateFlow(FreeVideoModel.VEO3_FAST)
    private val _cameraMotion = MutableStateFlow(VideoCameraMotion.AUTO)
    private val _sourceImageUri = MutableStateFlow<Uri?>(null)
    private val _extendParentClip = MutableStateFlow<VideoClipRecord?>(null)
    private val _selectedLastFramePath = MutableStateFlow<String?>(null)
    private val _selectedLastFrameLabel = MutableStateFlow<String?>(null)
    private val _isGenerating = MutableStateFlow(false)
    private val _progress = MutableStateFlow<VideoRenderProgress?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _filter = MutableStateFlow(ClipHistoryFilter.ALL)
    private val _playingClip = MutableStateFlow<VideoClipRecord?>(null)

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    private var activeJob: Job? = null

    private val clipsFlow = clipDao.observeAllClips().map { list -> list.map { it.toDomain() } }

    private val formFlow = combine(_activeTab, _mode, _prompt, _ratio, _durationSec) { tab, m, p, r, d ->
        FormData(tab, m, p, r, d)
    }
    private val modelFlow = combine(_selectedModel, _cameraMotion, _sourceImageUri, _extendParentClip, _selectedLastFramePath) { mdl, mot, img, par, last ->
        ModelData(mdl, mot, img, par, last)
    }
    private val statusFlow = combine(_selectedLastFrameLabel, _isGenerating, _progress, _errorMessage, _filter) { lbl, gen, prog, err, flt ->
        StatusData(lbl, gen, prog, err, flt)
    }

    val uiState: StateFlow<VideoStudioState> = combine(
        formFlow,
        modelFlow,
        statusFlow,
        clipsFlow,
        _playingClip
    ) { form, mdl, st, clips, playing ->
        VideoStudioState(
            activeTab = form.activeTab,
            mode = form.mode,
            prompt = form.prompt,
            ratio = form.ratio,
            durationSec = form.durationSec,
            selectedModel = mdl.selectedModel,
            cameraMotion = mdl.cameraMotion,
            sourceImageUri = mdl.sourceImageUri,
            extendParentClip = mdl.extendParentClip,
            selectedLastFramePath = mdl.selectedLastFramePath,
            selectedLastFrameLabel = st.selectedLastFrameLabel,
            isGenerating = st.isGenerating,
            progress = st.progress,
            errorMessage = st.errorMessage,
            clips = clips,
            filter = st.filter,
            playingClip = playing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VideoStudioState()
    )

    fun seedPromptFromChat(chatInput: String) {
        if (_prompt.value.isBlank() && chatInput.isNotBlank()) {
            _prompt.value = chatInput.trim()
        }
    }

    fun selectTab(tab: Int) { _activeTab.value = tab }
    fun setMode(mode: VideoStudioMode) { _mode.value = mode; _errorMessage.value = null }
    fun setPrompt(text: String) { _prompt.value = text }
    fun setRatio(ratio: VideoRatio) { _ratio.value = ratio }
    fun setDurationSec(sec: Int) { _durationSec.value = sec.coerceIn(5, 15) }
    fun setModel(model: FreeVideoModel) { _selectedModel.value = model; _errorMessage.value = null }
    fun setCameraMotion(motion: VideoCameraMotion) { _cameraMotion.value = motion }
    fun setFilter(filter: ClipHistoryFilter) { _filter.value = filter }
    fun openPlayer(clip: VideoClipRecord?) { _playingClip.value = clip }

    fun setSourceImageUri(uri: Uri?) {
        _sourceImageUri.value = uri
        if (uri != null && _mode.value == VideoStudioMode.TEXT_TO_VIDEO) {
            _mode.value = VideoStudioMode.IMAGE_TO_VIDEO
        }
    }

    fun selectLastFrameFromHistory(clip: VideoClipRecord) {
        viewModelScope.launch {
            var framePath = clip.lastFramePath
            if (!File(framePath).exists()) {
                val extracted = Mp4VideoEngine.extractLastFrame(getApplication(), filePath = clip.videoPath)
                if (extracted != null) {
                    val dest = File(getApplication<Application>().filesDir, "studio_videos/last_${clip.id}.png")
                    Mp4VideoEngine.savePng(extracted, dest)
                    extracted.recycle()
                    framePath = dest.absolutePath
                }
            }
            _extendParentClip.value = clip
            _selectedLastFramePath.value = framePath
            _selectedLastFrameLabel.value = "Last frame · \"${clip.prompt.take(30)}\" (${clip.totalDurationSec}s · ${clip.ratio.label})"
            _ratio.value = clip.ratio
            _mode.value = VideoStudioMode.EXTEND_VIDEO
            _playingClip.value = null
            _activeTab.value = 0
            _errorMessage.value = null
            _events.tryEmit("Last frame loaded for Extend Video")
        }
    }

    fun selectLastFrameFromDeviceVideo(videoUri: Uri) {
        viewModelScope.launch {
            _errorMessage.value = null
            val bmp = Mp4VideoEngine.extractLastFrame(getApplication(), uri = videoUri)
            if (bmp == null) {
                _errorMessage.value = "Could not extract the last frame from the selected video."
                return@launch
            }
            val detectedRatio = if (bmp.height > bmp.width) VideoRatio.PORTRAIT_9_16 else VideoRatio.LANDSCAPE_16_9
            val dest = File(
                getApplication<Application>().filesDir,
                "studio_videos/device_last_${UUID.randomUUID()}.png"
            )
            withContext(Dispatchers.IO) { Mp4VideoEngine.savePng(bmp, dest) }
            bmp.recycle()

            _extendParentClip.value = null
            _selectedLastFramePath.value = dest.absolutePath
            _selectedLastFrameLabel.value = "Extracted Last Frame from device video (${detectedRatio.label})"
            _ratio.value = detectedRatio
            _mode.value = VideoStudioMode.EXTEND_VIDEO
            _activeTab.value = 0
            _events.tryEmit("Last frame extracted from device video")
        }
    }

    fun clearExtendAnchor() {
        _extendParentClip.value = null
        _selectedLastFramePath.value = null
        _selectedLastFrameLabel.value = null
    }

    fun generateVideo() {
        if (_isGenerating.value) return
        val state = uiState.value
        val prompt = state.prompt.trim()
        if (prompt.isEmpty()) {
            _errorMessage.value = "Please enter a prompt describing your video scene."
            return
        }
        if (state.mode == VideoStudioMode.IMAGE_TO_VIDEO && state.sourceImageUri == null) {
            _errorMessage.value = "Please pick a starting image for Image-to-Video."
            return
        }
        if (state.mode == VideoStudioMode.EXTEND_VIDEO && state.selectedLastFramePath.isNullOrBlank()) {
            _errorMessage.value = "Select a Last Frame from Extend History or pick a video first."
            return
        }

        activeJob = viewModelScope.launch {
            _isGenerating.value = true
            _errorMessage.value = null
            _progress.value = VideoRenderProgress("Initializing Free Engine", "Preparing ${state.selectedModel.displayName}…", 0.05f)

            var anchorBmp: Bitmap? = null
            try {
                anchorBmp = when (state.mode) {
                    VideoStudioMode.IMAGE_TO_VIDEO -> state.sourceImageUri?.let { loadScaledBitmap(it) }
                    VideoStudioMode.EXTEND_VIDEO -> state.selectedLastFramePath?.let { path ->
                        withContext(Dispatchers.IO) { BitmapFactory.decodeFile(path) }
                    }
                    VideoStudioMode.TEXT_TO_VIDEO -> null
                }

                val created = generator.generate(
                    prompt = prompt,
                    mode = state.mode,
                    model = state.selectedModel,
                    ratio = state.ratio,
                    durationSec = state.durationSec,
                    cameraMotion = state.cameraMotion,
                    anchorBitmap = anchorBmp,
                    parentClip = if (state.mode == VideoStudioMode.EXTEND_VIDEO) state.extendParentClip else null
                ) { prog ->
                    _progress.value = prog
                }

                clipDao.upsertClip(created.toEntity())
                _playingClip.value = created
                _events.tryEmit("Generated ${created.totalDurationSec}s MP4 (${created.ratio.label})")
            } catch (e: CancellationException) {
                _events.tryEmit("Video generation stopped")
                throw e
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Video generation failed. Please retry."
            } finally {
                anchorBmp?.let { if (!it.isRecycled) it.recycle() }
                _isGenerating.value = false
                _progress.value = null
            }
        }
    }

    fun stopGeneration() {
        activeJob?.cancel()
        activeJob = null
        _isGenerating.value = false
        _progress.value = null
    }

    fun deleteClip(clip: VideoClipRecord) {
        viewModelScope.launch {
            if (_playingClip.value?.id == clip.id) _playingClip.value = null
            if (_extendParentClip.value?.id == clip.id) clearExtendAnchor()
            clipDao.deleteClip(clip.id)
            withContext(Dispatchers.IO) {
                runCatching { File(clip.videoPath).delete() }
                runCatching { File(clip.thumbPath).delete() }
                runCatching { File(clip.lastFramePath).delete() }
            }
            _events.tryEmit("Video deleted")
        }
    }

    fun exportClipToUri(clip: VideoClipRecord, destUri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val src = File(clip.videoPath)
                    if (!src.exists()) error("Video file missing")
                    getApplication<Application>().contentResolver.openOutputStream(destUri)?.use { out ->
                        src.inputStream().use { it.copyTo(out) }
                    } ?: error("Cannot write destination file")
                }
            }
            result.onSuccess { _events.tryEmit("MP4 saved successfully") }
                .onFailure { _events.tryEmit("Save failed: ${it.localizedMessage}") }
        }
    }

    private suspend fun loadScaledBitmap(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val bmp = getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            } ?: return@runCatching null
            val maxDim = 1024
            if (bmp.width <= maxDim && bmp.height <= maxDim) bmp
            else {
                val scale = maxDim.toFloat() / maxOf(bmp.width, bmp.height).toFloat()
                Bitmap.createScaledBitmap(
                    bmp,
                    (bmp.width * scale).toInt().coerceAtLeast(1),
                    (bmp.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            }
        }.getOrNull()
    }

    private fun VideoClipEntity.toDomain() = VideoClipRecord(
        id = id,
        prompt = prompt,
        mode = runCatching { VideoStudioMode.valueOf(mode) }.getOrDefault(VideoStudioMode.TEXT_TO_VIDEO),
        model = runCatching { FreeVideoModel.valueOf(model) }.getOrDefault(FreeVideoModel.VEO3_FAST),
        ratio = runCatching { VideoRatio.valueOf(ratio) }.getOrDefault(VideoRatio.LANDSCAPE_16_9),
        segmentDurationSec = segmentDurationSec,
        totalDurationSec = totalDurationSec,
        videoPath = videoPath,
        thumbPath = thumbPath,
        lastFramePath = lastFramePath,
        parentClipId = parentClipId,
        extendStep = extendStep,
        createdAt = createdAt
    )

    private fun VideoClipRecord.toEntity() = VideoClipEntity(
        id = id,
        prompt = prompt,
        mode = mode.name,
        model = model.name,
        ratio = ratio.name,
        segmentDurationSec = segmentDurationSec,
        totalDurationSec = totalDurationSec,
        videoPath = videoPath,
        thumbPath = thumbPath,
        lastFramePath = lastFramePath,
        parentClipId = parentClipId,
        extendStep = extendStep,
        createdAt = createdAt
    )

    private data class FormData(val activeTab: Int, val mode: VideoStudioMode, val prompt: String, val ratio: VideoRatio, val durationSec: Int)
    private data class ModelData(val selectedModel: FreeVideoModel, val cameraMotion: VideoCameraMotion, val sourceImageUri: Uri?, val extendParentClip: VideoClipRecord?, val selectedLastFramePath: String?)
    private data class StatusData(val selectedLastFrameLabel: String?, val isGenerating: Boolean, val progress: VideoRenderProgress?, val errorMessage: String?, val filter: ClipHistoryFilter)
}
