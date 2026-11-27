package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalStudioPalette
import kotlinx.coroutines.delay

private sealed class MarkdownBlock {
    data class Paragraph(val text: String) : MarkdownBlock()
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class BulletItem(val marker: String, val text: String) : MarkdownBlock()
    data class Quote(val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
}

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = LocalStudioPalette.current.textPrimary
) {
    val palette = LocalStudioPalette.current
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }

    SelectionContainer(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            blocks.forEach { block ->
                when (block) {
                    is MarkdownBlock.Heading -> {
                        val style = when (block.level) {
                            1 -> MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            2 -> MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            else -> MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = palette.emerald
                            )
                        }
                        Text(
                            text = parseInlineMarkdown(block.text, palette.emerald, palette.elevated),
                            style = style,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    is MarkdownBlock.Paragraph -> {
                        Text(
                            text = parseInlineMarkdown(block.text, palette.emerald, palette.elevated),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = textColor,
                                lineHeight = 24.sp
                            )
                        )
                    }

                    is MarkdownBlock.BulletItem -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = block.marker,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = palette.emerald,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = if (block.marker.first().isDigit()) JetBrainsMono else MaterialTheme.typography.bodyMedium.fontFamily
                                ),
                                modifier = Modifier.padding(end = 8.dp, top = 1.dp)
                            )
                            Text(
                                text = parseInlineMarkdown(block.text, palette.emerald, palette.elevated),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = textColor,
                                    lineHeight = 23.sp
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    is MarkdownBlock.Quote -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.elevated.copy(alpha = 0.65f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(palette.liquid)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = parseInlineMarkdown(block.text, palette.emerald, palette.elevated),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = palette.textSecondary,
                                    fontStyle = FontStyle.Italic
                                )
                            )
                        }
                    }

                    is MarkdownBlock.CodeBlock -> {
                        CodeBlockCard(
                            language = block.language,
                            code = block.code
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeBlockCard(
    language: String,
    code: String
) {
    val palette = LocalStudioPalette.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, palette.border, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = palette.codeBg
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.codeHeader)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = language.ifBlank { "code" }.lowercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = palette.liquid,
                        fontFamily = JetBrainsMono
                    )
                )
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(code))
                        copied = true
                    }
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                        contentDescription = if (copied) "Copied code" else "Copy code",
                        tint = if (copied) palette.emerald else Color(0xFFA7B8CC),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (copied) "Copied" else "Copy",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (copied) palette.emerald else Color(0xFFC5D2E0)
                        )
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = JetBrainsMono,
                        color = Color(0xFFE2ECF8),
                        lineHeight = 19.sp
                    )
                )
            }
        }
    }
}

private fun parseMarkdownBlocks(input: String): List<MarkdownBlock> {
    val lines = input.replace("\r\n", "\n").split("\n")
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraphBuffer = mutableListOf<String>()

    fun flushParagraph() {
        if (paragraphBuffer.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(paragraphBuffer.joinToString("\n")))
            paragraphBuffer.clear()
        }
    }

    var i = 0
    while (i < lines.size) {
        val rawLine = lines[i]
        val trimmed = rawLine.trim()

        if (trimmed.startsWith("```")) {
            flushParagraph()
            val lang = trimmed.removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            blocks.add(MarkdownBlock.CodeBlock(lang, codeLines.joinToString("\n")))
            i++
            continue
        }

        if (trimmed.isEmpty()) {
            flushParagraph()
            i++
            continue
        }

        when {
            trimmed.startsWith("### ") -> {
                flushParagraph()
                blocks.add(MarkdownBlock.Heading(3, trimmed.removePrefix("### ").trim()))
            }
            trimmed.startsWith("## ") -> {
                flushParagraph()
                blocks.add(MarkdownBlock.Heading(2, trimmed.removePrefix("## ").trim()))
            }
            trimmed.startsWith("# ") -> {
                flushParagraph()
                blocks.add(MarkdownBlock.Heading(1, trimmed.removePrefix("# ").trim()))
            }
            trimmed.startsWith("> ") -> {
                flushParagraph()
                blocks.add(MarkdownBlock.Quote(trimmed.removePrefix("> ").trim()))
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                flushParagraph()
                blocks.add(MarkdownBlock.BulletItem("•", trimmed.drop(2).trim()))
            }
            Regex("^\\d+\\.\\s+.*").matches(trimmed) -> {
                flushParagraph()
                val dotIdx = trimmed.indexOf('.')
                val marker = trimmed.substring(0, dotIdx + 1)
                val body = trimmed.substring(dotIdx + 1).trim()
                blocks.add(MarkdownBlock.BulletItem(marker, body))
            }
            else -> paragraphBuffer.add(rawLine)
        }
        i++
    }
    flushParagraph()
    return blocks
}

private fun parseInlineMarkdown(
    text: String,
    codeColor: Color,
    codeBg: Color
): AnnotatedString = buildAnnotatedString {
    var i = 0
    val n = text.length
    while (i < n) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = JetBrainsMono,
                            color = codeColor,
                            background = codeBg.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    ) {
                        append(" ${text.substring(i + 1, end)} ")
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            text[i] == '*' && (i == 0 || text[i - 1] != '*') -> {
                val end = text.indexOf('*', i + 1)
                if (end != -1 && end > i + 1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            else -> {
                append(text[i])
                i++
            }
        }
    }
}
