package com.scrabbl.ai.vision

import android.graphics.RectF
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.scrabbl.ai.engine.Board
import com.scrabbl.ai.engine.BoardType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ImageAnalysis.Analyzer qui, pour chaque frame :
 *   1. Envoie l'image à ML Kit pour reconnaître les lettres
 *   2. Passe le résultat au [BoardDetector] pour déduire le plateau
 *   3. Publie la détection stabilisée via [state]
 *
 * On applique un lissage temporel simple : une case n'est considérée « occupée »
 * qu'après avoir été vue N fois de suite avec la même lettre. Cela évite le
 * flicker sur du texte parasite (dictionnaire, annotations).
 */
class BoardAnalyzer(
    private val scope: CoroutineScope,
    private val onDetection: (BoardDetector.Detection) -> Unit,
) : ImageAnalysis.Analyzer {

    private val recognizer = LetterRecognizer()
    private val detector = BoardDetector()
    private val busy = AtomicBoolean(false)

    private val _state = MutableStateFlow<VisionState>(VisionState.Searching)
    val state: StateFlow<VisionState> = _state.asStateFlow()

    // Historique pour stabilisation (comptes par case/lettre)
    private var lastBoardType: BoardType? = null
    private val hits: HashMap<Triple<Int, Int, Char>, Int> = HashMap()

    override fun analyze(proxy: ImageProxy) {
        if (!busy.compareAndSet(false, true)) { proxy.close(); return }
        val input = imageProxyToInputImage(proxy)
        if (input == null) { proxy.close(); busy.set(false); return }

        scope.launch(Dispatchers.Default) {
            try {
                val letters = recognizer.recognize(input)
                val det = detector.detect(letters)
                if (det != null) {
                    val stabilized = stabilize(det)
                    _state.value = VisionState.Locked(stabilized)
                    onDetection(stabilized)
                } else {
                    _state.value = VisionState.Searching
                }
            } finally {
                proxy.close()
                busy.set(false)
            }
        }
    }

    /** Fait converger les hits successifs vers un plateau stable. */
    private fun stabilize(det: BoardDetector.Detection): BoardDetector.Detection {
        if (det.boardType != lastBoardType) {
            hits.clear()
            lastBoardType = det.boardType
        }
        val size = det.board.size
        // Compte les nouvelles observations
        for (r in 0 until size) for (c in 0 until size) {
            val v = det.board.letters[r][c]
            if (v != 0) {
                val ch = com.scrabbl.ai.engine.FrenchScrabble.ch(v)
                val key = Triple(r, c, ch)
                hits[key] = (hits[key] ?: 0) + 1
                // décrémente les concurrents à la même case
                val toDecay = hits.keys.filter { it.first == r && it.second == c && it.third != ch }
                for (k in toDecay) {
                    val nv = (hits[k] ?: 0) - 1
                    if (nv <= 0) hits.remove(k) else hits[k] = nv
                }
            }
        }
        val stable = Board(det.boardType)
        for ((key, count) in hits) {
            if (count >= 2) {
                stable.letters[key.first][key.second] = com.scrabbl.ai.engine.FrenchScrabble.l(key.third)
            }
        }
        return det.copy(board = stable, filledCount = countFilled(stable))
    }

    private fun countFilled(b: Board): Int {
        var n = 0
        for (r in 0 until b.size) for (c in 0 until b.size) if (b.letters[r][c] != 0) n++
        return n
    }

    fun close() = recognizer.close()
}

sealed class VisionState {
    object Searching : VisionState()
    data class Locked(val detection: BoardDetector.Detection) : VisionState()
}
