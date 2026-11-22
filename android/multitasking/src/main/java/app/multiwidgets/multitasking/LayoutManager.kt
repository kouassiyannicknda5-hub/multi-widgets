package app.multiwidgets.multitasking

import android.content.Context
import android.content.Intent
import app.multiwidgets.core.DeviceCapabilities

/**
 * Dispositions multitâches, adossées UNIQUEMENT aux fonctions natives que le
 * firmware confirme prendre en charge (voir [DeviceCapabilities]).
 *
 * Règle d'honnêteté de la spécification : une permission de superposition ne
 * permet PAS d'afficher six à dix applications tierces indépendantes. Sans
 * fenêtres libres (freeform) dans le firmware — cas de la plupart des ZTE,
 * Tecno, Itel, Infinix et de nombreux Redmi/Samsung — les dispositions à
 * trois zones et plus basculent sur le panneau flottant de secours.
 */
object LayoutManager {

    enum class Disposition(val libelle: String, val zones: Int) {
        UNE("Une application", 1),
        COTE("Deux applications côte à côte", 2),
        SUPERPOSEES("Deux applications superposées", 2),
        TROIS("Trois applications", 3),
        QUATRE("Quatre zones d'affichage", 4),
        SIX("Six zones d'affichage", 6),
    }

    data class Disponibilite(val disposition: Disposition, val possible: Boolean, val raison: String?)

    sealed class Resultat {
        data class Lance(val zones: Int) : Resultat()
        data class PanneauSecours(val raison: String) : Resultat()
        data class Refuse(val raison: String) : Resultat()
    }

    fun disponibilites(context: Context): List<Disponibilite> {
        val cap = DeviceCapabilities.snapshot(context)
        val freeform = cap.freeformWindows
        val split = cap.splitScreen
        return listOf(
            Disponibilite(Disposition.UNE, true, null),
            Disponibilite(Disposition.COTE, split,
                if (split) null else "Écran partagé natif absent avant Android 7."),
            Disponibilite(Disposition.SUPERPOSEES, split,
                if (split) null else "Écran partagé natif absent avant Android 7."),
            Disponibilite(Disposition.TROIS, freeform,
                if (freeform) null else FIRMWARE),
            Disponibilite(Disposition.QUATRE, freeform,
                if (freeform) null else FIRMWARE),
            Disponibilite(Disposition.SIX, freeform,
                if (freeform) null else FIRMWARE),
        )
    }

    private const val FIRMWARE =
        "Le firmware ne fournit pas de fenêtres libres (freeform) : impossible " +
        "d'afficher réellement plusieurs applications tierces simultanées. " +
        "Le panneau flottant de secours prend le relais."

    /**
     * Applique la disposition avec les applications demandées.
     * Aucun mensonge : si le système ne sait pas faire, [Resultat.PanneauSecours].
     */
    fun appliquer(context: Context, d: Disposition, paquets: List<String>): Resultat {
        if (paquets.isEmpty()) return Resultat.Refuse("Aucune application choisie.")
        val dispo = disponibilites(context).first { it.disposition == d }
        if (!dispo.possible) return Resultat.PanneauSecours(dispo.raison ?: FIRMWARE)

        return when (d) {
            Disposition.UNE -> {
                lance(context, paquets[0], adjacent = false)
                    ?.let { Resultat.Lance(1) }
                    ?: Resultat.Refuse("Application introuvable : ${paquets[0]}")
            }
            Disposition.COTE, Disposition.SUPERPOSEES -> {
                if (paquets.size < 2) return Resultat.Refuse("Deux applications sont nécessaires.")
                val a = lance(context, paquets[0], adjacent = false)
                val b = lance(context, paquets[1], adjacent = true)
                if (a == null || b == null) Resultat.Refuse("Une des applications n'a pas pu être lancée.")
                else Resultat.Lance(2)
            }
            Disposition.TROIS, Disposition.QUATRE, Disposition.SIX -> {
                // Fenêtres libres confirmées : chaque application ouvre sa fenêtre.
                val ok = paquets.take(d.zones).mapNotNull { lance(context, it, adjacent = true) }
                if (ok.isEmpty()) Resultat.Refuse("Aucune application n'a pu être lancée.")
                else Resultat.Lance(ok.size)
            }
        }
    }

    /**
     * Lancement natif : [adjacent] demande l'ouverture à côté de l'activité
     * courante (écran partagé / freeform selon le firmware).
     */
    private fun lance(context: Context, paquet: String, adjacent: Boolean): Boolean {
        val base = context.packageManager.getLaunchIntentForPackage(paquet) ?: return false
        val intent = Intent(base).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (adjacent) {
            intent.addFlags(
                Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            )
        }
        return runCatching { context.startActivity(intent); true }.getOrDefault(false)
    }
}
