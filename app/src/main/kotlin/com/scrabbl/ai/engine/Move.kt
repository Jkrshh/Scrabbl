package com.scrabbl.ai.engine

/** Direction d'un mot posé. */
enum class Direction { HORIZONTAL, VERTICAL }

/** Une lettre posée à une case précise. */
data class Placement(
    val row: Int,
    val col: Int,
    /** code 1..26 (A..Z) — la lettre effectivement placée. */
    val letter: Int,
    /** true si c'est un joker (rapporte 0 pt mais tient lieu de la lettre). */
    val isBlank: Boolean,
)

/**
 * Un coup complet : le mot principal joué, son score, l'ensemble des lettres
 * posées (les autres appartenaient déjà au plateau) et les lettres du rack
 * consommées (utile pour l'affichage / le rack restant).
 */
data class Move(
    val word: String,
    val row: Int,
    val col: Int,
    val direction: Direction,
    val score: Int,
    val placements: List<Placement>,
    val rackTilesUsed: List<Int>,
) : Comparable<Move> {
    override fun compareTo(other: Move): Int = other.score.compareTo(this.score)

    /** Coordonnée de fin (dernière case du mot principal). */
    val endRow: Int
        get() = if (direction == Direction.HORIZONTAL) row else row + word.length - 1
    val endCol: Int
        get() = if (direction == Direction.HORIZONTAL) col + word.length - 1 else col

    fun humanCoord(size: Int): String {
        // Standard Scrabble notation: 8H for row 8 column H if horizontal, H8 if vertical
        val colLetter = ('A' + col).toString()
        val rowNumber = (row + 1).toString()
        return if (direction == Direction.HORIZONTAL) "$rowNumber$colLetter" else "$colLetter$rowNumber"
    }
}
