package com.scrabbl.ai.engine

/**
 * Un plateau de Scrabble.
 *   - `letters[r][c]`  : 0 si case vide, sinon code 1..26 (A..Z).
 *   - `blanks[r][c]`   : true si la lettre placée est un joker (compte 0 point).
 *   - `premium[r][c]`  : type de case bonus — initialisé depuis [type] mais
 *                       éditable par l'utilisateur (chaque plateau détient son
 *                       propre tableau, pas de partage entre instances).
 */
class Board(val type: BoardType) {
    val size: Int = type.size
    val premium: Array<Array<Premium>> = run {
        val src = type.premiumGrid()
        Array(size) { r -> Array(size) { c -> src[r][c] } }
    }
    val letters: Array<IntArray> = Array(size) { IntArray(size) }
    val blanks: Array<BooleanArray> = Array(size) { BooleanArray(size) }

    fun isEmpty(r: Int, c: Int): Boolean = inBounds(r, c) && letters[r][c] == 0
    fun letterAt(r: Int, c: Int): Int = if (inBounds(r, c)) letters[r][c] else 0
    fun inBounds(r: Int, c: Int): Boolean = r in 0 until size && c in 0 until size

    fun isBoardEmpty(): Boolean {
        for (r in 0 until size) for (c in 0 until size) if (letters[r][c] != 0) return false
        return true
    }

    fun place(move: Move) {
        for (p in move.placements) {
            letters[p.row][p.col] = p.letter
            blanks[p.row][p.col] = p.isBlank
        }
    }

    fun setPremium(r: Int, c: Int, p: Premium) {
        if (inBounds(r, c)) premium[r][c] = p
    }

    fun copy(): Board {
        val b = Board(type)
        for (r in 0 until size) {
            letters[r].copyInto(b.letters[r])
            blanks[r].copyInto(b.blanks[r])
            for (c in 0 until size) b.premium[r][c] = premium[r][c]
        }
        return b
    }
}
