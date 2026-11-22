package app.multiwidgets.core

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "multiwidgets")

/**
 * Réglages persistants (DataStore).
 * Les secrets (clé API) vivent dans le DataStore privé de l'application,
 * jamais dans le code source ni dans un dépôt public.
 */
class Settings(private val context: Context) {

    private object K {
        val LANGUE_CIBLE = stringPreferencesKey("langue_cible")
        val LANGUE_SOURCE = stringPreferencesKey("langue_source")
        val MODE_TRAITEMENT = stringPreferencesKey("mode_traitement")   // auto | local | cloud
        val ECO = booleanPreferencesKey("eco")
        val CONFIDENTIALITE_STRICTE = booleanPreferencesKey("confidentialite_stricte")
        val HISTORIQUE = booleanPreferencesKey("historique")
        val FOURNISSEUR_URL = stringPreferencesKey("fournisseur_url")
        val FOURNISSEUR_CLE = stringPreferencesKey("fournisseur_cle")
        val TTS_GENRE = stringPreferencesKey("tts_genre")               // feminin | masculin | neutre
        val TTS_VITESSE = stringPreferencesKey("tts_vitesse")
        val SOUSTITRES_SYNC_MS = stringPreferencesKey("sous_titres_sync_ms")
        val OCR_FREQUENCE_MS = stringPreferencesKey("ocr_frequence_ms")
    }

    val flux: Flow<Map<String, String>> = context.dataStore.data.map { p ->
        mapOf(
            "langue_cible" to (p[K.LANGUE_CIBLE] ?: "fr"),
            "langue_source" to (p[K.LANGUE_SOURCE] ?: "auto"),
            "mode_traitement" to (p[K.MODE_TRAITEMENT] ?: "auto"),
            "eco" to (p[K.ECO] ?: false).toString(),
            "confidentialite_stricte" to (p[K.CONFIDENTIALITE_STRICTE] ?: true).toString(),
            "historique" to (p[K.HISTORIQUE] ?: false).toString(),
            "fournisseur_url" to (p[K.FOURNISSEUR_URL] ?: ""),
            "tts_genre" to (p[K.TTS_GENRE] ?: "neutre"),
            "tts_vitesse" to (p[K.TTS_VITESSE] ?: "1.0"),
            "sous_titres_sync_ms" to (p[K.SOUSTITRES_SYNC_MS] ?: "0"),
            "ocr_frequence_ms" to (p[K.OCR_FREQUENCE_MS] ?: "2500"),
        )
    }

    suspend fun get(key: String): String = flux.first()[key] ?: ""

    suspend fun set(key: String, value: String) {
        context.dataStore.edit { p ->
            when (key) {
                "langue_cible" -> p[K.LANGUE_CIBLE] = value
                "langue_source" -> p[K.LANGUE_SOURCE] = value
                "mode_traitement" -> p[K.MODE_TRAITEMENT] = value
                "eco" -> p[K.ECO] = value.toBoolean()
                "confidentialite_stricte" -> p[K.CONFIDENTIALITE_STRICTE] = value.toBoolean()
                "historique" -> p[K.HISTORIQUE] = value.toBoolean()
                "fournisseur_url" -> p[K.FOURNISSEUR_URL] = value
                "fournisseur_cle" -> p[K.FOURNISSEUR_CLE] = value
                "tts_genre" -> p[K.TTS_GENRE] = value
                "tts_vitesse" -> p[K.TTS_VITESSE] = value
                "sous_titres_sync_ms" -> p[K.SOUSTITRES_SYNC_MS] = value
                "ocr_frequence_ms" -> p[K.OCR_FREQUENCE_MS] = value
            }
        }
    }

    /** La clé API n'est lue que par le moteur cloud, jamais affichée en clair. */
    suspend fun cleFournisseur(): String =
        context.dataStore.data.first()[K.FOURNISSEUR_CLE] ?: ""
}
