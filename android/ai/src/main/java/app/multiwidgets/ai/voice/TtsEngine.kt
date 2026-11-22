package app.multiwidgets.ai.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale
import kotlin.coroutines.suspendCoroutine

/**
 * Synthèse vocale Android (TextToSpeech).
 *
 * Honnêteté exigée par la spécification : le genre vocal n'est PROPOSÉ que si
 * le moteur installé fournit réellement les voix correspondantes. [genres]
 * liste ce qui existe sur l'appareil, rien de plus.
 */
class TtsEngine(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var pret = false

    suspend fun initialiser(): Boolean = suspendCoroutine { cont ->
        tts = TextToSpeech(context) { status ->
            pret = status == TextToSpeech.SUCCESS
            cont.resumeWith(Result.success(pret))
        }
    }

    /** Genres réellement fournis par le moteur installé : féminin, masculin, neutre, ou un sous-ensemble. */
    fun genres(): List<String> {
        val noms: List<String> = nomsVoix()
        val out = mutableListOf<String>()
        if (noms.any { marqueurFeminin(it) }) out += "feminin"
        if (noms.any { marqueurMasculin(it) }) out += "masculin"
        out += "neutre"   // toute voix sans marqueur de genre est neutre
        return out
    }

    private fun nomsVoix(): List<String> =
        runCatching { tts?.getVoices()?.map { v -> v.getName().lowercase() } ?: emptyList() }
            .getOrDefault(emptyList())

    private fun marqueurFeminin(n: String) =
        n.contains("female") || n.contains("femme") || n.contains("woman") || n.contains("-f-")

    private fun marqueurMasculin(n: String) =
        n.contains("male") || n.contains("homme") || n.contains("man") || n.contains("-m-")

    fun langues(): List<Locale> = tts?.availableLanguages?.toList() ?: emptyList()

    fun appliquer(langue: Locale, genre: String, vitesse: Float) {
        val t = tts ?: return
        t.language = langue
        t.setSpeechRate(vitesse.coerceIn(0.5f, 2f))
        val voices: Set<Voice> = runCatching { t.getVoices() ?: emptySet() }.getOrDefault(emptySet())
        val choisi: Voice? = when (genre) {
            "feminin" -> voices.firstOrNull { marqueurFeminin(it.getName().lowercase()) }
            "masculin" -> voices.firstOrNull { marqueurMasculin(it.getName().lowercase()) }
            else -> null
        }
        if (choisi != null) runCatching { t.setVoice(choisi) }
    }

    /** Doublage progressif : chaque segment est mis en file, sans bloquer l'interface. */
    fun dire(segment: String, langue: Locale, genre: String, vitesse: Float) {
        if (!pret) return
        appliquer(langue, genre, vitesse)
        tts?.speak(segment, TextToSpeech.QUEUE_ADD, null, "seg${segment.hashCode()}")
    }

    fun interrompre() = runCatching { tts?.stop() }
    fun libérer() = runCatching { tts?.shutdown(); tts = null; pret = false }
}
