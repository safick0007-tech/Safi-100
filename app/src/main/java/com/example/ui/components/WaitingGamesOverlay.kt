package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.domain.model.WaitingGameType
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalStudioPalette
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

@Composable
fun WaitingGamesDialog(
    initialGame: WaitingGameType,
    statusBanner: String?,
    onDismiss: () -> Unit
) {
    val palette = LocalStudioPalette.current
    var activeGame by remember {
        mutableStateOf(if (initialGame == WaitingGameType.NONE) WaitingGameType.SNAKE else initialGame)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = palette.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.border, RoundedCornerShape(22.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = null,
                            tint = palette.emerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Waiting Lounge",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close game", tint = palette.textSecondary)
                    }
                }

                if (!statusBanner.isNullOrBlank()) {
                    Text(
                        text = statusBanner,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMono,
                            color = palette.liquid
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Game selector tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(WaitingGameType.SNAKE, WaitingGameType.GAME_2048, WaitingGameType.PONG).forEach { game ->
                        val selected = activeGame == game
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) palette.emeraldSoft else palette.card)
                                .border(
                                    1.dp,
                                    if (selected) palette.emerald else palette.border,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { activeGame = game }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = game.displayName,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = if (selected) palette.emerald else palette.textSecondary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (activeGame) {
                    WaitingGameType.SNAKE, WaitingGameType.NONE -> SnakeMiniGame()
                    WaitingGameType.GAME_2048 -> Mini2048Game()
                    WaitingGameType.PONG -> PongMiniGame()
                }
            }
        }
    }
}

