package com.scrabbl.ai.engine

/**
 * Un plateau de Scrabble.
 * `letters[r][c]`  : 0 si case vide, sinon code 1..26 (A..Z).
 * `blanks[r][c]`   : true si la lettre placée est un joker (compte 0 point).
 *
 * Les grilles de primes sont fournies par [type].
 */
class Board(val type: BoardType) {
    val size: Int = type.size
    val premium: Array<Array<Premium>> = type.premiumGrid()
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

    fun copy(): Board {
        val b = Board(type)
        for (r in 0 until size) {
            letters[r].copyInto(b.letters[r])
            blanks[r].copyInto(b.blanks[r])
        }
        return b
    }
}
