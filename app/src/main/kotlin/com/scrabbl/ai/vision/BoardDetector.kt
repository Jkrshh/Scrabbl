package com.scrabbl.ai.vision

import android.graphics.Rect
import android.graphics.RectF
import com.scrabbl.ai.engine.Board
import com.scrabbl.ai.engine.BoardType
import com.scrabbl.ai.engine.FrenchScrabble
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * À partir de lettres reconnues (positions + valeur), déduit :
 *   - la taille du plateau (11, 13, 15, 17 ou 21),
 *   - la position/taille de chaque case,
 *   - le contenu du plateau (matrice de lettres).
 *
 * L'algo :
 *   1. Filtre les lettres qui ne sont pas des tuiles (petites annotations,
 *      textes de dictionnaire etc.) en gardant celles qui partagent une
 *      taille de boîte cohérente.
 *   2. Regroupe les positions X (colonnes) et Y (lignes) par clustering
 *      1D avec seuil = 0.6 * taille tuile.
 *   3. Déduit un plateau carré N×N où N est le nombre de clusters (arrondi
 *      à la taille standard la plus proche parmi les [BoardType]).
 *   4. Chaque case est remplie avec la lettre dominante localisée à
 *      son intersection.
 */
class BoardDetector {

    data class Detection(
        val board: Board,
        val bounds: RectF,      // englobant du plateau dans l'image
        val tileSize: Float,    // en pixels image
        val boardType: BoardType,
        val filledCount: Int,   // nb de lettres placées
        val confidence: Float,  // 0..1
    )

    fun detect(letters: List<RecognizedLetter>): Detection? {
        if (letters.size < 4) return null

        // 1) Estime la taille de tuile prédominante (largeur moyenne des boîtes).
        val widths = letters.map { it.box.width().toFloat() }.sorted()
        val tileEstimate = widths[widths.size / 2]  // médiane
        val tolerance = tileEstimate * 0.55f

        // On ne garde que les lettres dont la boîte est proche de la taille dominante.
        val kept = letters.filter { rl ->
            val w = rl.box.width().toFloat()
            val h = rl.box.height().toFloat()
            w in (tileEstimate - tolerance)..(tileEstimate + tolerance) &&
            h in (tileEstimate * 0.6f)..(tileEstimate * 1.6f)
        }
        if (kept.size < 4) return null

        // 2) Cluster 1D des positions X (centres) puis Y.
        val centers = kept.map { it.box.exactCenterX() to it.box.exactCenterY() }
        val xs = centers.map { it.first }.sorted()
        val ys = centers.map { it.second }.sorted()

        val colGrid = inferGrid(xs, tileEstimate)
        val rowGrid = inferGrid(ys, tileEstimate)

        // 3) Choix d'un plateau carré. On prend le max des deux comptages pour
        //    couvrir les cases vides autour, arrondi à un standard.
        val estN = maxOf(colGrid.count, rowGrid.count, 11)
        val boardType = BoardType.forSize(estN)
        val N = boardType.size

        // Recompose une grille équidistante à partir des min/max observés :
        val minX = colGrid.min
        val minY = rowGrid.min
        val step = ((colGrid.step + rowGrid.step) / 2f).let { if (it < 1f) tileEstimate else it }
        val originX = minX - step * ((N - colGrid.count) / 2f) - step / 2f
        val originY = minY - step * ((N - rowGrid.count) / 2f) - step / 2f

        val board = Board(boardType)
        val filled = HashMap<Pair<Int, Int>, Char>()
        for (rl in kept) {
            val cx = rl.box.exactCenterX()
            val cy = rl.box.exactCenterY()
            val col = ((cx - originX) / step).roundToInt().coerceIn(0, N - 1)
            val row = ((cy - originY) / step).roundToInt().coerceIn(0, N - 1)
            filled[row to col] = rl.letter
        }
        for ((rc, ch) in filled) {
            board.letters[rc.first][rc.second] = FrenchScrabble.l(ch)
        }

        val bounds = RectF(originX, originY, originX + step * N, originY + step * N)
        val confidence = (kept.size.toFloat() / (letters.size + 4f)).coerceIn(0f, 1f)

        return Detection(
            board = board,
            bounds = bounds,
            tileSize = step,
            boardType = boardType,
            filledCount = filled.size,
            confidence = confidence,
        )
    }

    /**
     * Un cluster 1D grossier : trie les valeurs, calcule les gaps, si un gap
     * dépasse [gapThreshold] on démarre un nouveau cluster. La distance modale
     * entre deux clusters donne [step].
     */
    private fun inferGrid(sortedVals: List<Float>, tileSize: Float): Grid1D {
        if (sortedVals.isEmpty()) return Grid1D(0, 0f, tileSize, 0f)
        val gapThreshold = tileSize * 0.5f
        val clusters = ArrayList<MutableList<Float>>()
        var current = mutableListOf(sortedVals[0])
        for (i in 1 until sortedVals.size) {
            if (sortedVals[i] - sortedVals[i - 1] > gapThreshold) {
                clusters.add(current); current = mutableListOf(sortedVals[i])
            } else current.add(sortedVals[i])
        }
        clusters.add(current)

        val centers = clusters.map { it.average().toFloat() }
        val diffs = centers.zipWithNext { a, b -> b - a }
        val step = if (diffs.isEmpty()) tileSize else diffs.average().toFloat().coerceAtLeast(tileSize)
        return Grid1D(count = centers.size, min = centers.first(), step = step, max = centers.last())
    }

    private data class Grid1D(val count: Int, val min: Float, val step: Float, val max: Float)
}
