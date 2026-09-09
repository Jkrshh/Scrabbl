package com.scrabbl.ai.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrabbl.ai.MainViewModel
import com.scrabbl.ai.engine.Board
import com.scrabbl.ai.engine.BoardType
import com.scrabbl.ai.engine.Direction
import com.scrabbl.ai.engine.FrenchScrabble
import com.scrabbl.ai.engine.Move
import com.scrabbl.ai.engine.Premium

@Composable
fun BoardEditorScreen(vm: MainViewModel, onGoToMoves: () -> Unit) {
    val board by vm.board.collectAsState()
    val rack by vm.rack.collectAsState()
    val moves by vm.moves.collectAsState()
    val computing by vm.computing.collectAsState()

    var selected by remember { mutableStateOf(board.size / 2 to board.size / 2) }
    var horizontal by remember { mutableStateOf(true) }
    var placeAsBlank by remember { mutableStateOf(false) }
    var previewIndex by remember(moves) { mutableIntStateOf(0) }
    val preview: Move? = moves.getOrNull(previewIndex)

    val scroll = rememberScrollState()

    fun advanceSelection() {
        val (r, c) = selected
        selected = if (horizontal) r to (c + 1).coerceAtMost(board.size - 1)
        else (r + 1).coerceAtMost(board.size - 1) to c
    }

    fun retreatSelection() {
        val (r, c) = selected
        selected = if (horizontal) r to (c - 1).coerceAtLeast(0)
        else (r - 1).coerceAtLeast(0) to c
    }

    fun place(letter: Char) {
        val (r, c) = selected
        vm.setCellManual(r, c, letter, isBlank = placeAsBlank)
        placeAsBlank = false
        advanceSelection()
    }

    fun backspace() {
        val (r, c) = selected
        if (board.letters[r][c] != 0) {
            vm.setCellManual(r, c, null)
        } else {
            retreatSelection()
            val (rr, cc) = selected
            vm.setCellManual(rr, cc, null)
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(scroll).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BoardType.entries.forEach { t ->
                FilterChip(
                    selected = board.type == t,
                    onClick = {
                        vm.setBoardType(t)
                        selected = t.size / 2 to t.size / 2
                    },
                    label = { Text(t.label, fontSize = 12.sp) },
                )
            }
        }

        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            BoardCanvas(
                board = board,
                selected = selected,
                horizontal = horizontal,
                preview = preview,
                onCell = { r, c -> selected = r to c },
                onLongPress = { r, c -> vm.cyclePremium(r, c) },
            )
        }
        Text(
            "Astuce : appuie longtemps sur une case vide pour changer sa prime (LD → LT → MD → MT → centre → normale).",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6C4B2A),
        )

        if (preview != null) {
            SuggestionBar(
                move = preview,
                boardSize = board.size,
                onPlay = { vm.playMove(preview) },
            )
        } else if (rack.isEmpty()) {
            HintCard("Ajoute tes lettres dans le chevalet — le meilleur coup s'affichera directement sur le plateau.")
        } else if (moves.isEmpty() && !computing) {
            HintCard("Aucun coup jouable trouvé avec ce chevalet.")
        } else if (computing) {
            HintCard("Recherche en cours…")
        }

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val (r, c) = selected
            Text(
                "Case ${'A' + c}${r + 1}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(4.dp))
            FilledTonalButton(onClick = { horizontal = !horizontal }) {
                Icon(
                    if (horizontal) Icons.Filled.SwapHoriz else Icons.Filled.SwapVert,
                    contentDescription = null,
                )
                Spacer(Modifier.width(6.dp))
                Text(if (horizontal) "Horizontal →" else "Vertical ↓")
            }
            Spacer(Modifier.width(4.dp))
            OutlinedButton(
                onClick = { placeAsBlank = !placeAsBlank },
                colors = if (placeAsBlank)
                    ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFFDCB0))
                else ButtonDefaults.outlinedButtonColors(),
            ) {
                Text(if (placeAsBlank) "★ joker armé" else "★ joker")
            }
        }

        LetterKeypad(
            onLetter = { place(it) },
            onBackspace = { backspace() },
            onBlank = { placeAsBlank = true },
        )

        RackInput(rack = rack, onRackChange = vm::setRack)

        Button(
            onClick = onGoToMoves,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Icon(Icons.Filled.Psychology, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text(
                if (computing) "Recherche…" else "Voir tous les coups 🧠",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        if (moves.size > 1) {
            Text(
                "Autres coups (tape pour prévisualiser)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            for ((i, m) in moves.take(4).withIndex()) {
                AlternativeMoveRow(
                    m,
                    board.size,
                    selected = i == previewIndex,
                    onClick = { previewIndex = i },
                )
            }
            if (moves.size > 4) {
                OutlinedButton(onClick = onGoToMoves, modifier = Modifier.fillMaxWidth()) {
                    Text("Voir les ${moves.size} coups →")
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.clearBoard() }) {
                Icon(Icons.Filled.Delete, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Vider le plateau")
            }
            OutlinedButton(onClick = { vm.resetPremiums() }) {
                Text("Reset primes")
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SuggestionBar(move: Move, boardSize: Int, onPlay: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFDFF7DF),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Meilleur coup", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                Text(move.word, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B5E20))
                Text(
                    "${move.humanCoord(boardSize)} · ${if (move.direction == Direction.HORIZONTAL) "→" else "↓"} · ${move.placements.size} tuile(s) · ${move.score} pts",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2E7D32),
                )
            }
            Button(
                onClick = onPlay,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34A853), contentColor = Color.White),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Jouer")
            }
        }
    }
}

