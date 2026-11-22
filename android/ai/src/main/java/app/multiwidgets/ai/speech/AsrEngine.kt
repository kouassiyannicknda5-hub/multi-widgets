package app.multiwidgets.ai.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import app.multiwidgets.core.DeviceCapabilities
import java.io.File
import kotlin.coroutines.suspendCoroutine

/**
 * Reconnaissance de la parole, moteurs interchangeables.
 *
 * Moteur initial : [AndroidAsr] (reconnaissance système, gratuite et locale
 * ou cloud selon le fabricant).
 *
 * Whisper (tiny multilingue, puis base après benchmarks) : intégration
 * whisper.cpp/NDK PRÉVUE mais VERROUILLÉE tant que les conditions de la
 * spécification ne sont pas réunies sur l'appareil réel — architecture ARM64
 * vérifiée, modèle présent, benchmark passé. [WhisperGate] expose cet état
 * honnêtement au lieu de prétendre fonctionner.
 */
interface AsrEngine {
    val nom: String
    suspend fun ecouter(langue: String, onPartiel: (String) -> Unit): String
}

class AndroidAsr(private val context: Context) : AsrEngine {
    override val nom = "Reconnaissance Android"

    override suspend fun ecouter(langue: String, onPartiel: (String) -> Unit): String =
        suspendCoroutine { cont ->
            val rec = SpeechRecognizer.createSpeechRecognizer(context)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, langue)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            var final = ""
            rec.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(v: Float) {}
                override fun onBufferReceived(p: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(e: Int) { cont.resumeWith(Result.success(final)) }
                override fun onResults(r: Bundle?) {
                    final = r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    cont.resumeWith(Result.success(final))
                }
                override fun onPartialResults(r: Bundle?) {
                    onPartiel(r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty())
                }
                override fun onEvent(c: Int, p: Bundle?) {}
            })
            rec.startListening(intent)
        }
}

/**
 * Verrou d'honnêteté Whisper : dit précisément pourquoi Whisper n'est pas
 * encore actif sur cet appareil, et ce qui manque pour l'activer.
 */
object WhisperGate {

    data class Etat(val disponible: Boolean, val raisons: List<String>)

    fun evaluer(context: Context, modelePresent: Boolean, benchmarkPasse: Boolean): Etat {
        val raisons = mutableListOf<String>()
        if (!DeviceCapabilities.isArm64()) raisons += "Architecture non ARM64 (${DeviceCapabilities.abis().joinToString()}) : bibliothèque native whisper.cpp non compilée pour ce processeur."
        if (!modelePresent) raisons += "Modèle Whisper tiny non téléchargé (gguf quantifié, à la demande)."
        if (!benchmarkPasse) raisons += "Benchmark de débit non passé sur cet appareil : un modèle trop lent produirait des sous-titres inutilisables."
        if (!DeviceCapabilities.budgetModeleIA(context)) raisons += "Mémoire libre insuffisante pour charger Whisper sans risque."
        return Etat(raisons.isEmpty(), raisons)
    }

    fun fichierModele(context: Context): File = File(context.filesDir, "whisper-tiny.gguf")
}
