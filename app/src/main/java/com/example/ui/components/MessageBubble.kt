package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.model.ChatMessage
import com.example.domain.model.MagicMetadata
import com.example.domain.model.TastemakerScore
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalStudioPalette
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun MessageBubbleItem(
    message: ChatMessage,
    isGenerating: Boolean,
    onRetry: (ChatMessage) -> Unit,
    onEditAndResend: (ChatMessage, String) -> Unit,
    onSelectRaceIndex: (String, Int) -> Unit,
    onSelectLiquidIndex: (String, Int) -> Unit,
    onSaveImage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (message.isUser) {
        UserMessageBubble(
            message = message,
            isGenerating = isGenerating,
            onEditAndResend = onEditAndResend,
            modifier = modifier
        )
    } else {
        AssistantMessageBubble(
            message = message,
            isGenerating = isGenerating,
            onRetry = onRetry,
            onSelectRaceIndex = onSelectRaceIndex,
            onSelectLiquidIndex = onSelectLiquidIndex,
            onSaveImage = onSaveImage,
            modifier = modifier
        )
    }
}

@Composable
private fun UserMessageBubble(
    message: ChatMessage,
    isGenerating: Boolean,
    onEditAndResend: (ChatMessage, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editDraft by remember(message.content) { mutableStateOf(message.content) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.End
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 6.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
            color = palette.emeraldSoft,
            modifier = Modifier
                .widthIn(max = 560.dp)
                .border(
                    1.dp,
                    palette.emerald.copy(alpha = 0.35f),
                    RoundedCornerShape(topStart = 20.dp, topEnd = 6.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                )
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                if (!message.imageUri.isNullOrBlank()) {
                    AsyncImage(
                        model = Uri.parse(message.imageUri),
                        contentDescription = "User attached image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = palette.textPrimary
                    )
                )
            }
        }

        // Subtle user message actions
        Row(
            modifier = Modifier.padding(top = 2.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(
                onClick = {
                    clipboard.setText(AnnotatedString(message.content))
                    copied = true
                },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                    contentDescription = stringResource(R.string.action_copy),
                    tint = if (copied) palette.emerald else palette.textMuted,
                    modifier = Modifier.size(15.dp)
                )
            }
            if (!isGenerating) {
                IconButton(
                    onClick = {
                        editDraft = message.content
                        showEditDialog = true
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.action_edit),
                        tint = palette.textMuted,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = palette.card,
            title = {
                Text(
                    text = "Edit & Resend Prompt",
                    style = MaterialTheme.typography.titleMedium.copy(color = palette.textPrimary)
                )
            },
            text = {
                OutlinedTextField(
                    value = editDraft,
                    onValueChange = { editDraft = it },
                    minLines = 3,
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.emerald,
                        unfocusedBorderColor = palette.border,
                        focusedTextColor = palette.textPrimary,
                        unfocusedTextColor = palette.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEditDialog = false
                        onEditAndResend(message, editDraft)
                    }
                ) {
                    Text("Resend", color = palette.emerald)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = palette.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun AssistantMessageBubble(
    message: ChatMessage,
    isGenerating: Boolean,
    onRetry: (ChatMessage) -> Unit,
    onSelectRaceIndex: (String, Int) -> Unit,
    onSelectLiquidIndex: (String, Int) -> Unit,
    onSaveImage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    var magicExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }

    val activeCandidate = message.raceCandidates.getOrNull(message.activeRaceIndex)
    val activeScore = activeCandidate?.score ?: message.magic?.score
    val headerLabel = when {
        message.isError -> "Error"
        activeCandidate != null -> activeCandidate.label
        message.magic != null -> message.magic.winnerModel
        else -> "GodMode Studio"
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (message.isError) palette.dangerSoft else palette.card,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(
                width = 1.dp,
                color = if (message.isError) palette.danger.copy(alpha = 0.45f) else palette.border,
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Meta Header: Model/Winner badge + Score Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (message.isError) palette.danger.copy(alpha = 0.2f) else palette.emeraldSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (message.isError) Icons.Rounded.ErrorOutline else Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = if (message.isError) palette.danger else palette.emerald,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = headerLabel,
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (activeCandidate != null) {
                            Text(
                                text = "${activeCandidate.personaTag} · ${String.format("%.1fs", activeCandidate.durationMs / 1000.0)}",
                                style = MaterialTheme.typography.labelSmall.copy(color = palette.textMuted)
                            )
                        } else if (message.magic != null) {
                            Text(
                                text = "${message.magic.modeName} · ${String.format("%.1fs", message.magic.totalDurationMs / 1000.0)}",
                                style = MaterialTheme.typography.labelSmall.copy(color = palette.textMuted)
                            )
                        }
                    }
                }

                if (activeScore != null && !message.isError) {
                    ScoreBadgePill(score = activeScore)
                }
            }

            // Live Racing / Streaming Status Strip
            if (message.isStreaming && !message.statusText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.surface)
                        .border(1.dp, palette.emerald.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(15.dp),
                        strokeWidth = 2.dp,
                        color = palette.emerald
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = message.statusText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = palette.emerald,
                            fontFamily = JetBrainsMono
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Generated AI Photo Preview (if this assistant message produced an image)
            val generatedPath = message.imageUri
            if (!generatedPath.isNullOrBlank()) {
                val imgModel: Any = if (generatedPath.startsWith("/")) File(generatedPath) else Uri.parse(generatedPath)
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = imgModel,
                    contentDescription = "AI generated photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 380.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.surface)
                        .border(1.dp, palette.emerald.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .testTag("assistant_generated_image")
                )
            }

            // Main Markdown Content
            if (message.displayedContent.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                MarkdownText(
                    markdown = message.displayedContent,
                    textColor = if (message.isError) palette.danger else palette.textPrimary
                )
            }

            // Race Navigator (switch between all raced model responses)
            if (message.raceCandidates.size > 1) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.surface)
                        .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            val prev = (message.activeRaceIndex - 1).coerceAtLeast(0)
                            onSelectRaceIndex(message.id, prev)
                        },
                        enabled = message.activeRaceIndex > 0,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("race_prev_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "Previous model response",
                            tint = if (message.activeRaceIndex > 0) palette.textPrimary else palette.textMuted
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "RACE MODEL ${message.activeRaceIndex + 1} / ${message.raceCandidates.size}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMono,
                                color = palette.ultra,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = activeCandidate?.label.orEmpty(),
                            style = MaterialTheme.typography.labelSmall.copy(color = palette.textSecondary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = {
                            val next = (message.activeRaceIndex + 1).coerceAtMost(message.raceCandidates.lastIndex)
                            onSelectRaceIndex(message.id, next)
                        },
                        enabled = message.activeRaceIndex < message.raceCandidates.lastIndex,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("race_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Next model response",
                            tint = if (message.activeRaceIndex < message.raceCandidates.lastIndex) palette.textPrimary else palette.textMuted
                        )
                    }
                }
            }

            // Liquid Version Switcher (v0 -> v1 -> v2)
            if (message.liquidVersions.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                val activeLiquid = message.liquidVersions.getOrNull(message.activeLiquidIndex)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.liquidSoft)
                        .border(1.dp, palette.liquid.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            val prev = (message.activeLiquidIndex - 1).coerceAtLeast(0)
                            onSelectLiquidIndex(message.id, prev)
                        },
                        enabled = message.activeLiquidIndex > 0,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "Previous liquid version",
                            tint = if (message.activeLiquidIndex > 0) palette.liquid else palette.textMuted
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.WaterDrop,
                            contentDescription = null,
                            tint = palette.liquid,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${activeLiquid?.label ?: "Liquid"} (${message.activeLiquidIndex + 1}/${message.liquidVersions.size})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = palette.liquid,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    IconButton(
                        onClick = {
                            val next = (message.activeLiquidIndex + 1).coerceAtMost(message.liquidVersions.lastIndex)
                            onSelectLiquidIndex(message.id, next)
                        },
                        enabled = message.activeLiquidIndex < message.liquidVersions.lastIndex,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Next liquid version",
                            tint = if (message.activeLiquidIndex < message.liquidVersions.lastIndex) palette.liquid else palette.textMuted
                        )
                    }
                }
            }

            // Action Footer: Copy · Retry · Show Magic
            if (!message.isStreaming) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = palette.border.copy(alpha = 0.55f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!message.imageUri.isNullOrBlank() && message.imageUri.startsWith("/")) {
                            SmallActionChip(
                                icon = Icons.Rounded.Download,
                                label = "Save Photo",
                                accent = palette.emerald,
                                onClick = { onSaveImage(message.imageUri) },
                                modifier = Modifier.testTag("save_generated_photo_button")
                            )
                        }
                        SmallActionChip(
                            icon = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                            label = if (copied) stringResource(R.string.action_copied) else stringResource(R.string.action_copy),
                            accent = if (copied) palette.emerald else palette.textSecondary,
                            onClick = {
                                clipboard.setText(AnnotatedString(message.displayedContent))
                                copied = true
                            }
                        )
                        if (!isGenerating) {
                            SmallActionChip(
                                icon = Icons.Rounded.Refresh,
                                label = stringResource(R.string.action_retry),
                                accent = palette.textSecondary,
                                onClick = { onRetry(message) }
                            )
                        }
                    }

                    if (message.magic != null) {
                        SmallActionChip(
                            icon = if (magicExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            label = if (magicExpanded) stringResource(R.string.action_hide_magic) else stringResource(R.string.action_show_magic),
                            accent = palette.emerald,
                            onClick = { magicExpanded = !magicExpanded },
                            modifier = Modifier.testTag("show_magic_toggle")
                        )
                    }
                }

                // Expandable "Show Magic" Telemetry Inspector
                AnimatedVisibility(visible = magicExpanded && message.magic != null) {
                    message.magic?.let { magic ->
                        ShowMagicPanel(magic = magic)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreBadgePill(score: TastemakerScore) {
    val palette = LocalStudioPalette.current
    val (color, bg) = when {
        score.totalScore >= 88 -> palette.emerald to palette.emeraldSoft
        score.totalScore >= 76 -> palette.liquid to palette.liquidSoft
        score.totalScore >= 64 -> palette.ultra to palette.ultraSoft
        else -> palette.danger to palette.dangerSoft
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${score.totalScore}",
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = score.grade,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMono,
                color = color
            )
        )
    }
}

