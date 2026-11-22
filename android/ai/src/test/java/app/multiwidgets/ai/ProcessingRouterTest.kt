package app.multiwidgets.ai

import app.multiwidgets.ai.router.ProcessingRouter
import app.multiwidgets.ai.router.ProcessingRouter.Decision
import app.multiwidgets.ai.router.ProcessingRouter.Entree
import app.multiwidgets.ai.router.ProcessingRouter.Route
import org.junit.Assert.assertEquals
import org.junit.Test

/** Le routeur choisit le moteur selon capacités, réseau, confidentialité et état. */
class ProcessingRouterTest {

    private fun e(
        reseau: Boolean = true,
        modele: Boolean = true,
        fournisseur: Boolean = true,
        mode: String = "auto",
        eco: Boolean = false,
        stricte: Boolean = false,
        thermique: Boolean = true,
        memoire: Boolean = true,
    ) = Entree(reseau, modele, fournisseur, mode, eco, stricte, thermique, memoire)

    @Test fun `confidentialite stricte reste locale meme avec reseau`() {
        assertEquals(Route.LOCAL, ProcessingRouter.decider(e(stricte = true)).route)
    }

    @Test fun `confidentialite stricte sans modele refuse plutot que fuiter`() {
        val d = ProcessingRouter.decider(e(stricte = true, modele = false))
        assertEquals(Route.REFUS, d.route)
    }

    @Test fun `auto prefere le cloud si fournisseur et reseau`() {
        assertEquals(Route.CLOUD, ProcessingRouter.decider(e()).route)
    }

    @Test fun `auto bascule en local sans reseau`() {
        assertEquals(Route.LOCAL, ProcessingRouter.decider(e(reseau = false)).route)
    }

    @Test fun `mode eco economise avec modele local`() {
        assertEquals(Route.LOCAL_ECO, ProcessingRouter.decider(e(eco = true, fournisseur = false)).route)
    }

    @Test fun `appareil trop chaud suspend le traitement`() {
        assertEquals(Route.REFUS, ProcessingRouter.decider(e(thermique = false)).route)
    }

    @Test fun `memoire insuffisante refuse au lieu de saturer`() {
        assertEquals(Route.REFUS, ProcessingRouter.decider(e(memoire = false)).route)
    }

    @Test fun `mode local sans modele explique le refus`() {
        val d: Decision = ProcessingRouter.decider(e(mode = "local", modele = false))
        assertEquals(Route.REFUS, d.route)
        assertEquals(true, d.message?.isNotBlank())
    }

    @Test fun `mode cloud sans fournisseur explique le refus`() {
        assertEquals(Route.REFUS, ProcessingRouter.decider(e(mode = "cloud", fournisseur = false)).route)
    }
}
