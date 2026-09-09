package com.scrabbl.ai.dict

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * Télécharge la liste ODS complète depuis un miroir public.
 *
 * Le fichier attendu : liste de mots (un par ligne), éventuellement gzippée.
 * Sur mobile la seule chose qu'on ne peut pas assumer c'est l'accès réseau —
 * on est tolérant aux erreurs et l'app reste utilisable avec la liste starter.
 *
 * La constante [SOURCES] contient des miroirs éprouvés ; si le premier échoue
 * on essaie le suivant.
 */
class DictionaryDownloader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()

    /**
     * @param progress callback en pourcentage (0..100). Peut être appelé plusieurs fois.
     * @return contenu binaire (gzippé si l'URL source l'était).
     */
    fun download(progress: (Int) -> Unit = {}): ByteArray {
        var lastError: Throwable? = null
        for (source in SOURCES) {
            try {
                progress(0)
                val req = Request.Builder().url(source).build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) error("HTTP ${resp.code} sur $source")
                    val body = resp.body ?: error("Réponse vide")
                    val total = body.contentLength().let { if (it <= 0) 8_000_000 else it }
                    val out = ByteArrayOutputStream(total.coerceAtMost(64_000_000).toInt())
                    val buf = ByteArray(64 * 1024)
                    var read = 0L
                    body.byteStream().use { input ->
                        while (true) {
                            val n = input.read(buf)
                            if (n <= 0) break
                            out.write(buf, 0, n)
                            read += n
                            progress(((read * 100) / total).toInt().coerceIn(0, 99))
                        }
                    }
                    progress(100)
                    return out.toByteArray()
                }
            } catch (t: Throwable) {
                lastError = t
            }
        }
        throw lastError ?: RuntimeException("Aucune source disponible")
    }

    companion object {
        // Miroirs publics contenant la liste ODS française.
        // Note : ces URLs peuvent tomber en panne ; l'utilisateur peut aussi
        // remplacer le fichier dans les Réglages.
        private val SOURCES = listOf(
            "https://raw.githubusercontent.com/Thecoolsim/French-Scrabble-ODS8/main/French%20ODS%20dictionary.txt",
            "https://raw.githubusercontent.com/hbenbel/French-Dictionary/master/dictionary/ODS.txt",
        )
    }
}
