package app.multiwidgets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.multiwidgets.ui.EcranAppareil
import app.multiwidgets.ui.EcranBarreLaterale
import app.multiwidgets.ui.EcranConfidentialite
import app.multiwidgets.ui.EcranDemarrage
import app.multiwidgets.ui.EcranFenetres
import app.multiwidgets.ui.EcranHistorique
import app.multiwidgets.ui.EcranLanceur
import app.multiwidgets.ui.EcranLangues
import app.multiwidgets.ui.EcranModeles
import app.multiwidgets.ui.EcranPerformance
import app.multiwidgets.ui.EcranSousTitres
import app.multiwidgets.ui.EcranTableauDeBord
import app.multiwidgets.ui.EcranTraducteurEcran
import app.multiwidgets.ui.EcranTraducteurTexte
import app.multiwidgets.ui.EcranTraducteurVocal
import app.multiwidgets.ui.MultiWidgetsTheme

/** Activité unique : navigation Compose entre les quinze écrans. */
class MainActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContent {
            MultiWidgetsTheme {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = "demarrage") {
                    composable("demarrage") { EcranDemarrage { nav.navigate("tableau") } }
                    composable("tableau") { EcranTableauDeBord { r -> nav.navigate(r) } }
                    composable("barre") { EcranBarreLaterale() }
                    composable("fenetres") { EcranFenetres() }
                    composable("lanceur") { EcranLanceur() }
                    composable("texte") { EcranTraducteurTexte() }
                    composable("ecran") { EcranTraducteurEcran() }
                    composable("voix") { EcranTraducteurVocal() }
                    composable("sous_titres") { EcranSousTitres() }
                    composable("langues") { EcranLangues() }
                    composable("modeles") { EcranModeles() }
                    composable("confidentialite") { EcranConfidentialite() }
                    composable("performance") { EcranPerformance() }
                    composable("appareil") { EcranAppareil() }
                    composable("historique") { EcranHistorique() }
                }
            }
        }
    }
}
