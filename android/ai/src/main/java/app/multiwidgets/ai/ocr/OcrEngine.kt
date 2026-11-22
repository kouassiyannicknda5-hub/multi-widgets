package app.multiwidgets.ai.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.suspendCoroutine

/**
 * OCR local (ML Kit Text Recognition), un moteur par écriture.
 * L'architecture permet d'ajouter d'autres moteurs : implémenter [MoteurOcr]
 * et l'enregistrer dans [moteurs].
 */
interface MoteurOcr {
    val nom: String
    suspend fun reconnaitre(bitmap: Bitmap): String
}

class OcrEngine {

    enum class Ecriture(val code: String) { LATIN("latin"), CHINOIS("zh"), JAPONAIS("ja"), COREEN("ko"), DEVANAGARI("hi") }

    private val moteurs: Map<Ecriture, MoteurOcr> = Ecriture.values().associateWith { e -> MlKitOcr(e) }

    fun ecritures(): List<Ecriture> = moteurs.keys.toList()

    suspend fun reconnaitre(bitmap: Bitmap, ecriture: Ecriture = Ecriture.LATIN): String =
        moteurs.getValue(ecriture).reconnaitre(bitmap)

    private class MlKitOcr(private val e: Ecriture) : MoteurOcr {
        override val nom = "ML Kit ${e.code}"
        override suspend fun reconnaitre(bitmap: Bitmap): String {
            val options = when (e) {
                Ecriture.LATIN -> TextRecognizerOptions.DEFAULT_OPTIONS
                Ecriture.CHINOIS -> ChineseTextRecognizerOptions.Builder().build()
                Ecriture.JAPONAIS -> JapaneseTextRecognizerOptions.Builder().build()
                Ecriture.COREEN -> KoreanTextRecognizerOptions.Builder().build()
                Ecriture.DEVANAGARI -> DevanagariTextRecognizerOptions.Builder().build()
            }
            val client = TextRecognition.getClient(options)
            return try {
                suspendCoroutine { cont ->
                    client.process(InputImage.fromBitmap(bitmap, 0))
                        .addOnSuccessListener { cont.resumeWith(Result.success(it.text)) }
                        .addOnFailureListener { cont.resumeWith(Result.success("")) }
                }
            } finally {
                client.close()
            }
        }
    }
}
