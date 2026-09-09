package com.scrabbl.ai.engine

/**
 * Générateur de coups pour le Scrabble français, style Appel-Jacobson.
 *
 * Pour chaque direction (horizontale, puis verticale via transposition) :
 *   1. Précalcule les « cross-checks » : à chaque case vide, quelles lettres
 *      forment un mot perpendiculaire valide (ou toutes les lettres si aucun voisin).
 *   2. Enumère les positions de départ possibles pour un mot.
 *   3. Étend récursivement en piochant dans le rack, en s'appuyant sur
 *      [Dictionary.hasPrefix] pour élaguer.
 *
 * La performance en pratique sur mobile : quelques dizaines de ms pour un plateau 15x15.
 */
class MoveGenerator(private val dict: Dictionary) {

    fun generate(board: Board, rack: IntArray, topK: Int = 200): List<Move> {
        val all = ArrayList<Move>(1024)
        // Horizontal
        solveOnOrientation(board, rack, transposed = false, out = all)
        // Vertical — via transposition
        solveOnOrientation(board.transposed(), rack, transposed = true, out = all)
        // Dédoublonnage : un mot d'une seule lettre placée peut apparaître dans les deux orientations
        val seen = HashSet<String>()
        val uniq = ArrayList<Move>(all.size)
        for (m in all) {
            val key = "${m.word}#${m.row}#${m.col}#${m.direction}"
            if (seen.add(key)) uniq.add(m)
        }
        uniq.sort()
        return if (uniq.size > topK) uniq.subList(0, topK).toList() else uniq
    }

    private fun solveOnOrientation(
        board: Board,
        rack: IntArray,
        transposed: Boolean,
        out: MutableList<Move>,
    ) {
        val size = board.size
        val cc = computeCrossChecks(board)   // vertical cross-words
        val rackCounts = IntArray(27).also { for (t in rack) if (t in 0..26) it[t]++ }
        val emptyBoard = board.isBoardEmpty()

        for (r in 0 until size) {
            for (startC in 0 until size) {
                // Le mot ne peut pas commencer au milieu d'un mot existant
                if (startC > 0 && !board.isEmpty(r, startC - 1)) continue
                val sb = StringBuilder(size)
                val placements = ArrayList<Placement>(FrenchScrabble.RACK_SIZE)
                extend(
                    board, r, startC, sb, placements, rackCounts, cc,
                    out, transposed, emptyBoard,
                )
            }
        }
    }

    private fun extend(
        board: Board,
        r: Int,
        startC: Int,
        sb: StringBuilder,
        placements: ArrayList<Placement>,
        rack: IntArray,
        cc: Array<Array<BooleanArray>>,
        out: MutableList<Move>,
        transposed: Boolean,
        emptyBoard: Boolean,
    ) {
        val c = startC + sb.length
        val prefix = sb.toString()
        if (prefix.isNotEmpty() && !dict.hasPrefix(prefix)) return

        // Émission éventuelle : mot valide, terminant à cette position
        val nextEmpty = (c >= board.size) || board.isEmpty(r, c)
        if (nextEmpty && placements.isNotEmpty() && prefix.length >= 2 && dict.contains(prefix)) {
            val connects = if (emptyBoard) {
                val cr = board.size / 2
                placements.any { it.row == cr && it.col == cr }
            } else {
                // Au moins une lettre existante du plateau est traversée
                // OU une des placements a un voisin filled (fourni via anchors calc)
                touchesBoard(board, r, startC, prefix.length, placements)
            }
            if (connects) {
                val move = buildMove(board, r, startC, prefix, placements, transposed)
                out.add(move)
            }
        }

        if (c >= board.size) return

        // Case occupée : on prolonge avec la lettre du plateau (aucune tuile consommée)
        if (!board.isEmpty(r, c)) {
            val letter = board.letters[r][c]
            sb.append(FrenchScrabble.ch(letter))
            extend(board, r, startC, sb, placements, rack, cc, out, transposed, emptyBoard)
            sb.deleteCharAt(sb.length - 1)
            return
        }

        // Case vide : on peut poser
        //   1) une lettre du rack respectant le cross-check,
        //   2) ou un joker → toute lettre respectant le cross-check.
        for (letter in 1..26) {
            if (!cc[r][c][letter]) continue
            if (rack[letter] > 0) {
                rack[letter]--
                sb.append(FrenchScrabble.ch(letter))
                placements.add(Placement(r, c, letter, isBlank = false))
                extend(board, r, startC, sb, placements, rack, cc, out, transposed, emptyBoard)
                placements.removeAt(placements.lastIndex)
                sb.deleteCharAt(sb.length - 1)
                rack[letter]++
            }
        }
        if (rack[0] > 0) {
            for (letter in 1..26) {
                if (!cc[r][c][letter]) continue
                rack[0]--
                sb.append(FrenchScrabble.ch(letter))
                placements.add(Placement(r, c, letter, isBlank = true))
                extend(board, r, startC, sb, placements, rack, cc, out, transposed, emptyBoard)
                placements.removeAt(placements.lastIndex)
                sb.deleteCharAt(sb.length - 1)
                rack[0]++
            }
        }
    }

