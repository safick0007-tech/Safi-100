package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Hexagon
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.Conversation
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalStudioPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SidebarDrawerContent(
    conversations: List<Conversation>,
    activeConversationId: String?,
    searchQuery: String,
    logsCount: Int,
    onSearchChange: (String) -> Unit,
    onNewChat: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onRenameConversation: (String, String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onOpenLogs: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    var renameTarget by remember { mutableStateOf<Conversation?>(null) }
    var renameDraft by remember { mutableStateOf("") }

    val filtered = remember(conversations, searchQuery) {
        if (searchQuery.isBlank()) conversations
        else conversations.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    Surface(
        modifier = modifier.fillMaxHeight(),
        color = palette.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            // Branded Studio Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(palette.emeraldSoft)
                        .border(1.dp, palette.emerald.copy(alpha = 0.45f), RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Hexagon,
                        contentDescription = stringResource(R.string.app_name),
                        tint = palette.emerald,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = palette.emerald
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary New Chat Button
            Button(
                onClick = onNewChat,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sidebar_new_chat_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.emerald,
                    contentColor = if (palette.isDark) palette.canvas else palette.surface
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.action_new_chat),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Conversations
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.sidebar_search_hint),
                        style = MaterialTheme.typography.bodySmall.copy(color = palette.textMuted)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search conversations",
                        tint = palette.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.emerald,
                    unfocusedBorderColor = palette.border,
                    focusedContainerColor = palette.card,
                    unfocusedContainerColor = palette.card,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sidebar_search_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.sidebar_chats_title).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = palette.textMuted,
                    fontFamily = JetBrainsMono
                ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )

            // Conversations List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (filtered.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.sidebar_no_chats),
                                style = MaterialTheme.typography.bodySmall.copy(color = palette.textMuted)
                            )
                        }
                    }
                } else {
                    items(filtered, key = { it.id }) { conv ->
                        val isActive = conv.id == activeConversationId
                        ConversationRow(
                            conversation = conv,
                            isActive = isActive,
                            onSelect = { onSelectConversation(conv.id) },
                            onRenameClick = {
                                renameTarget = conv
                                renameDraft = conv.title
                            },
                            onDeleteClick = { onDeleteConversation(conv.id) }
                        )
                    }
                }
            }

            HorizontalDivider(
                color = palette.border.copy(alpha = 0.7f),
                modifier = Modifier.padding(vertical = 10.dp)
            )

            // Strategy Logs & Settings Footer Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = onOpenLogs,
                    shape = RoundedCornerShape(12.dp),
                    color = palette.card,
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                        .testTag("sidebar_logs_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Analytics,
                            contentDescription = "Strategy Logs",
                            tint = palette.liquid,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Logs ($logsCount)",
                            style = MaterialTheme.typography.labelLarge.copy(color = palette.textPrimary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(12.dp),
                    color = palette.card,
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                        .testTag("sidebar_settings_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.action_settings),
                            tint = palette.emerald,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_settings),
                            style = MaterialTheme.typography.labelLarge.copy(color = palette.textPrimary),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Local Self-Custody Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.card.copy(alpha = 0.7f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = palette.emerald,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.sidebar_local_storage),
                    style = MaterialTheme.typography.labelSmall.copy(color = palette.textMuted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = palette.card,
            title = {
                Text(
                    text = "Rename Conversation",
                    style = MaterialTheme.typography.titleMedium.copy(color = palette.textPrimary)
                )
            },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it },
                    singleLine = true,
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
                        renameTarget?.let { onRenameConversation(it.id, renameDraft) }
                        renameTarget = null
                    }
                ) {
                    Text(stringResource(R.string.action_save), color = palette.emerald)
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text(stringResource(R.string.action_cancel), color = palette.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun ConversationRow(
    conversation: Conversation,
    isActive: Boolean,
    onSelect: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val palette = LocalStudioPalette.current
    val dateLabel = remember(conversation.updatedAt) {
        SimpleDateFormat("MMM d · HH:mm", Locale.getDefault()).format(Date(conversation.updatedAt))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) palette.emeraldSoft else palette.card.copy(alpha = 0.65f))
            .border(
                1.dp,
                if (isActive) palette.emerald.copy(alpha = 0.5f) else palette.border.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onSelect)
            .padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
            .testTag("conversation_item_${conversation.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.ChatBubbleOutline,
            contentDescription = null,
            tint = if (isActive) palette.emerald else palette.textMuted,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = palette.textPrimary,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$dateLabel · ${conversation.messageCount} msg",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = palette.textMuted
                ),
                maxLines = 1
            )
        }
        IconButton(
            onClick = onRenameClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = stringResource(R.string.action_rename),
                tint = palette.textMuted,
                modifier = Modifier.size(16.dp)
            )
        }
        IconButton(
            onClick = onDeleteClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = stringResource(R.string.action_delete),
                tint = palette.danger.copy(alpha = 0.85f),
                modifier = Modifier.size(17.dp)
            )
        }
    }
}
