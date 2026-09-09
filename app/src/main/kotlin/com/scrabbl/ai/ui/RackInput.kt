package com.scrabbl.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.scrabbl.ai.R

/** Zone de saisie du chevalet — accepte A..Z et « ? » pour un joker. */
@Composable
fun RackInput(rack: String, onRackChange: (String) -> Unit) {
    Column(
        Modifier.background(Color(0xAA000000), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("Votre chevalet", color = Color.White, style = MaterialTheme.typography.labelMedium)
        OutlinedTextField(
            value = rack,
            onValueChange = onRackChange,
            singleLine = true,
            placeholder = { Text(stringResource(R.string.rack_hint), color = Color.Gray) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier,
        )
        Row {
            for (c in rack) TileMini(c)
        }
    }
}

@Composable
private fun TileMini(c: Char) {
    Text(
        text = if (c == '?') "★" else c.toString(),
        color = Color(0xFF3B2A16),
        modifier = Modifier
            .padding(end = 4.dp)
            .background(Color(0xFFF1D9A0), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        style = MaterialTheme.typography.titleMedium,
    )
}
