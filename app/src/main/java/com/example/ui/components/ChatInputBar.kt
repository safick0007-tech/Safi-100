package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.model.StudioSettings
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalStudioPalette

@Composable
fun ChatInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    attachedImageUri: Uri?,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
    isGenerating: Boolean,
    settings: StudioSettings,
    onSend: () -> Unit,
    onStop: () -> Unit,
    videoCount: Int = 0,
    onOpenVideoStudio: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    val canSend = (inputText.isNotBlank() || attachedImageUri != null) && !isGenerating

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.canvas)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 820.dp)
        ) {
            // Attached Image Thumbnail Preview
            AnimatedVisibility(visible = attachedImageUri != null) {
                if (attachedImageUri != null) {
                    Row(
                        modifier = Modifier
                            .padding(bottom = 8.dp, start = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.card)
                            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = attachedImageUri,
                            contentDescription = "Attached image preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Image attached",
                                style = MaterialTheme.typography.labelLarge.copy(color = palette.textPrimary)
                            )
                            Text(
                                text = "Ready for multimodal vision",
                                style = MaterialTheme.typography.labelSmall.copy(color = palette.liquid)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onRemoveImage,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("remove_image_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.action_remove_image),
                                tint = palette.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Elevated Floating Composer Container
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = palette.card,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (isGenerating) palette.emerald.copy(alpha = 0.6f) else palette.border,
                        shape = RoundedCornerShape(22.dp)
                    )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 6.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Zero-permission Photo Picker Trigger
                        IconButton(
                            onClick = onPickImage,
                            enabled = !isGenerating,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("attach_image_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddPhotoAlternate,
                                contentDescription = stringResource(R.string.action_attach_image),
                                tint = if (attachedImageUri != null) palette.liquid else palette.textSecondary
                            )
                        }

                        // Free Video Studio Trigger beside Chat Box
                        IconButton(
                            onClick = onOpenVideoStudio,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("open_video_studio_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(palette.emeraldSoft)
                                    .border(1.dp, palette.emerald.copy(alpha = 0.45f), RoundedCornerShape(11.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MovieCreation,
                                    contentDescription = stringResource(R.string.action_open_video_studio),
                                    tint = palette.emerald,
                                    modifier = Modifier.size(19.dp)
                                )
                                if (videoCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(2.dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(palette.liquid)
                                    )
                                }
                            }
                        }

                        // Multiline Composer TextField
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp, max = 148.dp)
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (inputText.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.input_placeholder),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = palette.textMuted
                                    )
                                )
                            }
                            BasicTextField(
                                value = inputText,
                                onValueChange = onInputChange,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = palette.textPrimary
                                ),
                                cursorBrush = SolidColor(palette.emerald),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("chat_input_field")
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Send or Stop Button
                        if (isGenerating) {
                            IconButton(
                                onClick = onStop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("stop_generation_button")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(palette.dangerSoft)
                                        .border(1.dp, palette.danger.copy(alpha = 0.6f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Stop,
                                        contentDescription = stringResource(R.string.action_stop),
                                        tint = palette.danger,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        } else {
                            IconButton(
                                onClick = onSend,
                                enabled = canSend,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("send_message_button")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (canSend) palette.emerald else palette.elevated
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Send,
                                        contentDescription = stringResource(R.string.action_send),
                                        tint = if (canSend) {
                                            if (palette.isDark) palette.canvas else palette.surface
                                        } else {
                                            palette.textMuted
                                        },
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Subtle Active Engine Telemetry Footer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(palette.surface.copy(alpha = 0.55f))
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AutoFixHigh,
                                contentDescription = null,
                                tint = if (settings.autoTuneEnabled) palette.emerald else palette.textMuted,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (settings.autoTuneEnabled) {
                                    "AutoTune: ${settings.autoTuneStrategy.displayName}"
                                } else {
                                    "Manual: T=${String.format("%.2f", settings.modelParameters.temperature)}"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMono,
                                    color = palette.textMuted
                                )
                            )
                        }

                        val badges = buildList {
                            if (settings.coachingEnabled && settings.activeMode.name == "ULTRA") add("COACH")
                            if (settings.liquidEnabled) add("LIQUID")
                            if (settings.customSystemPromptEnabled && settings.customSystemPrompt.isNotBlank()) add("SYS")
                        }
                        if (badges.isNotEmpty()) {
                            Text(
                                text = badges.joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.liquid
                                )
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}
