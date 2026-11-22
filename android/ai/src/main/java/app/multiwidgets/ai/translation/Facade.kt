package app.multiwidgets.ai.translation

import android.content.Context
import app.multiwidgets.ai.language.LanguageId
import app.multiwidgets.ai.router.ProcessingRouter
import app.multiwidgets.core.DeviceCapabilities
import app.multiwidgets.core.Settings

data class ResultatTraduction(
    val langueSource: String,
    val traduction: String?,
    val message: String?,
)

/**
 * Chaîne complète : détection de langue → routeur → moteur local ou cloud.
 * Partagée par le traducteur de texte, le traducteur vocal et les sous-titres.
 */
suspend fun traduireSelonRouteur(context: Context, texte: String): ResultatTraduction {
    val settings = Settings(context)
    val langue = LanguageId.detecter(texte)
    if (langue == "und") return ResultatTraduction(langue, null, "Langue non reconnue.")
    val cible = settings.get("langue_cible").ifBlank { "fr" }
    if (langue == cible) return ResultatTraduction(langue, null, "Le texte est déjà en $cible.")
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    val decision = ProcessingRouter.decider(
        ProcessingRouter.Entree(
            reseau = cm.activeNetwork != null,
            modeleLocal = TranslationEngine.modelePresent(context, langue) && TranslationEngine.modelePresent(context, cible),
            fournisseurConfigure = settings.get("fournisseur_url").isNotBlank(),
            mode = settings.get("mode_traitement").ifBlank { "auto" },
            eco = settings.get("eco").toBoolean(),
            confidentialiteStricte = settings.get("confidentialite_stricte").toBoolean(),
            thermiqueAcceptable = DeviceCapabilities.thermal(context) != DeviceCapabilities.Thermal.HOT,
            memoireSuffisante = DeviceCapabilities.budgetModeleIA(context),
        )
    )
    val traduit = when (decision.route) {
        ProcessingRouter.Route.LOCAL, ProcessingRouter.Route.LOCAL_ECO ->
            TranslationEngine.traduire(texte, langue, cible)
        ProcessingRouter.Route.CLOUD -> {
            val f = fournisseurConfigure(settings.get("fournisseur_url"))
            f?.traduire(texte, langue, cible, settings.cleFournisseur())?.getOrNull()
        }
        ProcessingRouter.Route.REFUS -> null
    }
    return ResultatTraduction(langue, traduit, decision.message)
}
