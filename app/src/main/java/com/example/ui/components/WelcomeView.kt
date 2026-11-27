package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Architecture
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Hexagon
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.StudioSettings
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalStudioPalette

private data class StarterPrompt(
    val title: String,
    val subtitle: String,
    val prompt: String,
    val icon: ImageVector,
    val tag: String
)

@Composable
fun WelcomeView(
    settings: StudioSettings,
    onPromptClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    val (modeColor, modeSoft, modeIcon) = modeVisuals(settings.activeMode)

    val starters = listOf(
        StarterPrompt(
            title = "Clean Architecture Blueprint",
            subtitle = "Compare offline-first sync patterns for mobile apps",
            prompt = "Compare Room + WorkManager vs real-time cloud sync architectures for a modern Android app. Include clear trade-offs and a concise Kotlin example.",
            icon = Icons.Rounded.Architecture,
            tag = "ENGINEERING"
        ),
        StarterPrompt(
            title = "Write & Optimize Code",
            subtitle = "Production-ready algorithms with complexity breakdown",
            prompt = "Write a clean, thread-safe Kotlin LRU Cache with TTL expiration, explain its time complexity, and highlight edge cases.",
            icon = Icons.Rounded.Code,
            tag = "CODE LAB"
        ),
        StarterPrompt(
            title = "Side-by-Side Decision",
            subtitle = "Race multiple reasoning angles for sharper clarity",
            prompt = "I have 6 weeks to launch a focused SaaS MVP as a solo builder. Give me a realistic week-by-week execution plan and the top 3 mistakes to avoid.",
            icon = Icons.Rounded.CompareArrows,
            tag = "STRATEGY"
        ),
        StarterPrompt(
            title = "Creative Product Concept",
            subtitle = "Fresh UX naming, positioning, and feature hooks",
            prompt = "Design a standout onboarding experience and visual identity concept for a calm, distraction-free personal knowledge notebook app.",
            icon = Icons.Rounded.Lightbulb,
            tag = "CREATIVE"
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Studio Emblem
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                palette.emeraldSoft,
                                palette.liquidSoft
                            )
                        )
                    )
                    .border(1.5.dp, palette.emerald.copy(alpha = 0.5f), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Hexagon,
                    contentDescription = stringResource(R.string.app_name),
                    tint = palette.emerald,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.welcome_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = palette.textSecondary
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 480.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Active Mode Indicator Chip
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(modeSoft)
                    .border(1.dp, modeColor.copy(alpha = 0.4f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = modeIcon,
                    contentDescription = null,
                    tint = modeColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${settings.activeMode.displayName} Active · ${settings.preferredProvider.displayName}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = modeColor,
                        fontFamily = JetBrainsMono
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Starter Prompt Cards (2x2 or stacked fluidly)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                starters.chunked(2).forEachIndexed { rowIdx, pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEachIndexed { colIdx, item ->
                            val accent = when (rowIdx * 2 + colIdx) {
                                0 -> palette.emerald
                                1 -> palette.liquid
                                2 -> palette.ultra
                                else -> palette.classic
                            }
                            StarterCard(
                                item = item,
                                accent = accent,
                                onClick = { onPromptClick(item.prompt) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("starter_card_${rowIdx}_${colIdx}")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StarterCard(
    item: StarterPrompt,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(palette.card)
            .border(1.dp, palette.border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = item.tag,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMono,
                    color = accent
                )
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleSmall.copy(
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.subtitle,
            style = MaterialTheme.typography.bodySmall.copy(
                color = palette.textSecondary
            ),
            maxLines = 2
        )
    }
}
