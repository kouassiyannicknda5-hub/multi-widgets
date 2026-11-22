package app.multiwidgets.ai.language

import com.google.mlkit.nl.languageid.LanguageIdentification
import kotlin.coroutines.suspendCoroutine

/** Détection automatique de langue, locale (ML Kit Language Identification). */
object LanguageId {

    suspend fun detecter(texte: String): String = suspendCoroutine { cont ->
        if (texte.isBlank()) { cont.resumeWith(Result.success("und")); return@suspendCoroutine }
        LanguageIdentification.getClient().identifyLanguage(texte)
            .addOnSuccessListener { cont.resumeWith(Result.success(it)) }
            .addOnFailureListener { cont.resumeWith(Result.success("und")) }
    }
}
