package com.example.ui.components

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.domain.model.VideoClipRecord
import com.example.domain.model.VideoRatio
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalStudioPalette
import java.io.File

@Composable
fun VideoPlaybackModal(
    clip: VideoClipRecord,
    onExtendFromLastFrame: (VideoClipRecord) -> Unit,
    onSaveMp4: (VideoClipRecord) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalStudioPalette.current
    var isPlaying by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var surfaceRef by remember { mutableStateOf<Surface?>(null) }
    val aspectFloat = if (clip.ratio == VideoRatio.LANDSCAPE_16_9) 16f / 9f else 9f / 16f

    DisposableEffect(clip.id) {
        onDispose {
            runCatching { mediaPlayerRef?.stop() }
            runCatching { mediaPlayerRef?.release() }
            mediaPlayerRef = null
            runCatching { surfaceRef?.release() }
            surfaceRef = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = palette.surface,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .border(1.dp, palette.border, RoundedCornerShape(24.dp))
                .testTag("video_playback_modal")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (clip.extendStep > 0) "Extended Video · #${clip.extendStep}" else clip.mode.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "${clip.model.displayName} · ${clip.ratio.label} · ${clip.totalDurationSec}s",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMono,
                                color = palette.emerald
                            )
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close player", tint = palette.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.codeBg)
                        .border(1.dp, palette.border, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val file = File(clip.videoPath)
                    if (file.exists() && playbackError == null) {
                        AndroidView(
                            factory = { ctx ->
                                TextureView(ctx).apply {
                                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                        override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                                            try {
                                                val s = Surface(st)
                                                surfaceRef = s
                                                val mp = MediaPlayer().apply {
                                                    setDataSource(file.absolutePath)
                                                    setSurface(s)
                                                    isLooping = true
                                                    setOnPreparedListener {
                                                        it.start()
                                                        isPlaying = true
                                                    }
                                                    setOnErrorListener { _, what, extra ->
                                                        playbackError = "Playback error ($what/$extra)"
                                                        true
                                                    }
                                                    prepareAsync()
                                                }
                                                mediaPlayerRef = mp
                                            } catch (e: Exception) {
                                                playbackError = e.localizedMessage ?: "Unable to play video"
                                            }
                                        }

                                        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) = Unit

                                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                            runCatching { mediaPlayerRef?.stop() }
                                            runCatching { mediaPlayerRef?.release() }
                                            mediaPlayerRef = null
                                            runCatching { surfaceRef?.release() }
                                            surfaceRef = null
                                            return true
                                        }

                                        override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(aspectFloat)
                        )
                    } else {
                        Text(
                            text = playbackError ?: "Video file unavailable",
                            style = MaterialTheme.typography.bodyMedium.copy(color = palette.danger),
                            modifier = Modifier.padding(24.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                mediaPlayerRef?.let { mp ->
                                    runCatching {
                                        if (mp.isPlaying) {
                                            mp.pause()
                                            isPlaying = false
                                        } else {
                                            mp.start()
                                            isPlaying = true
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(palette.surface.copy(alpha = 0.85f))
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = palette.emerald
                            )
                        }
                        IconButton(
                            onClick = {
                                mediaPlayerRef?.let { mp ->
                                    runCatching {
                                        mp.seekTo(0)
                                        mp.start()
                                        isPlaying = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(palette.surface.copy(alpha = 0.85f))
                        ) {
                            Icon(Icons.Rounded.Replay, contentDescription = "Replay", tint = palette.textPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.card)
                        .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val lastFile = File(clip.lastFramePath)
                    if (lastFile.exists()) {
                        AsyncImage(
                            model = lastFile,
                            contentDescription = "Captured last frame",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, palette.liquid, RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LAST FRAME READY TO EXTEND",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMono,
                                color = palette.liquid
                            )
                        )
                        Text(
                            text = clip.prompt,
                            style = MaterialTheme.typography.bodySmall.copy(color = palette.textSecondary),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onExtendFromLastFrame(clip) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.emerald,
                            contentColor = if (palette.isDark) palette.canvas else palette.surface
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("player_extend_button")
                    ) {
                        Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Extend Last Frame", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { onSaveMp4(clip) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("player_save_mp4_button")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save MP4", maxLines = 1)
                    }
                }
            }
        }
    }
}
