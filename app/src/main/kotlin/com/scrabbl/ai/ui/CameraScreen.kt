package com.scrabbl.ai.ui

import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.scrabbl.ai.MainViewModel
import com.scrabbl.ai.R
import com.scrabbl.ai.vision.BoardAnalyzer
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(vm: MainViewModel) {
    val camPerm = rememberPermissionState(android.Manifest.permission.CAMERA)
    if (!camPerm.status.isGranted) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("Cadrez le plateau — nous avons besoin de la caméra.")
            Spacer(Modifier.padding(vertical = 8.dp))
            Button(onClick = { camPerm.launchPermissionRequest() }) { Text("Autoriser la caméra") }
        }
        return
    }
    CameraContent(vm)
}

@Composable
private fun CameraContent(vm: MainViewModel) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    val visionScope = remember {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)
    }
    val analyzer = remember {
        BoardAnalyzer(scope = visionScope, onDetection = { det -> vm.onDetection(det) })
    }
    DisposableEffect(Unit) {
        onDispose {
            analyzer.close()
            visionScope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
            executor.shutdown()
        }
    }

    val moves by vm.moves.collectAsState()
    val bestMove = moves.firstOrNull()
    val boardLocked by vm.boardLocked.collectAsState()
    val rack by vm.rack.collectAsState()
    val det by vm.lastDetection.collectAsState()

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().apply {
                        setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setResolutionSelector(
                            ResolutionSelector.Builder()
                                .setResolutionStrategy(
                                    ResolutionStrategy(Size(1280, 720),
                                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER)
                                )
                                .build()
                        )
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .apply { setAnalyzer(executor, analyzer) }

                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycle, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis,
                    )
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Overlay du meilleur coup dessiné sur le plateau détecté
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val d = det ?: return@Canvas
            val tile = d.tileSize
            if (tile <= 0f) return@Canvas
            // Cadre du plateau détecté
            val stroke = Stroke(width = 3f)
            val r = d.bounds
            drawRoundRect(
                color = Color(0xFF34C759),
                topLeft = androidx.compose.ui.geometry.Offset(r.left, r.top),
                size = androidx.compose.ui.geometry.Size(r.width(), r.height()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
                style = stroke,
                alpha = 0.65f,
            )
            // Surligne le meilleur coup
            bestMove?.let { move ->
                for (p in move.placements) {
                    val x = r.left + tile * p.col
                    val y = r.top + tile * p.row
                    drawRoundRect(
                        color = Color(0xE6FFC947),
                        topLeft = androidx.compose.ui.geometry.Offset(x + 4f, y + 4f),
                        size = androidx.compose.ui.geometry.Size(tile - 8f, tile - 8f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
                    )
                }
            }
        }

        // HUD haut : infos plateau/detection
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xAA000000),
                contentColor = Color.White,
            ) {
                Column(Modifier.padding(10.dp)) {
                    Text(
                        det?.let {
                            "${it.boardType.label} · ${it.filledCount} tuiles · confiance ${(it.confidence * 100).toInt()} %"
                        } ?: stringResource(R.string.scan_hint),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    bestMove?.let {
                        Text(
                            "★ ${it.word} · ${it.humanCoord(det?.board?.size ?: 15)} · ${it.score} pts",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }

        // HUD bas : rack + verrou
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RackInput(rack = rack, onRackChange = vm::setRack)
            Row(
                Modifier.fillMaxWidth().background(Color(0xAA000000), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Figer le plateau", color = Color.White)
                Spacer(Modifier.width(8.dp))
                Switch(checked = boardLocked, onCheckedChange = vm::lockBoard)
                Spacer(Modifier.width(12.dp))
                FilledTonalButton(onClick = { vm.clearBoard() }) { Text("Vider") }
            }
        }
    }
}