@Composable
private fun AlternativeMoveRow(m: Move, boardSize: Int, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = if (selected) Color(0xFFDFF7DF) else Color(0xFFFFF8EC),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                .let { if (selected) it.border(1.dp, Color(0xFF34A853), RoundedCornerShape(10.dp)) else it },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${m.score}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFFC85A3E),
                modifier = Modifier.width(48.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(m.word, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${m.humanCoord(boardSize)} · ${if (m.direction == Direction.HORIZONTAL) "→" else "↓"} · ${m.placements.size} tuile(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray,
                )
            }
        }
    }
}

@Composable
private fun HintCard(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFF7E9CC),
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(text, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun BoardCanvas(
    board: Board,
    selected: Pair<Int, Int>,
    horizontal: Boolean,
    preview: Move?,
    onCell: (Int, Int) -> Unit,
    onLongPress: (Int, Int) -> Unit,
) {
    val n = board.size
    val previewCells: Map<Pair<Int, Int>, Pair<Int, Boolean>> =
        preview?.placements?.associate { (it.row to it.col) to (it.letter to it.isBlank) } ?: emptyMap()

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .pointerInput(n) {
                detectTapGestures(
                    onTap = { pos ->
                        val tile = size.width / n
                        val c = (pos.x / tile).toInt().coerceIn(0, n - 1)
                        val r = (pos.y / tile).toInt().coerceIn(0, n - 1)
                        onCell(r, c)
                    },
                    onLongPress = { pos ->
                        val tile = size.width / n
                        val c = (pos.x / tile).toInt().coerceIn(0, n - 1)
                        val r = (pos.y / tile).toInt().coerceIn(0, n - 1)
                        onLongPress(r, c)
                    },
                )
            },
    ) {
        val tile = size.width / n
        for (r in 0 until n) for (c in 0 until n) {
            val x = c * tile; val y = r * tile
            val prem = board.premium[r][c]
            val cellColor = when (prem) {
                Premium.DL -> Color(0xFF9AD7EB)
                Premium.TL -> Color(0xFF5D9CC9)
                Premium.DW -> Color(0xFFEEB0BC)
                Premium.TW -> Color(0xFFE05A6A)
                Premium.CENTER -> Color(0xFFFFB86B)
                else -> Color(0xFFF7E9CC)
            }
            drawRect(cellColor, androidx.compose.ui.geometry.Offset(x, y),
                androidx.compose.ui.geometry.Size(tile, tile))
            drawRect(Color(0x33000000),
                androidx.compose.ui.geometry.Offset(x, y),
                androidx.compose.ui.geometry.Size(tile, tile),
                style = Stroke(width = 1f))

            if (prem != Premium.NORMAL && prem != Premium.CENTER &&
                board.letters[r][c] == 0 && !previewCells.containsKey(r to c) && tile > 24f) {
                val label = when (prem) {
                    Premium.DL -> "LD"; Premium.TL -> "LT"
                    Premium.DW -> "MD"; Premium.TW -> "MT"
                    else -> ""
                }
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = 0x80000000.toInt()
                        textSize = tile * 0.22f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    drawText(label, x + tile / 2f, y + tile / 2f + tile * 0.08f, paint)
                }
            }

            val letter = board.letters[r][c]
            if (letter != 0) {
                val bg = if (board.blanks[r][c]) Color(0xFFFFE1BA) else Color(0xFFF1D9A0)
                drawRect(bg,
                    androidx.compose.ui.geometry.Offset(x + 2, y + 2),
                    androidx.compose.ui.geometry.Size(tile - 4, tile - 4))
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = 0xFF3B2A16.toInt()
                        textSize = tile * 0.55f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        isFakeBoldText = true
                    }
                    drawText(
                        FrenchScrabble.ch(letter).toString(),
                        x + tile / 2f,
                        y + tile / 2f + tile * 0.2f,
                        paint,
                    )
                    if (!board.blanks[r][c] && tile > 26f) {
                        val vPaint = android.graphics.Paint().apply {
                            color = 0xFF5C4224.toInt()
                            textSize = tile * 0.2f
                            textAlign = android.graphics.Paint.Align.RIGHT
                            isAntiAlias = true
                        }
                        drawText(
                            "${FrenchScrabble.value[letter]}",
                            x + tile - tile * 0.08f,
                            y + tile - tile * 0.08f,
                            vPaint,
                        )
                    }
                }
            }

            val pv = previewCells[r to c]
            if (pv != null && letter == 0) {
                val (pLetter, pIsBlank) = pv
                drawRect(Color(0xCC8BE28E),
                    androidx.compose.ui.geometry.Offset(x + 2, y + 2),
                    androidx.compose.ui.geometry.Size(tile - 4, tile - 4))
                drawRect(Color(0xFF1B5E20),
                    androidx.compose.ui.geometry.Offset(x + 2, y + 2),
                    androidx.compose.ui.geometry.Size(tile - 4, tile - 4),
                    style = Stroke(width = 3f))
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = 0xFF1B5E20.toInt()
                        textSize = tile * 0.55f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        isFakeBoldText = true
                    }
                    drawText(
                        FrenchScrabble.ch(pLetter).toString(),
                        x + tile / 2f,
                        y + tile / 2f + tile * 0.2f,
                        paint,
                    )
                    if (!pIsBlank && tile > 26f) {
                        val vPaint = android.graphics.Paint().apply {
                            color = 0xFF2E7D32.toInt()
                            textSize = tile * 0.2f
                            textAlign = android.graphics.Paint.Align.RIGHT
                            isAntiAlias = true
                        }
                        drawText(
                            "${FrenchScrabble.value[pLetter]}",
                            x + tile - tile * 0.08f,
                            y + tile - tile * 0.08f,
                            vPaint,
                        )
                    }
                    if (pIsBlank && tile > 20f) {
                        val jPaint = android.graphics.Paint().apply {
                            color = 0xFF1B5E20.toInt()
                            textSize = tile * 0.18f
                            textAlign = android.graphics.Paint.Align.LEFT
                            isAntiAlias = true
                            isFakeBoldText = true
                        }
                        drawText("★", x + tile * 0.08f, y + tile * 0.28f, jPaint)
                    }
                }
            }

            if (selected == r to c) {
                drawRect(Color(0xAA34C759),
                    androidx.compose.ui.geometry.Offset(x, y),
                    androidx.compose.ui.geometry.Size(tile, tile),
                    style = Stroke(width = 4f))
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = 0xFF1B5E20.toInt()
                        textSize = tile * 0.4f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        isFakeBoldText = true
                    }
                    drawText(
                        if (horizontal) "→" else "↓",
                        x + tile / 2f,
                        y + tile * 0.35f,
                        paint,
                    )
                }
            }
        }

        if (preview != null) {
            val startX = preview.col * tile
            val startY = preview.row * tile
            val endX = (preview.endCol + 1) * tile
            val endY = (preview.endRow + 1) * tile
            drawRect(
                color = Color(0xFF1B5E20),
                topLeft = androidx.compose.ui.geometry.Offset(startX + 1f, startY + 1f),
                size = androidx.compose.ui.geometry.Size(endX - startX - 2f, endY - startY - 2f),
                style = Stroke(
                    width = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f),
                ),
            )
        }
    }
}