@Composable
private fun SmallActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(palette.surface)
            .border(1.dp, palette.border.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accent,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = accent,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun ShowMagicPanel(magic: MagicMetadata) {
    val palette = LocalStudioPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surface)
            .border(1.dp, palette.border, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "SHOW MAGIC · EXECUTION TELEMETRY",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMono,
                color = palette.emerald,
                fontWeight = FontWeight.Bold
            )
        )

        magic.score?.let { sc ->
            ScoreMetricBar("Quality & Depth", sc.qualityScore, palette.emerald)
            ScoreMetricBar("Clarity & Directness", sc.clarityScore, palette.liquid)
            ScoreMetricBar("Latency Speed", sc.speedScore, palette.ultra)
            Text(
                text = sc.rationale,
                style = MaterialTheme.typography.bodySmall.copy(color = palette.textSecondary)
            )
        }

        HorizontalDivider(color = palette.border.copy(alpha = 0.6f))

        Text(
            text = "Provider: ${magic.providerUsed} · Context: ${magic.autoTuneContext}",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMono,
                color = palette.textSecondary
            )
        )
        Text(
            text = "Params: T=${String.format("%.2f", magic.appliedParams.temperature)} · topP=${String.format("%.2f", magic.appliedParams.topP)} · maxTok=${magic.appliedParams.maxTokens}",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMono,
                color = palette.textMuted
            )
        )

        if (magic.processLogs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            magic.processLogs.forEach { line ->
                Text(
                    text = "▸ $line",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMono,
                        color = palette.textSecondary
                    )
                )
            }
        }
    }
}

@Composable
private fun ScoreMetricBar(label: String, value: Int, color: Color) {
    val palette = LocalStudioPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(color = palette.textSecondary)
            )
            Text(
                text = "$value / 100",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMono,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        LinearProgressIndicator(
            progress = { (value / 100f).coerceIn(0f, 1f) },
            color = color,
            trackColor = palette.elevated,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
    }
}
