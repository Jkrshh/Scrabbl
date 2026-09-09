package com.scrabbl.ai.dict

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class DictionaryDownloader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(180, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun download(progress: (Int) -> Unit = {}): ByteArray {
        var lastError: Throwable? = null
        for (source in SOURCES) {
            try {
                progress(0)
                val req = Request.Builder()
                    .url(source)
                    .header("User-Agent", "Scrabbl-AI/1.0")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) error("HTTP ${resp.code} sur $source")
                    val body = resp.body ?: error("Réponse vide")
                    val total = body.contentLength().let { if (it <= 0) 5_000_000L else it }
                    val out = ByteArrayOutputStream(total.coerceAtMost(20_000_000).toInt())
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
                    val bytes = out.toByteArray()
                    if (bytes.size < 500_000) error("Fichier trop petit (${bytes.size} B) sur $source")
                    progress(100)
                    return bytes
                }
            } catch (t: Throwable) {
                lastError = t
            }
        }
        throw lastError ?: RuntimeException("Aucune source disponible")
    }

    companion object {
        private val SOURCES = listOf(
            "https://raw.githubusercontent.com/Thecoolsim/French-Scrabble-ODS8/main/French%20ODS%20dictionary.txt",
            "https://raw.githubusercontent.com/lehublot/scrabble-fr/master/fr.txt",
            "https://raw.githubusercontent.com/pierrepo/PyBioTools/master/data/liste_francais.txt",
        )
    }
}
