package com.scrabbl.ai.vision

import android.graphics.Rect
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.scrabbl.ai.engine.FrenchScrabble
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Une lettre reconnue à un endroit précis de l'image. */
data class RecognizedLetter(
    val letter: Char,   // A..Z
    val box: Rect,      // dans l'espace de l'image d'entrée
    val confidence: Float,
)

/**
 * OCR des lettres du plateau via ML Kit Text Recognition (Latin).
 *
 * ML Kit renvoie des « symboles » ; on garde ceux qui sont exactement
 * UN caractère alphabétique (A-Z, insensible à la casse, sans accent).
 * Chaque symbole devient une [RecognizedLetter] avec sa boîte englobante.
 *
 * On rejette :
 *   - les symboles multi-caractères (mots imprimés autour du plateau),
 *   - les caractères non-latins,
 *   - les confidences trop faibles.
 */
class LetterRecognizer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(image: InputImage): List<RecognizedLetter> =
        suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { text ->
                    val out = ArrayList<RecognizedLetter>(64)
                    for (block in text.textBlocks) collectFromBlock(block, out)
                    cont.resume(out)
                }
                .addOnFailureListener { _ -> cont.resume(emptyList()) }
        }

    private fun collectFromBlock(block: Text.TextBlock, out: MutableList<RecognizedLetter>) {
        for (line in block.lines) for (element in line.elements) {
            for (symbol in element.symbols) {
                val raw = symbol.text ?: continue
                if (raw.length != 1) continue
                val ch = FrenchScrabble.normalize(raw[0])
                if (ch !in 'A'..'Z') continue
                val box = symbol.boundingBox ?: continue
                val conf = symbol.confidence ?: 0.5f
                if (conf < 0.35f) continue
                out.add(RecognizedLetter(ch, box, conf))
            }
        }
    }

    fun close() {
        runCatching { recognizer.close() }
    }
}

/** Utilitaire pour construire un InputImage depuis un [ImageProxy] CameraX. */
fun imageProxyToInputImage(proxy: ImageProxy): InputImage? {
    val media = proxy.image ?: return null
    return InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
}
