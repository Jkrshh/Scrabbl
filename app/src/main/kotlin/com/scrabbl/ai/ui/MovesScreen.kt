package com.scrabbl.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scrabbl.ai.MainViewModel
import com.scrabbl.ai.R
import com.scrabbl.ai.engine.Move

@Composable
fun MovesScreen(vm: MainViewModel) {
    val moves by vm.moves.collectAsState()
    val computing by vm.computing.collectAsState()
    val board by vm.board.collectAsState()
    val rack by vm.rack.collectAsState()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.board_size, board.size),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text("Rack : ${if (rack.isEmpty()) "(vide)" else rack}",
                    style = MaterialTheme.typography.bodyMedium)
            }
            if (computing) CircularProgressIndicator(strokeWidth = 2.dp)
        }
        HorizontalDivider()
        Box(Modifier.fillMaxSize()) {
            if (moves.isEmpty() && !computing) {
                Text(
                    if (rack.isEmpty()) stringResource(R.string.empty_rack)
                    else stringResource(R.string.no_moves),
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(8.dp)) {
                    items(moves) { m -> MoveRow(m, board.size) }
                }
            }
        }
    }
}

@Composable
private fun MoveRow(m: Move, boardSize: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        color = Color(0xFFFFF8EC),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${m.score}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.width(52.dp),
                color = Color(0xFFC85A3E),
            )
            Column(Modifier.weight(1f)) {
                Text(m.word, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${m.humanCoord(boardSize)} · ${m.direction.name.lowercase()} · ${m.placements.size} tuile(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray,
                )
            }
        }
    }
}
