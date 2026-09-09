package com.scrabbl.ai.engine

/**
 * Calcul du score d'un coup, en respectant les règles ODS :
 *   - Les multiplicateurs de lettre s'appliquent seulement aux tuiles
 *     nouvellement posées.
 *   - Les multiplicateurs de mot s'appliquent au mot principal ET à
 *     chaque mot perpendiculaire créé, mais seulement si une tuile
 *     nouvellement posée est sur la case avec bonus de mot.
 *   - Un joker vaut 0 point.
 *   - Poser les 7 tuiles du chevalet ajoute 50 points (« scrabble »).
 */
object Scoring {

    fun score(
        board: Board,
        row: Int,
        startCol: Int,
        word: String,
        placements: List<Placement>,
    ): Int {
        // On considère uniquement la direction horizontale ici — l'appelant
        // transpose au préalable pour la verticale.
        val placedByCol: Map<Int, Placement> = placements.associateBy { it.col }
        val premium = board.premium

        var horizontal = 0
        var wordMul = 1
        var crossSum = 0

        for (i in word.indices) {
            val r = row
            val c = startCol + i
            val letter = FrenchScrabble.l(word[i])
            val placement = placedByCol[c]
            val isNew = placement != null
            val effectiveLetter = if (isNew) placement!!.letter else letter
            val isBlank = if (isNew) placement!!.isBlank else board.blanks[r][c]
            val baseVal = if (isBlank) 0 else FrenchScrabble.value[effectiveLetter]
            val prem = if (isNew) premium[r][c] else Premium.NORMAL
            val lm = prem.letterMul
            val wm = prem.wordMul

            horizontal += baseVal * lm
            wordMul *= wm

            if (isNew) {
                // Calcule le mot perpendiculaire à (r, c)
                val cross = crossScore(board, r, c, effectiveLetter, baseVal * lm, wm)
                crossSum += cross
            }
        }
        val mainWord = horizontal * wordMul
        val bingo = if (placements.size == FrenchScrabble.RACK_SIZE) FrenchScrabble.BINGO_BONUS else 0
        return mainWord + crossSum + bingo
    }

    /**
     * Calcule le score du mot perpendiculaire créé en posant la nouvelle tuile
     * à (r, c). Retourne 0 s'il n'y a pas de mot perpendiculaire (aucune tuile voisine).
     */
    private fun crossScore(
        board: Board,
        r: Int,
        c: Int,
        newLetter: Int,
        newLetterContribution: Int,
        wordMul: Int,
    ): Int {
        // Cherche les tuiles au dessus / en dessous
        var top = r - 1
        while (top >= 0 && !board.isEmpty(top, c)) top--
        val startRow = top + 1
        var bot = r + 1
        while (bot < board.size && !board.isEmpty(bot, c)) bot++
        val endRow = bot - 1
        if (startRow == endRow) return 0 // pas de voisin vertical => pas de mot perp

        var sum = 0
        for (rr in startRow..endRow) {
            if (rr == r) {
                sum += newLetterContribution
            } else {
                val v = if (board.blanks[rr][c]) 0 else FrenchScrabble.value[board.letters[rr][c]]
                sum += v
            }
        }
        return sum * wordMul
    }
}
