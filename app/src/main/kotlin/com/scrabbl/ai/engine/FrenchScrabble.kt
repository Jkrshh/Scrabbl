package com.scrabbl.ai.engine

/**
 * Règles françaises officielles (ODS).
 *
 * L'alphabet est stocké sur 27 slots :
 *  0 = joker (blanc)
 *  1..26 = A..Z
 *
 * Les lettres accentuées (É, È, Ê, Ç…) sont traitées comme leur équivalent non
 * accentué (l'ODS n'utilise pas les accents sur les tuiles).
 */
object FrenchScrabble {

    /** Code du joker sur le rack. */
    const val BLANK: Int = 0

    /** Nombre de lettres normales (hors joker). */
    const val LETTERS = 26

    /** Taille de la pioche par lettre — total 102 tuiles. */
    val distribution: IntArray = IntArray(27).also {
        it[BLANK] = 2
        it[l('A')] = 9;  it[l('B')] = 2;  it[l('C')] = 2;  it[l('D')] = 3
        it[l('E')] = 15; it[l('F')] = 2;  it[l('G')] = 2;  it[l('H')] = 2
        it[l('I')] = 8;  it[l('J')] = 1;  it[l('K')] = 1;  it[l('L')] = 5
        it[l('M')] = 3;  it[l('N')] = 6;  it[l('O')] = 6;  it[l('P')] = 2
        it[l('Q')] = 1;  it[l('R')] = 6;  it[l('S')] = 6;  it[l('T')] = 6
        it[l('U')] = 6;  it[l('V')] = 2;  it[l('W')] = 1;  it[l('X')] = 1
        it[l('Y')] = 1;  it[l('Z')] = 1
    }

    /** Points par tuile. */
    val value: IntArray = IntArray(27).also {
        it[BLANK] = 0
        for (c in 'A'..'Z') it[l(c)] = when (c) {
            'A','E','I','L','N','O','R','S','T','U' -> 1
            'D','G','M' -> 2
            'B','C','P' -> 3
            'F','H','V' -> 4
            'J','Q' -> 8
            'K','W','X','Y','Z' -> 10
            else -> 0
        }
    }

    /** Taille du chevalet (rack). */
    const val RACK_SIZE = 7

    /** Bonus « scrabble » : les 7 lettres jouées d'un coup rapportent 50 points de plus. */
    const val BINGO_BONUS = 50

    /** Convertit un char en code 1..26 (majuscule attendue, sans accent). */
    fun l(c: Char): Int = c.code - 'A'.code + 1

    /** Convertit un code 1..26 en majuscule. Retourne '?' pour un joker (0). */
    fun ch(code: Int): Char = if (code == 0) '?' else ('A' + code - 1)

    /** Normalise une lettre saisie ou reconnue (majuscule, sans accent). */
    fun normalize(c: Char): Char {
        val upper = c.uppercaseChar()
        return when (upper) {
            'À','Â','Ä','Á','Ã','Å' -> 'A'
            'Ç' -> 'C'
            'È','É','Ê','Ë' -> 'E'
            'Ì','Í','Î','Ï' -> 'I'
            'Ñ' -> 'N'
            'Ò','Ó','Ô','Ö','Õ' -> 'O'
            'Ù','Ú','Û','Ü' -> 'U'
            'Ý','Ÿ' -> 'Y'
            'Œ' -> 'O' // approx — ODS n'utilise pas les ligatures
            'Æ' -> 'A'
            else -> upper
        }
    }

    /** Convertit un mot texte en tableau de codes 1..26 (blanks non représentés ici). */
    fun encode(word: String): IntArray {
        val arr = IntArray(word.length)
        for (i in word.indices) {
            val c = normalize(word[i])
            arr[i] = if (c in 'A'..'Z') l(c) else 0
        }
        return arr
    }
}
