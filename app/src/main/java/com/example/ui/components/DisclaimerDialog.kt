package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Hexagon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalStudioPalette

@Composable
fun DisclaimerDialog(
    onAccept: () -> Unit
) {
    val palette = LocalStudioPalette.current
    AlertDialog(
        onDismissRequest = onAccept,
        containerColor = palette.card,
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.emeraldSoft)
                    .border(1.dp, palette.emerald.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Hexagon,
                    contentDescription = null,
                    tint = palette.emerald,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.disclaimer_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.disclaimer_body),
                    style = MaterialTheme.typography.bodyMedium.copy(color = palette.textSecondary)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Multi-model parallel racing & quality scoring\n• Liquid progressive answer refinement\n• AutoTune context-aware parameter control",
                    style = MaterialTheme.typography.bodySmall.copy(color = palette.emerald)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.emerald,
                    contentColor = if (palette.isDark) palette.canvas else palette.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("disclaimer_accept_button")
            ) {
                Text(
                    text = stringResource(R.string.disclaimer_accept),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    )
}
