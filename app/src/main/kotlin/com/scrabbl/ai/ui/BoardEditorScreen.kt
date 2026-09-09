package com.scrabbl.ai.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrabbl.ai.MainViewModel
import com.scrabbl.ai.engine.BoardType
import com.scrabbl.ai.engine.FrenchScrabble
import com.scrabbl.ai.engine.Premium

/**
 * Éditeur manuel du plateau : utile pour corriger une détection incomplète
 * ou saisir une partie sans caméra. On tape sur une case → clavier apparaît
 * pour cette case seule.
 */
@Composable
fun BoardEditorScreen(vm: MainViewModel) {
    val board by vm.board.collectAsState()
    val rack by vm.rack.collectAsState()
    var selected by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    Column(
        Modifier.fillMaxWidth().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Sélecteur de type de plateau
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BoardType.values().forEach { t ->
                FilterChip(
                    selected = board.type == t,
                    onClick = { vm.setBoardType(t) },
                    label = { Text(t.label, fontSize = 12.sp) },
                )
            }
        }
        RackInput(rack = rack, onRackChange = vm::setRack)

        Box(
            Modifier.fillMaxWidth().aspectRatio(1f),
        ) {
            BoardCanvas(
                board = board,
                selected = selected,
                onCell = { r, c -> selected = r to c },
            )
        }

        selected?.let { (r, c) ->
            var value by remember(selected) {
                mutableStateOf(FrenchScrabble.ch(board.letters[r][c]).let { if (it == '?') "" else it.toString() })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Case ${r + 1},${'A' + c} :", modifier = Modifier.padding(end = 8.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        val s = it.take(1).uppercase()
                        value = s
                        vm.setCellManual(r, c, s.firstOrNull())
                    },
                    singleLine = true,
                    modifier = Modifier.width(80.dp),
                )
                Spacer(Modifier.padding(horizontal = 6.dp))
                AssistChip(onClick = {
                    value = ""
                    vm.setCellManual(r, c, null)
                }, label = { Text("Vider") }, leadingIcon = { Icon(Icons.Filled.Delete, null) })
            }
        }

        Row {
            Button(onClick = { vm.clearBoard() }) { Text("Vider le plateau") }
        }
    }
}

@Composable
private fun BoardCanvas(
    board: com.scrabbl.ai.engine.Board,
    selected: Pair<Int, Int>?,
    onCell: (Int, Int) -> Unit,
) {
    val n = board.size
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .pointerInput(n) {
                detectTapGestures { pos ->
                    val tile = size.width / n
                    val c = (pos.x / tile).toInt().coerceIn(0, n - 1)
                    val r = (pos.y / tile).toInt().coerceIn(0, n - 1)
                    onCell(r, c)
                }
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
            drawRect(color = cellColor, topLeft = androidx.compose.ui.geometry.Offset(x, y),
                size = androidx.compose.ui.geometry.Size(tile, tile))
            drawRect(color = Color(0x33000000),
                topLeft = androidx.compose.ui.geometry.Offset(x, y),
                size = androidx.compose.ui.geometry.Size(tile, tile),
                style = Stroke(width = 1f))
            val letter = board.letters[r][c]
            if (letter != 0) {
                drawRect(color = Color(0xFFF1D9A0),
                    topLeft = androidx.compose.ui.geometry.Offset(x + 2, y + 2),
                    size = androidx.compose.ui.geometry.Size(tile - 4, tile - 4))
                val nc = drawContext.canvas.nativeCanvas
                val paint = android.graphics.Paint().apply {
                    color = 0xFF3B2A16.toInt()
                    textSize = tile * 0.55f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    isFakeBoldText = true
                }
                nc.drawText(
                    FrenchScrabble.ch(letter).toString(),
                    x + tile / 2f,
                    y + tile / 2f + tile * 0.2f,
                    paint,
                )
            }
            if (selected == r to c) {
                drawRect(color = Color(0xAA34C759),
                    topLeft = androidx.compose.ui.geometry.Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(tile, tile),
                    style = Stroke(width = 3f))
            }
        }
    }
}
