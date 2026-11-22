package app.multiwidgets.ai.router

/**
 * AI Processing Router : choisit le moteur de traitement selon les capacités
 * réelles, la connexion, les préférences de confidentialité et l'état de
 * l'appareil. Logique pure, testable sans appareil.
 */
object ProcessingRouter {

    enum class Route { LOCAL, CLOUD, LOCAL_ECO, REFUS }

    data class Entree(
        val reseau: Boolean,
        val modeleLocal: Boolean,
        val fournisseurConfigure: Boolean,
        val mode: String,            // "auto" | "local" | "cloud"
        val eco: Boolean,
        val confidentialiteStricte: Boolean,
        val thermiqueAcceptable: Boolean,
        val memoireSuffisante: Boolean,
    )

    data class Decision(val route: Route, val message: String? = null)

    fun decider(e: Entree): Decision {
        // Confidentialité stricte : rien ne quitte l'appareil, jamais.
        if (e.confidentialiteStricte) {
            return if (e.modeleLocal) Decision(Route.LOCAL, "Confidentialité stricte : traitement local uniquement.")
            else Decision(Route.REFUS, "Confidentialité stricte : téléchargez d'abord le modèle local, ou assouplissez le réglage.")
        }
        if (!e.thermiqueAcceptable)
            return Decision(Route.REFUS, "Appareil trop chaud : traitement suspendu pour protéger la batterie.")
        if (!e.memoireSuffisante)
            return Decision(Route.REFUS, "Mémoire disponible insuffisante pour un modèle : fermez des applications ou activez un mode plus léger.")

        return when (e.mode) {
            "local" -> if (e.modeleLocal) local(e)
            else Decision(Route.REFUS, "Mode local choisi mais modèle non téléchargé.")
            "cloud" -> if (e.reseau && e.fournisseurConfigure) Decision(Route.CLOUD)
            else Decision(Route.REFUS, "Mode cloud choisi : connexion ou fournisseur manquant.")
            else -> auto(e)   // mode automatique intelligent
        }
    }

    private fun auto(e: Entree): Decision = when {
        e.modeleLocal && (e.eco || !e.reseau || !e.fournisseurConfigure) -> local(e)
        e.reseau && e.fournisseurConfigure -> Decision(Route.CLOUD)
        e.modeleLocal -> local(e)
        else -> Decision(Route.REFUS,
            "Aucun moteur disponible : téléchargez un modèle hors ligne ou configurez un fournisseur de traduction.")
    }

    private fun local(e: Entree): Decision =
        if (e.eco) Decision(Route.LOCAL_ECO, "Mode économie : analyses espacées et modèles légers.")
        else Decision(Route.LOCAL)
}