@Composable
private fun SnakeMiniGame() {
    val palette = LocalStudioPalette.current
    val grid = 14
    var snake by remember { mutableStateOf(listOf(7 to 7, 6 to 7, 5 to 7)) }
    var dir by remember { mutableStateOf(1 to 0) }
    var food by remember { mutableStateOf(10 to 7) }
    var score by remember { mutableIntStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }

    fun reset() {
        snake = listOf(7 to 7, 6 to 7, 5 to 7)
        dir = 1 to 0
        food = Random.nextInt(grid) to Random.nextInt(grid)
        score = 0
        gameOver = false
    }

    LaunchedEffect(gameOver) {
        while (!gameOver) {
            delay(170)
            val head = snake.first()
            val next = (head.first + dir.first + grid) % grid to (head.second + dir.second + grid) % grid
            if (next in snake) {
                gameOver = true
            } else {
                val grew = next == food
                snake = if (grew) listOf(next) + snake else listOf(next) + snake.dropLast(1)
                if (grew) {
                    score += 10
                    var candidate = Random.nextInt(grid) to Random.nextInt(grid)
                    while (candidate in snake) {
                        candidate = Random.nextInt(grid) to Random.nextInt(grid)
                    }
                    food = candidate
                }
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (gameOver) "GAME OVER · SCORE $score" else "SCORE $score",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = JetBrainsMono,
                    color = if (gameOver) palette.danger else palette.emerald
                )
            )
            IconButton(onClick = { reset() }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Restart Snake", tint = palette.textSecondary)
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.15f)
                .clip(RoundedCornerShape(14.dp))
                .background(palette.codeBg)
                .border(1.dp, palette.border, RoundedCornerShape(14.dp))
        ) {
            val cellW = size.width / grid
            val cellH = size.height / grid
            // Food
            drawRoundRect(
                color = palette.ultra,
                topLeft = Offset(food.first * cellW + 2f, food.second * cellH + 2f),
                size = Size(cellW - 4f, cellH - 4f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            // Snake body
            snake.forEachIndexed { idx, (x, y) ->
                drawRoundRect(
                    color = if (idx == 0) palette.emerald else palette.liquid.copy(alpha = 0.85f),
                    topLeft = Offset(x * cellW + 2f, y * cellH + 2f),
                    size = Size(cellW - 4f, cellH - 4f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        DPadControls(
            onUp = { if (dir.second != 1) dir = 0 to -1 },
            onDown = { if (dir.second != -1) dir = 0 to 1 },
            onLeft = { if (dir.first != 1) dir = -1 to 0 },
            onRight = { if (dir.first != -1) dir = 1 to 0 }
        )
    }
}

@Composable
private fun Mini2048Game() {
    val palette = LocalStudioPalette.current
    var board by remember { mutableStateOf(spawnTile(spawnTile(List(16) { 0 }))) }
    var score by remember { mutableIntStateOf(0) }

    fun move(direction: String) {
        val (nextBoard, gained) = slide2048(board, direction)
        if (nextBoard != board) {
            board = spawnTile(nextBoard)
            score += gained
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "2048 · SCORE $score",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = JetBrainsMono,
                    color = palette.emerald
                )
            )
            IconButton(onClick = {
                board = spawnTile(spawnTile(List(16) { 0 }))
                score = 0
            }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Restart 2048", tint = palette.textSecondary)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(palette.codeBg)
                .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (r in 0 until 4) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (c in 0 until 4) {
                        val v = board[r * 4 + c]
                        val tileColor = when (v) {
                            0 -> palette.card
                            2 -> palette.emeraldSoft
                            4 -> palette.liquidSoft
                            8 -> palette.classicSoft
                            16 -> palette.ultraSoft
                            else -> palette.emerald.copy(alpha = 0.35f)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(tileColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (v > 0) {
                                Text(
                                    text = "$v",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMono,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        DPadControls(
            onUp = { move("UP") },
            onDown = { move("DOWN") },
            onLeft = { move("LEFT") },
            onRight = { move("RIGHT") }
        )
    }
}

@Composable
private fun PongMiniGame() {
    val palette = LocalStudioPalette.current
    var paddleX by remember { mutableFloatStateOf(0.5f) }
    var ballX by remember { mutableFloatStateOf(0.5f) }
    var ballY by remember { mutableFloatStateOf(0.35f) }
    var vx by remember { mutableFloatStateOf(0.018f) }
    var vy by remember { mutableFloatStateOf(0.020f) }
    var score by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(20)
            ballX += vx
            ballY += vy
            if (ballX <= 0.04f || ballX >= 0.96f) vx = -vx
            if (ballY <= 0.05f) vy = abs(vy)
            if (ballY >= 0.86f && abs(ballX - paddleX) <= 0.16f && vy > 0f) {
                vy = -abs(vy)
                score += 1
            }
            if (ballY > 0.98f) {
                ballX = 0.5f
                ballY = 0.3f
                vy = 0.020f
                score = 0
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "PONG RALLY · $score (Drag paddle below)",
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = JetBrainsMono,
                color = palette.emerald
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.2f)
                .clip(RoundedCornerShape(14.dp))
                .background(palette.codeBg)
                .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        paddleX = (paddleX + dragAmount.x / size.width.coerceAtLeast(1)).coerceIn(0.14f, 0.86f)
                    }
                }
        ) {
            // Ball
            drawCircle(
                color = palette.liquid,
                radius = size.minDimension * 0.035f,
                center = Offset(ballX * size.width, ballY * size.height)
            )
            // Paddle
            val pw = size.width * 0.26f
            val ph = size.height * 0.045f
            drawRoundRect(
                color = palette.emerald,
                topLeft = Offset(paddleX * size.width - pw / 2f, size.height * 0.89f),
                size = Size(pw, ph),
                cornerRadius = CornerRadius(12f, 12f)
            )
        }
    }
}

@Composable
private fun DPadControls(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit
) {
    val palette = LocalStudioPalette.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DirButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Left", palette.card, palette.textPrimary, onLeft)
        DirButton(Icons.Rounded.KeyboardArrowUp, "Up", palette.card, palette.textPrimary, onUp)
        DirButton(Icons.Rounded.KeyboardArrowDown, "Down", palette.card, palette.textPrimary, onDown)
        DirButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Right", palette.card, palette.textPrimary, onRight)
    }
}

@Composable
private fun DirButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    bg: Color,
    tint: Color,
    onClick: () -> Unit
) {
    val palette = LocalStudioPalette.current
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, palette.border, RoundedCornerShape(12.dp))
    ) {
        Icon(icon, contentDescription = desc, tint = tint)
    }
}

private fun spawnTile(board: List<Int>): List<Int> {
    val empty = board.indices.filter { board[it] == 0 }
    if (empty.isEmpty()) return board
    val copy = board.toMutableList()
    copy[empty.random()] = if (Random.nextFloat() < 0.85f) 2 else 4
    return copy
}

private fun slideLine(line: List<Int>): Pair<List<Int>, Int> {
    val nonZero = line.filter { it != 0 }.toMutableList()
    val merged = mutableListOf<Int>()
    var gained = 0
    var i = 0
    while (i < nonZero.size) {
        if (i + 1 < nonZero.size && nonZero[i] == nonZero[i + 1]) {
            val doubled = nonZero[i] * 2
            merged.add(doubled)
            gained += doubled
            i += 2
        } else {
            merged.add(nonZero[i])
            i += 1
        }
    }
    while (merged.size < 4) merged.add(0)
    return merged to gained
}

private fun slide2048(board: List<Int>, direction: String): Pair<List<Int>, Int> {
    val out = MutableList(16) { 0 }
    var totalGain = 0
    for (i in 0 until 4) {
        val line = when (direction) {
            "LEFT" -> (0 until 4).map { c -> board[i * 4 + c] }
            "RIGHT" -> (3 downTo 0).map { c -> board[i * 4 + c] }
            "UP" -> (0 until 4).map { r -> board[r * 4 + i] }
            else -> (3 downTo 0).map { r -> board[r * 4 + i] }
        }
        val (slid, gain) = slideLine(line)
        totalGain += gain
        for (j in 0 until 4) {
            when (direction) {
                "LEFT" -> out[i * 4 + j] = slid[j]
                "RIGHT" -> out[i * 4 + (3 - j)] = slid[j]
                "UP" -> out[j * 4 + i] = slid[j]
                else -> out[(3 - j) * 4 + i] = slid[j]
            }
        }
    }
    return out to totalGain
}
