package com.scrabbl.ai.vision

import android.graphics.RectF
import com.scrabbl.ai.engine.Board
import com.scrabbl.ai.engine.BoardType
import com.scrabbl.ai.engine.FrenchScrabble
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * À partir de lettres reconnues (positions + valeur), déduit :
 *   - la taille du plateau (11, 13, 15 ou 21),
 *   - la position/taille de chaque case,
 *   - le contenu du plateau (matrice de lettres).
 */
class BoardDetector {

    data class Detection(
        val board: Board,
        val bounds: RectF,
        val tileSize: Float,
        val boardType: BoardType,
        val filledCount: Int,
        val confidence: Float,
    )

    fun detect(letters: List<RecognizedLetter>): Detection? {
        if (letters.size < MIN_TILES) return null

        val widths = letters.map { it.box.width().toFloat() }.sorted()
        val tileEstimate = widths[widths.size / 2]
        if (tileEstimate < 12f) return null

        val tolerance = tileEstimate * 0.45f
        val kept = letters.filter { rl ->
            val w = rl.box.width().toFloat()
            val h = rl.box.height().toFloat()
            w in (tileEstimate - tolerance)..(tileEstimate + tolerance) &&
                h in (tileEstimate * 0.55f)..(tileEstimate * 1.55f)
        }
        if (kept.size < MIN_TILES) return null

        val centers = kept.map { it.box.exactCenterX() to it.box.exactCenterY() }
        val xs = centers.map { it.first }.sorted()
        val ys = centers.map { it.second }.sorted()

        val colGrid = inferGrid(xs, tileEstimate) ?: return null
        val rowGrid = inferGrid(ys, tileEstimate) ?: return null

        val step = ((colGrid.step + rowGrid.step) / 2f)
        if (step < 4f) return null
        val gridRegularity = minOf(colGrid.regularity, rowGrid.regularity)
        if (gridRegularity < 0.55f) return null

        val estN = maxOf(colGrid.count, rowGrid.count)
        val boardType = matchBoardType(estN) ?: return null
        val N = boardType.size

        val originX = colGrid.min - step * ((N - colGrid.count) / 2f) - step / 2f
        val originY = rowGrid.min - step * ((N - rowGrid.count) / 2f) - step / 2f

        val board = Board(boardType)
        val filled = HashMap<Pair<Int, Int>, Pair<Char, Float>>()
        for (rl in kept) {
            val cx = rl.box.exactCenterX()
            val cy = rl.box.exactCenterY()
            val col = ((cx - originX) / step).roundToInt().coerceIn(0, N - 1)
            val row = ((cy - originY) / step).roundToInt().coerceIn(0, N - 1)
            val existing = filled[row to col]
            if (existing == null || existing.second < rl.confidence) {
                filled[row to col] = rl.letter to rl.confidence
            }
        }
        for ((rc, chConf) in filled) {
            board.letters[rc.first][rc.second] = FrenchScrabble.l(chConf.first)
        }

        val bounds = RectF(originX, originY, originX + step * N, originY + step * N)
        val coverage = kept.size.toFloat() / (letters.size.coerceAtLeast(1)).toFloat()
        val confidence = ((coverage * 0.5f) + (gridRegularity * 0.5f)).coerceIn(0f, 1f)

        return Detection(
            board = board,
            bounds = bounds,
            tileSize = step,
            boardType = boardType,
            filledCount = filled.size,
            confidence = confidence,
        )
    }

    private fun inferGrid(sortedVals: List<Float>, tileSize: Float): Grid1D? {
        if (sortedVals.isEmpty()) return null
        val gapThreshold = tileSize * 0.5f
        val clusters = ArrayList<MutableList<Float>>()
        var current = mutableListOf(sortedVals[0])
        for (i in 1 until sortedVals.size) {
            if (sortedVals[i] - sortedVals[i - 1] > gapThreshold) {
                clusters.add(current); current = mutableListOf(sortedVals[i])
            } else current.add(sortedVals[i])
        }
        clusters.add(current)
        if (clusters.size < 3) return null

        val centers = clusters.map { it.average().toFloat() }
        val diffs = centers.zipWithNext { a, b -> b - a }
        val meanStep = diffs.average().toFloat()
        if (meanStep < tileSize * 0.5f) return null

        val variance = diffs.map { (it - meanStep) * (it - meanStep) }.average()
        val stddev = kotlin.math.sqrt(variance).toFloat()
        val regularity = (1f - stddev / meanStep).coerceIn(0f, 1f)

        return Grid1D(
            count = centers.size,
            min = centers.first(),
            step = meanStep,
            max = centers.last(),
            regularity = regularity,
        )
    }

    private fun matchBoardType(n: Int): BoardType? {
        val candidates = BoardType.values().toList()
        val best = candidates.minByOrNull { abs(it.size - n) } ?: return null
        return if (abs(best.size - n) <= 2) best else null
    }

    private data class Grid1D(
        val count: Int,
        val min: Float,
        val step: Float,
        val max: Float,
        val regularity: Float,
    )

    companion object {
        private const val MIN_TILES = 6
    }
}