    /** Le mot forme-t-il une jonction valide avec des tuiles existantes ? */
    private fun touchesBoard(
        board: Board,
        r: Int,
        startC: Int,
        wordLen: Int,
        placements: List<Placement>,
    ): Boolean {
        // Si une lettre du mot est déjà présente sur le plateau, on traverse.
        for (c in startC until startC + wordLen) {
            if (!board.isEmpty(r, c) && placements.none { it.col == c && it.row == r }) return true
        }
        // Sinon vérifier voisinage vertical de chaque nouvelle placement
        for (p in placements) {
            if (p.row - 1 >= 0 && !board.isEmpty(p.row - 1, p.col)) return true
            if (p.row + 1 < board.size && !board.isEmpty(p.row + 1, p.col)) return true
        }
        return false
    }

    private fun buildMove(
        board: Board,
        r: Int,
        c: Int,
        word: String,
        placements: List<Placement>,
        transposed: Boolean,
    ): Move {
        val score = Scoring.score(board, r, c, word, placements)
        val rackUsed = placements.map { if (it.isBlank) 0 else it.letter }
        val move = Move(
            word = word,
            row = r,
            col = c,
            direction = Direction.HORIZONTAL,
            score = score,
            placements = placements.toList(),
            rackTilesUsed = rackUsed,
        )
        return if (transposed) move.transposedFromTransposed() else move
    }

    // --------------------------------------------------------------
    // Cross-check : pour chaque case vide, quelles lettres sont OK
    // en tenant compte du mot perpendiculaire (colonne).
    // --------------------------------------------------------------
    private fun computeCrossChecks(board: Board): Array<Array<BooleanArray>> {
        val size = board.size
        val cc = Array(size) { Array(size) { BooleanArray(27) } }
        for (r in 0 until size) for (c in 0 until size) {
            if (!board.isEmpty(r, c)) continue
            // Cherche la fin du bloc au dessus
            var top = r - 1
            val pre = StringBuilder()
            while (top >= 0 && !board.isEmpty(top, c)) {
                pre.insert(0, FrenchScrabble.ch(board.letters[top][c])); top--
            }
            var bot = r + 1
            val post = StringBuilder()
            while (bot < size && !board.isEmpty(bot, c)) {
                post.append(FrenchScrabble.ch(board.letters[bot][c])); bot++
            }
            if (pre.isEmpty() && post.isEmpty()) {
                for (l in 1..26) cc[r][c][l] = true
            } else {
                for (l in 1..26) {
                    val w = pre.toString() + FrenchScrabble.ch(l) + post.toString()
                    if (dict.contains(w)) cc[r][c][l] = true
                }
            }
        }
        return cc
    }
}

/** Retourne le plateau transposé (lignes ↔ colonnes). */
fun Board.transposed(): Board {
    val t = Board(type)
    for (r in 0 until size) for (c in 0 until size) {
        t.letters[c][r] = letters[r][c]
        t.blanks[c][r] = blanks[r][c]
    }
    return t
}

/** Convertit un coup généré sur un plateau transposé en coup sur le plateau original. */
private fun Move.transposedFromTransposed(): Move = copy(
    row = col,
    col = row,
    direction = Direction.VERTICAL,
    placements = placements.map { it.copy(row = it.col, col = it.row) },
)
