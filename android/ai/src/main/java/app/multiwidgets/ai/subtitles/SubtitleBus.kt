package app.multiwidgets.ai.subtitles

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Segment de sous-titre : source reconnue, langue détectée, traduction, horodatage. */
data class Segment(
    val source: String,
    val langue: String,
    val traduction: String,
    val ts: Long = System.currentTimeMillis(),
)

/**
 * File des sous-titres partagée entre les moteurs (capture d'écran, voix) et
 * l'affichage flottant. Mémoire bornée : on ne garde que les derniers
 * segments, jamais toute une conversation (confidentialité + RAM).
 */
object SubtitleBus {

    private val _segments = MutableStateFlow<List<Segment>>(emptyList())
    val segments: StateFlow<List<Segment>> = _segments

    private const val MAX = 12

    fun publier(s: Segment) {
        _segments.value = (_segments.value + s).takeLast(MAX)
    }

    fun vider() {
        _segments.value = emptyList()
    }
}
