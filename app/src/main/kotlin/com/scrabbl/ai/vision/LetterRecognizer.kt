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
import kotlin.math.abs

/** Une lettre reconnue à un endroit précis de l'image. */
data class RecognizedLetter(
    val letter: Char,   // A..Z
    val box: Rect,      // dans l'espace de l'image d'entrée
    val confidence: Float,
)

/**
 * OCR des lettres du plateau via ML Kit Text Recognition (Latin).
 *
 * On accepte une lettre uniquement si :
 *   - Son [Text.Element] parent (« mot » ML Kit) est exactement un caractère —
 *     ça élimine les étiquettes de primes du type « 3x », « vl », « vm » qu'on
 *     voit sur les vraies apps de Scrabble et qui polluaient énormément le
 *     détecteur.
 *   - Le caractère est A..Z (normalisé sans accent).
 *   - Sa boîte est ~carrée (les tuiles le sont).
 *   - Sa confiance est suffisante.
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
            // On rejette les mots multi-caractères : ce sont des labels d'UI
            // (« 3x », « vl », « vm », « Bag », « Score », etc.) et non des tuiles.
            val elementText = element.text?.trim() ?: continue
            if (elementText.length != 1) continue

            val ch = FrenchScrabble.normalize(elementText[0])
            if (ch !in 'A'..'Z') continue

            val elementBox = element.boundingBox ?: continue
            // Rejette les boîtes trop allongées (les tuiles sont carrées).
            val w = elementBox.width().toFloat()
            val h = elementBox.height().toFloat()
            if (w < 8f || h < 8f) continue
            val ratio = if (w > h) w / h else h / w
            if (ratio > 2.2f) continue

            // Utilise la confiance globale de l'élément (meilleure que celle du symbole isolé).
            val conf = element.confidence ?: 0.5f
            if (conf < 0.35f) continue

            out.add(RecognizedLetter(ch, elementBox, conf))
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
