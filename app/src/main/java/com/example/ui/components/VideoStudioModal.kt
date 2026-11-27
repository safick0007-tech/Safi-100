package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.model.FreeVideoModel
import com.example.domain.model.VideoCameraMotion
import com.example.domain.model.VideoClipRecord
import com.example.domain.model.VideoRatio
import com.example.domain.model.VideoStudioMode
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.viewmodel.ClipHistoryFilter
import com.example.ui.viewmodel.VideoStudioState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val QUICK_DURATIONS = listOf(5, 8, 10, 12, 15)

@Composable
fun VideoStudioModal(
    state: VideoStudioState,
    onSelectTab: (Int) -> Unit,
    onModeChange: (VideoStudioMode) -> Unit,
    onPromptChange: (String) -> Unit,
    onModelChange: (FreeVideoModel) -> Unit,
    onRatioChange: (VideoRatio) -> Unit,
    onDurationChange: (Int) -> Unit,
    onCameraMotionChange: (VideoCameraMotion) -> Unit,
    onPickSourceImage: () -> Unit,
    onClearSourceImage: () -> Unit,
    onPickDeviceVideoForLastFrame: () -> Unit,
    onClearExtendAnchor: () -> Unit,
    onSelectLastFrameFromHistory: (VideoClipRecord) -> Unit,
    onFilterChange: (ClipHistoryFilter) -> Unit,
    onGenerate: () -> Unit,
    onStop: () -> Unit,
    onPlayClip: (VideoClipRecord) -> Unit,
    onExportClip: (VideoClipRecord) -> Unit,
    onDeleteClip: (VideoClipRecord) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalStudioPalette.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = palette.surface,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .border(1.dp, palette.border, RoundedCornerShape(24.dp))
                .testTag("video_studio_modal")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.card)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.emeraldSoft)
                                .border(1.dp, palette.emerald.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MovieCreation,
                                contentDescription = null,
                                tint = palette.emerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.video_studio_title),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = stringResource(R.string.video_studio_subtitle),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMono,
                                    color = palette.emerald
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_video_studio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.action_close),
                            tint = palette.textSecondary
                        )
                    }
                }

                // Top 2-Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surface)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudioTopTab(
                        title = "Create & Extend",
                        icon = Icons.Rounded.AutoAwesome,
                        selected = state.activeTab == 0,
                        onClick = { onSelectTab(0) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("video_tab_create")
                    )
                    StudioTopTab(
                        title = "Extend History (${state.clips.size})",
                        icon = Icons.Rounded.History,
                        selected = state.activeTab == 1,
                        onClick = { onSelectTab(1) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("video_tab_history")
                    )
                }

                // Tab Body
                if (state.activeTab == 0) {
                    CreateAndExtendTab(
                        state = state,
                        onModeChange = onModeChange,
                        onPromptChange = onPromptChange,
                        onModelChange = onModelChange,
                        onRatioChange = onRatioChange,
                        onDurationChange = onDurationChange,
                        onCameraMotionChange = onCameraMotionChange,
                        onPickSourceImage = onPickSourceImage,
                        onClearSourceImage = onClearSourceImage,
                        onOpenHistory = { onSelectTab(1) },
                        onPickDeviceVideoForLastFrame = onPickDeviceVideoForLastFrame,
                        onClearExtendAnchor = onClearExtendAnchor,
                        onGenerate = onGenerate,
                        onStop = onStop,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    ExtendHistoryTab(
                        clips = state.clips,
                        filter = state.filter,
                        onFilterChange = onFilterChange,
                        onPickDeviceVideoForLastFrame = onPickDeviceVideoForLastFrame,
                        onCreateFirst = { onSelectTab(0) },
                        onPlayClip = onPlayClip,
                        onSelectLastFrameFromHistory = onSelectLastFrameFromHistory,
                        onExportClip = onExportClip,
                        onDeleteClip = onDeleteClip,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioTopTab(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) palette.emeraldSoft else palette.card)
            .border(
                width = 1.dp,
                color = if (selected) palette.emerald else palette.border,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) palette.emerald else palette.textSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                color = if (selected) palette.emerald else palette.textSecondary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CreateAndExtendTab(
    state: VideoStudioState,
    onModeChange: (VideoStudioMode) -> Unit,
    onPromptChange: (String) -> Unit,
    onModelChange: (FreeVideoModel) -> Unit,
    onRatioChange: (VideoRatio) -> Unit,
    onDurationChange: (Int) -> Unit,
    onCameraMotionChange: (VideoCameraMotion) -> Unit,
    onPickSourceImage: () -> Unit,
    onClearSourceImage: () -> Unit,
    onOpenHistory: () -> Unit,
    onPickDeviceVideoForLastFrame: () -> Unit,
    onClearExtendAnchor: () -> Unit,
    onGenerate: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Mode Selector: Text to Video / Image to Video / Extend Video
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionLabel("GENERATION MODE")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VideoStudioMode.entries.forEach { mode ->
                    val selected = state.mode == mode
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) palette.liquidSoft else palette.card)
                            .border(
                                width = 1.dp,
                                color = if (selected) palette.liquid else palette.border,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable(enabled = !state.isGenerating) { onModeChange(mode) }
                            .padding(vertical = 10.dp, horizontal = 6.dp)
                            .testTag("video_mode_${mode.name.lowercase()}"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = mode.badge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) palette.liquid else palette.textMuted
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = mode.title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (selected) palette.textPrimary else palette.textSecondary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 2. Mode-Specific Anchor Card (Image-to-Video OR Extend Video Last Frame)
        if (state.mode == VideoStudioMode.IMAGE_TO_VIDEO) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.card)
                    .border(1.dp, palette.border, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Starting Reference Image",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    if (state.sourceImageUri != null) {
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelMedium.copy(color = palette.danger),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !state.isGenerating, onClick = onClearSourceImage)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (state.sourceImageUri != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = state.sourceImageUri,
                            contentDescription = "Selected starting image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, palette.emerald, RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Frame #1 Locked",
                                style = MaterialTheme.typography.labelLarge.copy(color = palette.emerald)
                            )
                            Text(
                                text = "Gemini vision will animate motion & camera continuation from this image.",
                                style = MaterialTheme.typography.bodySmall.copy(color = palette.textSecondary)
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onPickSourceImage,
                    enabled = !state.isGenerating,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pick_source_image_button")
                ) {
                    Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (state.sourceImageUri == null) "Pick Starting Image" else "Change Starting Image")
                }
            }
        } else if (state.mode == VideoStudioMode.EXTEND_VIDEO) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.card)
                    .border(
                        width = 1.dp,
                        color = if (state.selectedLastFramePath != null) palette.emerald.copy(alpha = 0.6f) else palette.border,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Select Last Frame From Video",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    if (state.selectedLastFramePath != null) {
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelMedium.copy(color = palette.danger),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !state.isGenerating, onClick = onClearExtendAnchor)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (state.selectedLastFramePath != null && File(state.selectedLastFramePath).exists()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = File(state.selectedLastFramePath),
                            contentDescription = "Selected video last frame",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.5.dp, palette.emerald, RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (state.extendParentClip != null) "SEAMLESS MP4 STITCH READY" else "LAST FRAME ANCHOR READY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMono,
                                    color = palette.emerald,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = state.selectedLastFrameLabel ?: "Last frame loaded",
                                style = MaterialTheme.typography.bodySmall.copy(color = palette.textSecondary),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Pick a clip from Extend History to seamlessly stitch + extend its final frame, or extract the exact last frame from any video on your device.",
                        style = MaterialTheme.typography.bodySmall.copy(color = palette.textSecondary)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenHistory,
                        enabled = !state.isGenerating,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("select_last_frame_from_history_button")
                    ) {
                        Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("From History", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = onPickDeviceVideoForLastFrame,
                        enabled = !state.isGenerating,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("select_last_frame_from_video_button")
                    ) {
                        Icon(Icons.Rounded.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Video", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // 3. Prompt Input
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionLabel(
                when (state.mode) {
                    VideoStudioMode.TEXT_TO_VIDEO -> "VIDEO SCENE PROMPT"
                    VideoStudioMode.IMAGE_TO_VIDEO -> "MOTION & ACTION PROMPT"
                    VideoStudioMode.EXTEND_VIDEO -> "CONTINUATION PROMPT (NEXT ${state.durationSec}s)"
                }
            )
            OutlinedTextField(
                value = state.prompt,
                onValueChange = onPromptChange,
                enabled = !state.isGenerating,
                placeholder = {
                    Text(
                        text = when (state.mode) {
                            VideoStudioMode.TEXT_TO_VIDEO -> "e.g., Neon rain-soaked Dhaka skyline at dusk, drones gliding through glowing clouds, cinematic 35mm..."
                            VideoStudioMode.IMAGE_TO_VIDEO -> "e.g., Camera slowly pushes in while wind ripples across the scene and golden light beams shift..."
                            VideoStudioMode.EXTEND_VIDEO -> "e.g., Continue smoothly forward as the camera rises into the misty sky and reveals a glowing city..."
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(color = palette.textMuted)
                    )
                },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = palette.card,
                    unfocusedContainerColor = palette.card,
                    focusedBorderColor = palette.emerald,
                    unfocusedBorderColor = palette.border,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary,
                    cursorColor = palette.emerald
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("video_prompt_input")
            )
        }

        // 4. 5 Free Model Tools Carousel
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel("FREE VIDEO MODELS (5 TOOLS)")
                Text(
                    text = "NO API KEY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMono,
                        color = palette.emerald,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FreeVideoModel.entries.forEach { model ->
                    val selected = state.selectedModel == model
                    Column(
                        modifier = Modifier
                            .width(172.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) palette.emeraldSoft else palette.card)
                            .border(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) palette.emerald else palette.border,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable(enabled = !state.isGenerating) { onModelChange(model) }
                            .padding(12.dp)
                            .testTag("video_model_${model.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = model.tag,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMono,
                                    color = if (selected) palette.emerald else palette.liquid,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Selected model",
                                    tint = palette.emerald,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = model.displayName,
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = model.engineSummary,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = palette.textSecondary
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 5. Aspect Ratio Picker (16:9 & 9:16)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionLabel("ASPECT RATIO")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VideoRatio.entries.forEach { ratio ->
                    val selected = state.ratio == ratio
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) palette.emeraldSoft else palette.card)
                            .border(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) palette.emerald else palette.border,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable(enabled = !state.isGenerating) { onRatioChange(ratio) }
                            .padding(horizontal = 12.dp)
                            .testTag("video_ratio_${ratio.label.replace(":", "_")}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(
                                    width = if (ratio == VideoRatio.LANDSCAPE_16_9) 26.dp else 16.dp,
                                    height = if (ratio == VideoRatio.LANDSCAPE_16_9) 16.dp else 26.dp
                                )
                                .clip(RoundedCornerShape(4.dp))
                                .border(
                                    width = 1.8.dp,
                                    color = if (selected) palette.emerald else palette.textSecondary,
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = ratio.label,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = JetBrainsMono,
                                    color = if (selected) palette.emerald else palette.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = ratio.subtitle,
                                style = MaterialTheme.typography.labelSmall.copy(color = palette.textSecondary)
                            )
                        }
                    }
                }
            }
        }

        // 6. Duration Picker (5s to 15s)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(palette.card)
                .border(1.dp, palette.border, RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel("VIDEO DURATION (5s – 15s)")
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.emeraldSoft)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${state.durationSec}s · 24fps",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = JetBrainsMono,
                            color = palette.emerald,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QUICK_DURATIONS.forEach { sec ->
                    val selected = state.durationSec == sec
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) palette.emerald else palette.surface)
                            .border(
                                width = 1.dp,
                                color = if (selected) palette.emerald else palette.border,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable(enabled = !state.isGenerating) { onDurationChange(sec) }
                            .testTag("video_duration_${sec}s"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${sec}s",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) {
                                    if (palette.isDark) palette.canvas else palette.surface
                                } else {
                                    palette.textPrimary
                                }
                            )
                        )
                    }
                }
            }

            Slider(
                value = state.durationSec.toFloat(),
                onValueChange = { onDurationChange(it.roundToInt()) },
                valueRange = 5f..15f,
                steps = 9,
                enabled = !state.isGenerating,
                colors = SliderDefaults.colors(
                    thumbColor = palette.emerald,
                    activeTrackColor = palette.emerald,
                    inactiveTrackColor = palette.border
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("video_duration_slider")
            )
        }

        // 7. Camera Motion Presets
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionLabel("CAMERA MOTION")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VideoCameraMotion.entries.forEach { motion ->
                    val selected = state.cameraMotion == motion
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) palette.liquidSoft else palette.card)
                            .border(
                                width = 1.dp,
                                color = if (selected) palette.liquid else palette.border,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable(enabled = !state.isGenerating) { onCameraMotionChange(motion) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("video_motion_${motion.name.lowercase()}")
                    ) {
                        Text(
                            text = motion.displayName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (selected) palette.liquid else palette.textSecondary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }

        // 8. Live Progress Card
        val progress = state.progress
        if (state.isGenerating && progress != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.emeraldSoft)
                    .border(1.dp, palette.emerald.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = progress.stageTitle,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = palette.emerald,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "${(progress.progress * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = JetBrainsMono,
                            color = palette.emerald,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                LinearProgressIndicator(
                    progress = { progress.progress.coerceIn(0f, 1f) },
                    color = palette.emerald,
                    trackColor = palette.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                )
                Text(
                    text = progress.detail,
                    style = MaterialTheme.typography.bodySmall.copy(color = palette.textPrimary)
                )
            }
        }

        // 9. Transparent Error Banner
        if (!state.errorMessage.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.dangerSoft)
                    .border(1.dp, palette.danger.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = "Error",
                    tint = palette.danger,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = state.errorMessage,
                    style = MaterialTheme.typography.bodySmall.copy(color = palette.textPrimary)
                )
            }
        }

        // 10. Primary Generate / Stop Action Button
        if (state.isGenerating) {
            Button(
                onClick = onStop,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.danger,
                    contentColor = palette.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("stop_video_generation_button")
            ) {
                Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Stop Generation",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        } else {
            Button(
                onClick = onGenerate,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.emerald,
                    contentColor = if (palette.isDark) palette.canvas else palette.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("generate_video_button")
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                val ctaLabel = when (state.mode) {
                    VideoStudioMode.TEXT_TO_VIDEO -> "Generate ${state.durationSec}s Video · ${state.selectedModel.displayName}"
                    VideoStudioMode.IMAGE_TO_VIDEO -> "Animate Image (${state.durationSec}s · ${state.ratio.label})"
                    VideoStudioMode.EXTEND_VIDEO -> "Extend Video +${state.durationSec}s (${state.ratio.label})"
                }
                Text(
                    text = ctaLabel,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun ExtendHistoryTab(
    clips: List<VideoClipRecord>,
    filter: ClipHistoryFilter,
    onFilterChange: (ClipHistoryFilter) -> Unit,
    onPickDeviceVideoForLastFrame: () -> Unit,
    onCreateFirst: () -> Unit,
    onPlayClip: (VideoClipRecord) -> Unit,
    onSelectLastFrameFromHistory: (VideoClipRecord) -> Unit,
    onExportClip: (VideoClipRecord) -> Unit,
    onDeleteClip: (VideoClipRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    val filteredClips = remember(clips, filter) {
        when (filter) {
            ClipHistoryFilter.ALL -> clips
            ClipHistoryFilter.EXTENDED -> clips.filter { it.extendStep > 0 }
            ClipHistoryFilter.RATIO_16_9 -> clips.filter { it.ratio == VideoRatio.LANDSCAPE_16_9 }
            ClipHistoryFilter.RATIO_9_16 -> clips.filter { it.ratio == VideoRatio.PORTRAIT_9_16 }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
    ) {
        // Filter Chips + Device Video Extract Shortcut
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ClipHistoryFilter.entries.forEach { item ->
                    val selected = filter == item
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) palette.emeraldSoft else palette.card)
                            .border(
                                width = 1.dp,
                                color = if (selected) palette.emerald else palette.border,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onFilterChange(item) }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("history_filter_${item.name.lowercase()}")
                    ) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (selected) palette.emerald else palette.textSecondary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onPickDeviceVideoForLastFrame,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("history_pick_device_video_button")
            ) {
                Icon(Icons.Rounded.VideoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Device Video", style = MaterialTheme.typography.labelSmall)
            }
        }

        if (filteredClips.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MovieCreation,
                        contentDescription = null,
                        tint = palette.textMuted,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = if (clips.isEmpty()) "No Generated Videos Yet" else "No Videos Match Filter",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Every generated MP4 automatically saves its exact Last Frame here so you can seamlessly extend it anytime.",
                        style = MaterialTheme.typography.bodySmall.copy(color = palette.textSecondary)
                    )
                    if (clips.isEmpty()) {
                        Button(
                            onClick = onCreateFirst,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.emerald,
                                contentColor = if (palette.isDark) palette.canvas else palette.surface
                            )
                        ) {
                            Text("Create First Free Video")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 6.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredClips, key = { it.id }) { clip ->
                    HistoryClipCard(
                        clip = clip,
                        onPlay = { onPlayClip(clip) },
                        onExtend = { onSelectLastFrameFromHistory(clip) },
                        onExport = { onExportClip(clip) },
                        onDelete = { onDeleteClip(clip) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryClipCard(
    clip: VideoClipRecord,
    onPlay: () -> Unit,
    onExtend: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = LocalStudioPalette.current
    val formattedDate = remember(clip.createdAt) {
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(clip.createdAt))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.card)
            .border(1.dp, palette.border, RoundedCornerShape(16.dp))
            .padding(12.dp)
            .testTag("history_clip_card_${clip.id}"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Playable Thumbnail
            Box(
                modifier = Modifier
                    .size(width = 104.dp, height = 74.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.codeBg)
                    .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                    .clickable(onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                val thumbFile = File(clip.thumbPath)
                if (thumbFile.exists()) {
                    AsyncImage(
                        model = thumbFile,
                        contentDescription = "Video thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(palette.surface.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play video",
                        tint = palette.emerald,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(palette.surface.copy(alpha = 0.85f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${clip.ratio.label} · ${clip.totalDurationSec}s",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMono,
                            color = palette.emerald
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (clip.extendStep > 0) palette.liquidSoft else palette.emeraldSoft)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (clip.extendStep > 0) "EXT #${clip.extendStep}" else clip.mode.badge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                color = if (clip.extendStep > 0) palette.liquid else palette.emerald
                            )
                        )
                    }
                    Text(
                        text = clip.model.displayName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = clip.prompt,
                    style = MaterialTheme.typography.bodySmall.copy(color = palette.textSecondary),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMono,
                        color = palette.textMuted
                    )
                )
            }
        }

        // Action Row: Select Last Frame & Extend + Save + Delete
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onExtend,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.emeraldSoft,
                    contentColor = palette.emerald
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .border(1.dp, palette.emerald.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .testTag("extend_clip_${clip.id}")
            ) {
                Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Select Last Frame & Extend",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onExport,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surface)
                    .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                    .testTag("export_clip_${clip.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = "Save MP4",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.dangerSoft)
                    .border(1.dp, palette.danger.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .testTag("delete_clip_${clip.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete video",
                    tint = palette.danger,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val palette = LocalStudioPalette.current
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = JetBrainsMono,
            color = palette.textMuted,
            fontWeight = FontWeight.Bold
        )
    )
}
