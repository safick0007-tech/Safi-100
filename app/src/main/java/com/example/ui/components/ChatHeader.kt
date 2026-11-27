package com.example.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.ChatMode
import com.example.domain.model.StudioSettings
import com.example.domain.model.WaitingGameType
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalStudioPalette

@Composable
fun ChatHeader(
    settings: StudioSettings,
    showMenuButton: Boolean,
    onOpenDrawer: () -> Unit,
    onModeSelected: (ChatMode) -> Unit,
    onToggleLiquid: () -> Unit,
    onNewChat: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    var modeDropdownExpanded by remember { mutableStateOf(false) }

    val (modeColor, modeSoft, modeIcon) = modeVisuals(settings.activeMode)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = palette.surface,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (showMenuButton) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("menu_drawer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Menu,
                            contentDescription = stringResource(R.string.action_menu),
                            tint = palette.textPrimary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Mode Switcher Pill + Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(modeSoft)
                            .border(1.dp, modeColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .clickable { modeDropdownExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("mode_selector_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = modeIcon,
                            contentDescription = settings.activeMode.displayName,
                            tint = modeColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = settings.activeMode.displayName,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = when (settings.activeMode) {
                                    ChatMode.ULTRA -> "${settings.speedTier.displayName} · ${settings.preferredProvider.displayName}"
                                    ChatMode.CLASSIC -> if (settings.classicFullCombo) "5 Personas · Parallel" else settings.classicSelectedPersona
                                    ChatMode.PROMPT_LAB -> "${settings.promptLabModel} · Stream"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = modeColor
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.ExpandMore,
                            contentDescription = "Switch mode",
                            tint = palette.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = modeDropdownExpanded,
                        onDismissRequest = { modeDropdownExpanded = false },
                        modifier = Modifier
                            .widthIn(min = 270.dp, max = 320.dp)
                            .background(palette.card)
                    ) {
                        ChatMode.entries.forEach { mode ->
                            val (accent, soft, icon) = modeVisuals(mode)
                            val selected = mode == settings.activeMode
                            DropdownMenuItem(
                                modifier = Modifier.testTag("mode_option_${mode.name.lowercase()}"),
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(soft),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = accent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = mode.displayName,
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        color = palette.textPrimary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                                if (selected) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = "Selected",
                                                        tint = accent,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = mode.description,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = palette.textSecondary
                                                )
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    modeDropdownExpanded = false
                                    onModeSelected(mode)
                                }
                            )
                        }
                    }
                }

                // Quick Liquid Toggle Pill
                Surface(
                    onClick = onToggleLiquid,
                    shape = RoundedCornerShape(12.dp),
                    color = if (settings.liquidEnabled) palette.liquidSoft else palette.card,
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (settings.liquidEnabled) palette.liquid.copy(alpha = 0.55f) else palette.border,
                            RoundedCornerShape(12.dp)
                        )
                        .testTag("liquid_quick_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WaterDrop,
                            contentDescription = if (settings.liquidEnabled) {
                                stringResource(R.string.liquid_on)
                            } else {
                                stringResource(R.string.liquid_off)
                            },
                            tint = if (settings.liquidEnabled) palette.liquid else palette.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (settings.liquidEnabled) "LIQUID" else "Liquid",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (settings.liquidEnabled) palette.liquid else palette.textSecondary,
                                fontWeight = if (settings.liquidEnabled) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                }

                // Prompt Counter Pill
                if (settings.promptCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.card)
                            .border(1.dp, palette.border, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "#${settings.promptCount}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = JetBrainsMono,
                                color = palette.textSecondary
                            )
                        )
                    }
                }

                // Waiting Mini-Game Launcher (when configured)
                if (settings.waitingGame != WaitingGameType.NONE) {
                    IconButton(
                        onClick = onOpenGame,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("waiting_game_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = stringResource(R.string.action_play_game),
                            tint = palette.emerald
                        )
                    }
                }

                // Quick Settings Trigger
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("header_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = stringResource(R.string.action_settings),
                        tint = palette.textSecondary
                    )
                }

                // Quick New Chat Button
                IconButton(
                    onClick = onNewChat,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("header_new_chat_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(palette.emeraldSoft)
                            .border(1.dp, palette.emerald.copy(alpha = 0.45f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.action_new_chat),
                            tint = palette.emerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            HorizontalDivider(color = palette.border.copy(alpha = 0.7f), thickness = 1.dp)
        }
    }
}

@Composable
fun modeVisuals(mode: ChatMode): Triple<Color, Color, ImageVector> {
    val palette = LocalStudioPalette.current
    return when (mode) {
        ChatMode.ULTRA -> Triple(palette.ultra, palette.ultraSoft, Icons.Rounded.Bolt)
        ChatMode.CLASSIC -> Triple(palette.classic, palette.classicSoft, Icons.Rounded.AutoAwesome)
        ChatMode.PROMPT_LAB -> Triple(palette.emerald, palette.emeraldSoft, Icons.Rounded.Science)
    }
}
