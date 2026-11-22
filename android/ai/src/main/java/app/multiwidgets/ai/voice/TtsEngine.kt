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
        val voices = tts?.voices ?: return emptyList()
        val tags = voices.flatMap { it.tags ?: emptySet() }.map { it.lowercase() }.toSet()
        val noms = voices.map { it.name.lowercase() }
        val out = mutableListOf<String>()
        if (tags.any { it.contains("female") } || noms.any { it.contains("female") || it.contains("femme") }) out += "feminin"
        if (tags.any { it.contains("male") } || noms.any { it.contains("male") || it.contains("homme") }) out += "masculin"
        out += "neutre"   // toute voix sans marqueur de genre est neutre
        return out
    }

    fun langues(): List<Locale> = tts?.availableLanguages?.toList() ?: emptyList()

    fun appliquer(langue: Locale, genre: String, vitesse: Float) {
        val t = tts ?: return
        t.language = langue
        t.setSpeechRate(vitesse.coerceIn(0.5f, 2f))
        val voices: Set<Voice> = t.voices ?: emptySet()
        val choisi = when (genre) {
            "feminin" -> voices.firstOrNull { it.tags?.any { x -> x.contains("female", true) } == true }
            "masculin" -> voices.firstOrNull { it.tags?.any { x -> x.contains("male", true) } == true }
            else -> null
        }
        if (choisi != null) runCatching { t.voice = choisi }
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
