package com.scrabbl.ai.engine

/**
 * Dictionnaire ODS.
 *
 * On combine :
 *   - un HashSet pour la validation O(1) d'un mot complet,
 *   - un tableau trié pour la vérification d'un préfixe en O(log n)
 *     (utilisé pour élaguer le générateur de coups).
 *
 * En pratique, pour l'ODS 8 (~400k mots) : ~60 MB de RAM, chargement en ~1s.
 */
class Dictionary private constructor(
    private val sortedWords: Array<String>,
    private val wordSet: HashSet<String>,
) {
    val size: Int get() = sortedWords.size

    fun contains(word: String): Boolean = wordSet.contains(word)

    fun hasPrefix(prefix: String): Boolean {
        if (prefix.isEmpty()) return sortedWords.isNotEmpty()
        var lo = 0
        var hi = sortedWords.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (sortedWords[mid] < prefix) lo = mid + 1 else hi = mid
        }
        return lo < sortedWords.size && sortedWords[lo].startsWith(prefix)
    }

    companion object {
        /** Construit un dictionnaire à partir de mots quelconques (nettoie + dédoublonne + trie). */
        fun from(words: Iterable<String>): Dictionary {
            val cleaned = HashSet<String>(64_000)
            for (raw in words) {
                if (raw.isEmpty()) continue
                val w = buildString(raw.length) {
                    for (c in raw) {
                        val n = FrenchScrabble.normalize(c)
                        if (n in 'A'..'Z') append(n) else return@buildString // stop on invalid char
                    }
                }
                if (w.length in 2..30) cleaned.add(w)
            }
            val sorted = cleaned.toTypedArray()
            sorted.sort()
            return Dictionary(sorted, cleaned)
        }

        val EMPTY: Dictionary = Dictionary(emptyArray(), HashSet())
    }
}
