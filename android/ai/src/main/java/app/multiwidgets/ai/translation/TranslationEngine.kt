package app.multiwidgets.ai.translation

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.ModelManager
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

    fun modelePresent(langue: String): Boolean =
        runCatching {
            ModelManager.getInstance()
                .isModelDownloaded(com.google.mlkit.nl.translate.TranslateRemoteModel.Builder(langue).build())
        }.getOrDefault(false)

    suspend fun telecharger(langue: String): Boolean = suspendCoroutine { cont ->
        val model = com.google.mlkit.nl.translate.TranslateRemoteModel.Builder(langue).build()
        ModelManager.getInstance().download(model, DownloadConditions.Builder().build())
            .addOnSuccessListener { cont.resumeWith(Result.success(true)) }
            .addOnFailureListener { cont.resumeWith(Result.success(false)) }
    }

    suspend fun supprimer(langue: String): Boolean = suspendCoroutine { cont ->
        val model = com.google.mlkit.nl.translate.TranslateRemoteModel.Builder(langue).build()
        ModelManager.getInstance().deleteModel(model)
            .addOnSuccessListener { cont.resumeWith(Result.success(true)) }
            .addOnFailureListener { cont.resumeWith(Result.success(false)) }
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
