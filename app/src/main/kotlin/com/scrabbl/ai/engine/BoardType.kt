package com.scrabbl.ai.engine

/**
 * Type d'un carreau du plateau (case).
 *  - LM/TM = Lettre × 2 ou × 3
 *  - LW/TW = Mot × 2 ou × 3
 *  - CENTER = case centrale (Mot × 2 dans les règles ODS)
 */
enum class Premium(val letterMul: Int, val wordMul: Int) {
    NORMAL(1, 1),
    DL(2, 1),
    TL(3, 1),
    DW(1, 2),
    TW(1, 3),
    CENTER(1, 2);
}

/**
 * Types de plateaux reconnus. La détection caméra choisit le plus proche
 * en se basant sur la taille de grille.
 */
enum class BoardType(val size: Int, val label: String) {
    CLASSIC(15, "Classique 15x15"),
    DELUXE(15, "Deluxe 15x15"),
    SUPER(21, "Super 21x21"),
    WESH(11, "Wesh 11x11"),
    JUNIOR(13, "Junior 13x13");

    fun premiumGrid(): Array<Array<Premium>> = when (this) {
        CLASSIC, DELUXE -> classic15
        SUPER -> super21
        WESH -> wesh11
        JUNIOR -> junior13
    }

    val center: Pair<Int, Int> get() = size / 2 to size / 2

    companion object {
        fun forSize(size: Int): BoardType = when {
            size <= 11 -> WESH
            size <= 13 -> JUNIOR
            size <= 17 -> CLASSIC
            else -> SUPER
        }
    }
}

// ------------------------------------------------------------------
// Layouts de primes (les grilles sont symétriques ; on les construit
// à partir du quart supérieur-gauche).
// ------------------------------------------------------------------

private fun build(size: Int, quadrant: Array<String>): Array<Array<Premium>> {
    val half = (size + 1) / 2
    require(quadrant.size == half) { "quadrant must have $half rows (has ${quadrant.size})" }
    val grid = Array(size) { Array(size) { Premium.NORMAL } }
    for (r in 0 until half) for (c in 0 until half) {
        val p = when (quadrant[r][c]) {
            '.' -> Premium.NORMAL
            'l' -> Premium.DL
            'L' -> Premium.TL
            'w' -> Premium.DW
            'W' -> Premium.TW
            '*' -> Premium.CENTER
            else -> Premium.NORMAL
        }
        // reflect into the four quadrants
        val rr = size - 1 - r
        val cc = size - 1 - c
        grid[r][c] = p
        grid[r][cc] = p
        grid[rr][c] = p
        grid[rr][cc] = p
    }
    return grid
}

// Layout ODS classique 15x15 (moitié 8x8 ; * au centre = Mot ×2 = premier coup).
private val classic15 = build(
    15,
    arrayOf(
        "W..l...W",
        ".w...L..",
        "..w...l.",
        "l..w...l",
        "....w...",
        ".L...L..",
        "..l...l.",
        "W..l...*",
    ),
)

// Layout Super 21x21 — extension homogène du 15x15 (approximation).
private val super21 = build(
    21,
    arrayOf(
        "W...l....w..",
        ".w....L.....",
        "..w....L....",
        "l..w....l...",
        "....w....l..",
        ".L...w......",
        "..l...w...L.",
        "L..l...w....",
        "....l...w...",
        ".L....l..w..",
        "..L....l..w.",
        "l..w....l..*",
    ),
)

// Layout Wesh 11x11 (compact).
private val wesh11 = build(
    11,
    arrayOf(
        "W....l",
        ".w..L.",
        "..w..l",
        "l..w..",
        "..L.w.",
        "l....*",
    ),
)

// Layout Junior 13x13.
private val junior13 = build(
    13,
    arrayOf(
        "W..l..w",
        ".w...L.",
        "..w...l",
        "l..w...",
        "....w..",
        ".L...L.",
        "w...l.*",
    ),
)
