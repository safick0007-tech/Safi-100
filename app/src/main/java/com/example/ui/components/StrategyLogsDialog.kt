package com.example.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.StrategyLogEntry
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalStudioPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StrategyLogsDialog(
    logs: List<StrategyLogEntry>,
    onClearLogs: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalStudioPalette.current
    val avgScore = remember(logs) {
        if (logs.isEmpty()) 0 else logs.map { it.score }.average().toInt()
    }
    val avgSec = remember(logs) {
        if (logs.isEmpty()) 0.0 else logs.map { it.durationMs }.average() / 1000.0
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = palette.surface,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 620.dp)
                .border(1.dp, palette.border, RoundedCornerShape(22.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Analytics,
                            contentDescription = null,
                            tint = palette.liquid,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Strategy & Race Logs",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close logs",
                            tint = palette.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // KPI Summary Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LogKpiCard("TOTAL RUNS", "${logs.size}", palette.emerald, Modifier.weight(1f))
                    LogKpiCard("AVG SCORE", "$avgScore/100", palette.liquid, Modifier.weight(1f))
                    LogKpiCard("AVG SPEED", String.format("%.1fs", avgSec), palette.ultra, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = palette.border)
                Spacer(modifier = Modifier.height(10.dp))

                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No race logs yet. Send a prompt to inspect performance telemetry.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = palette.textMuted)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(logs, key = { it.id }) { entry ->
                            val timeStr = remember(entry.timestamp) {
                                SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(entry.timestamp))
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(palette.card)
                                    .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${entry.modeName} · ${entry.winnerModel}",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            color = palette.emerald,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${entry.score}/100",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = JetBrainsMono,
                                            color = palette.liquid,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = entry.promptPreview,
                                    style = MaterialTheme.typography.bodySmall.copy(color = palette.textPrimary),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "$timeStr · ${String.format("%.1fs", entry.durationMs / 1000.0)} · ${entry.candidatesCount} racers",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMono,
                                            color = palette.textMuted
                                        )
                                    )
                                    if (entry.liquidApplied) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.WaterDrop,
                                                contentDescription = null,
                                                tint = palette.liquid,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "LIQUID",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = JetBrainsMono,
                                                    color = palette.liquid
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (logs.isNotEmpty()) {
                        TextButton(onClick = onClearLogs) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = null,
                                tint = palette.danger,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear Logs", color = palette.danger)
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = palette.emerald)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogKpiCard(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(palette.card)
            .border(1.dp, palette.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMono,
                color = palette.textMuted
            )
        )
    }
}
