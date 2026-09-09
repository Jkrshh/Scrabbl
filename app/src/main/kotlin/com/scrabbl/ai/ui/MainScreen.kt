package com.scrabbl.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.scrabbl.ai.MainViewModel
import com.scrabbl.ai.R
import com.scrabbl.ai.dict.DictState

@Composable
fun MainScreen(vm: MainViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val dictState by vm.dictState.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.CameraAlt, null) },
                    label = { Text(stringResource(R.string.tab_camera)) },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.GridOn, null) },
                    label = { Text(stringResource(R.string.tab_board)) },
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.List, null) },
                    label = { Text(stringResource(R.string.tab_moves)) },
                )
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            DictBanner(dictState, onRetry = vm::retryDictionaryDownload)
            HorizontalDivider()
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                when (tab) {
                    0 -> CameraScreen(vm)
                    1 -> BoardEditorScreen(vm)
                    else -> MovesScreen(vm)
                }
            }
        }
    }
}

@Composable
private fun DictBanner(state: DictState, onRetry: () -> Unit) {
    val (label, showSpinner, showRetry) = when (state) {
        is DictState.Idle -> Triple("…", true, false)
        is DictState.LoadingStarter -> Triple("Chargement dictionnaire…", true, false)
        is DictState.StarterReady -> Triple(
            "ODS starter : ${state.words} mots — téléchargement du complet…", true, false,
        )
        is DictState.Downloading -> Triple("Téléchargement ODS ${state.percent} %…", true, false)
        is DictState.FullReady -> Triple(
            "Dictionnaire ODS français : ${state.words} mots", false, false,
        )
        is DictState.Failed -> Triple(
            "Dictionnaire de base uniquement — appuie sur ↻ pour réessayer", false, true,
        )
    }
    Row(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showSpinner) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.height(16.dp))
            Spacer(Modifier.padding(horizontal = 6.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        if (showRetry) {
            IconButton(onClick = onRetry) {
                Icon(Icons.Filled.Refresh, contentDescription = "Réessayer")
            }
        }
    }
}
