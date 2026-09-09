package com.scrabbl.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Clavier virtuel pour poser des lettres sur le plateau.
 *
 * Rangées AZERTY classiques + une rangée de contrôle (⌫, direction, joker).
 */
@Composable
fun LetterKeypad(
    onLetter: (Char) -> Unit,
    onBackspace: () -> Unit,
    onBlank: () -> Unit,
) {
    val rows = listOf(
        "AZERTYUIOP",
        "QSDFGHJKLM",
        "WXCVBN",
    )
    Column(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (row in rows) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (ch in row) {
                    Key(ch.toString(), Modifier.weight(1f)) { onLetter(ch) }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Key("★", Modifier.weight(1.5f), Color(0xFFFFDCB0)) { onBlank() }
            Key("⌫", Modifier.weight(2f), Color(0xFFF3B9B9)) { onBackspace() }
        }
    }
}

@Composable
private fun Key(
    label: String,
    modifier: Modifier = Modifier,
    bg: Color = Color(0xFFF1D9A0),
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .aspectRatio(1.4f)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF3B2A16),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
