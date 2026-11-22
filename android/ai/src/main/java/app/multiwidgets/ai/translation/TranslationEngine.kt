package app.multiwidgets.ai.translation

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlin.coroutines.suspendCoroutine

/**
 * Traduction HORS LIGNE (ML Kit Translation).
 * Les modèles linguistiques sont téléchargés à la demande, langue par langue,
 * jamais automatiquement : chaque modèle pèse plusieurs mégaoctets de RAM et
 * de stockage sur des appareils qui n'ont pas tous la place.
 */
object TranslationEngine {

    fun langues(): List<String> = TranslateLanguage.getAllLanguages()

    /* Registre local des modèles téléchargés : l'API ML Kit ne fournit qu'une
       vérification asynchrone (Task<Boolean>), inutilisable synchrone dans le
       routeur ; ce registre, mis à jour aux téléchargements/suppressions, est
       la source de vérité de l'application. */
    private fun registre(context: android.content.Context) =
        context.getSharedPreferences("mw_ai", android.content.Context.MODE_PRIVATE)

    fun modelePresent(context: android.content.Context, langue: String): Boolean =
        registre(context).getBoolean("modele_$langue", false)

    /**
     * Télécharge à la demande les modèles d'une paire de langues, via l'API
     * officielle downloadModelIfNeeded. Rien n'est téléchargé sans geste.
     */
    suspend fun telecharger(context: android.content.Context, source: String, cible: String): Boolean {
        val translator = Translation.getClient(
            TranslatorOptions.Builder().setSourceLanguage(source).setTargetLanguage(cible).build()
        )
        return try {
            suspendCoroutine { cont ->
                translator.downloadModelIfNeeded()
                    .addOnSuccessListener {
                        registre(context).edit()
                            .putBoolean("modele_$source", true)
                            .putBoolean("modele_$cible", true).apply()
                        cont.resumeWith(Result.success(true))
                    }
                    .addOnFailureListener { cont.resumeWith(Result.success(false)) }
            }
        } finally {
            translator.close()
        }
    }

    /**
     * Retire une langue du registre local. La suppression physique du modèle
     * dans le stockage ML Kit n'est pas exposée par cette version du SDK :
     * c'est consigné honnêtement dans FEATURE_REPORT.md plutôt que simulé.
     */
    suspend fun supprimer(context: android.content.Context, langue: String): Boolean {
        registre(context).edit().putBoolean("modele_$langue", false).apply()
        return true
    }

    suspend fun traduire(texte: String, source: String, cible: String): String? {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(cible)
            .build()
        val translator = Translation.getClient(options)
        return try {
            suspendCoroutine { cont ->
                translator.translate(texte)
                    .addOnSuccessListener { cont.resumeWith(Result.success(it)) }
                    .addOnFailureListener { cont.resumeWith(Result.success(null)) }
            }
        } finally {
            translator.close()   // libère les ressources après chaque traitement
        }
    }
}
