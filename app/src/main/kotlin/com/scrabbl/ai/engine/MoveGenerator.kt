package com.scrabbl.ai.engine

class MoveGenerator(private val dict: Dictionary) {

    fun generate(board: Board, rack: IntArray, topK: Int = 200): List<Move> {
        val all = ArrayList<Move>(1024)
        solveOnOrientation(board, rack, transposed = false, out = all)
        solveOnOrientation(board.transposed(), rack, transposed = true, out = all)
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
        val cc = computeCrossChecks(board)
        val rackCounts = IntArray(27).also { for (t in rack) if (t in 0..26) it[t]++ }
        val emptyBoard = board.isBoardEmpty()

        for (r in 0 until size) {
            for (startC in 0 until size) {
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

        val nextEmpty = (c >= board.size) || board.isEmpty(r, c)
        if (nextEmpty && placements.isNotEmpty() && prefix.length >= 2 && dict.contains(prefix)) {
            val connects = if (emptyBoard) {
                val cr = board.size / 2
                placements.any { it.row == cr && it.col == cr }
            } else {
                touchesBoard(board, r, startC, prefix.length, placements)
            }
            if (connects) {
                val move = buildMove(board, r, startC, prefix, placements, transposed)
                out.add(move)
            }
        }

        if (c >= board.size) return

        if (!board.isEmpty(r, c)) {
            val letter = board.letters[r][c]
            sb.append(FrenchScrabble.ch(letter))
            extend(board, r, startC, sb, placements, rack, cc, out, transposed, emptyBoard)
            sb.deleteCharAt(sb.length - 1)
            return
        }

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

    private fun touchesBoard(
        board: Board,
        r: Int,
        startC: Int,
        wordLen: Int,
        placements: List<Placement>,
    ): Boolean {
        for (c in startC until startC + wordLen) {
            if (!board.isEmpty(r, c) && placements.none { it.col == c && it.row == r }) return true
        }
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

    private fun computeCrossChecks(board: Board): Array<Array<BooleanArray>> {
        val size = board.size
        val cc = Array(size) { Array(size) { BooleanArray(27) } }
        for (r in 0 until size) for (c in 0 until size) {
            if (!board.isEmpty(r, c)) continue
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

fun Board.transposed(): Board {
    val t = Board(type)
    for (r in 0 until size) for (c in 0 until size) {
        t.letters[c][r] = letters[r][c]
        t.blanks[c][r] = blanks[r][c]
        t.premium[c][r] = premium[r][c]
    }
    return t
}

private fun Move.transposedFromTransposed(): Move = copy(
    row = col,
    col = row,
    direction = Direction.VERTICAL,
    placements = placements.map { it.copy(row = it.col, col = it.row) },
)
