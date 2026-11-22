package app.multiwidgets.ui

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.multiwidgets.ai.subtitles.SubtitleOverlayService
import app.multiwidgets.ai.capture.ScreenCaptureService
import app.multiwidgets.core.DeviceCapabilities
import app.multiwidgets.core.LauncherRepository
import app.multiwidgets.core.Permissions
import app.multiwidgets.multitasking.LayoutManager
import app.multiwidgets.multitasking.SidebarService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* ---------- gabarit commun ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Page(titre: String, content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(titre) }) }) { pv -> content(pv) }
}

@Composable
fun Carte(titre: String, detail: String, action: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(titre, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall)
            Button(onClick = action, modifier = Modifier.padding(top = 8.dp)) { Text("Ouvrir") }
        }
    }
}

/* ---------- 1. démarrage ---------- */

@Composable
fun EcranDemarrage(suite: () -> Unit) {
    LaunchedEffect(Unit) { delay(700); suite() }
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        Text("Multi-widgets", style = MaterialTheme.typography.headlineLarge)
        Text("AI Multitasking & Universal Translator", style = MaterialTheme.typography.bodyMedium)
    }
}

/* ---------- 2. tableau de bord ---------- */

@Composable
fun EcranTableauDeBord(nav: (String) -> Unit) {
    val ctx = LocalContext.current
    val cap = remember { DeviceCapabilities.snapshot(ctx) }
    Page("Tableau de bord") { pv ->
        LazyColumn(Modifier.fillMaxSize().padding(pv).padding(horizontal = 12.dp)) {
            item {
                Text(
                    "${cap.manufacturer} ${cap.model} · Android ${cap.androidSdk} · " +
                    "${cap.availableRamMb} Mo libres · ${if (cap.freeformWindows) "fenêtres libres" else "sans fenêtres libres"}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
            item { Carte("Barre latérale", "Poignée au bord de l'écran, bouton flottant, applications favorites.", { nav("barre") }) }
            item { Carte("Gestionnaire de fenêtres", "Dispositions natives : 1, 2, 3, 4 ou 6 zones selon le firmware.", { nav("fenetres") }) }
            item { Carte("Lanceur d'applications", "Liste et recherche des applications installées.", { nav("lanceur") }) }
            item { Carte("Traducteur de texte", "Détection de langue, traduction locale ou cloud.", { nav("texte") }) }
            item { Carte("Traducteur d'écran", "OCR de l'affichage consenti, sous-titres traduits flottants.", { nav("ecran") }) }
            item { Carte("Traducteur vocal", "Micro, traduction, voix traduite segmentée.", { nav("voix") }) }
            item { Carte("Sous-titres", "File des segments traduits, synchronisation, copie.", { nav("sous_titres") }) }
            item { Carte("Langues", "Langue source et langue cible.", { nav("langues") }) }
            item { Carte("Modèles IA", "Téléchargements à la demande, état de Whisper.", { nav("modeles") }) }
            item { Carte("Confidentialité", "Mode strict, historique, explication des permissions.", { nav("confidentialite") }) }
            item { Carte("Performance", "Mode économie, cadence OCR, vitesse de voix.", { nav("performance") }) }
            item { Carte("Appareil", "Capacités réelles mesurées : ABI, RAM, thermique, firmware.", { nav("appareil") }) }
            item { Carte("Historique", "Traductions conservées seulement si vous l'activez.", { nav("historique") }) }
        }
    }
}

/* ---------- 3. barre latérale ---------- */

@Composable
fun EcranBarreLaterale() {
    val ctx = LocalContext.current
    var accord by remember { mutableStateOf(Permissions.overlayGranted(ctx)) }
    var active by remember { mutableStateOf(false) }
    var expliquer by remember { mutableStateOf(false) }
    val demande = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        accord = Permissions.overlayGranted(ctx)
    }
    Page("Barre latérale") { pv ->
        Column(Modifier.fillMaxSize().padding(pv).padding(16.dp), Arrangement.spacedBy(10.dp)) {
            Text("La barre latérale flotte par-dessus les autres applications : poignée au bord droit, bouton ☰ déplaçable, recherche d'applications, accès aux dispositions et aux paramètres.")
            if (expliquer) {
                AlertDialog(
                    onDismissRequest = { expliquer = false },
                    title = { Text("Pourquoi cette autorisation ?") },
                    text = { Text(Permissions.explication("overlay")) },
                    confirmButton = {
                        Button(onClick = { expliquer = false; demande.launch(Permissions.overlayIntent(ctx)) }) { Text("Autoriser") }
                    },
                    dismissButton = { OutlinedButton(onClick = { expliquer = false }) { Text("Plus tard") } },
                )
            }
            Text(if (accord) "Autorisation de superposition : accordée." else "Autorisation de superposition : manquante.",
                style = MaterialTheme.typography.bodySmall)
            if (!accord) Button(onClick = { expliquer = true }) { Text("Comprendre et autoriser") }
            Button(
                enabled = accord,
                onClick = {
                    if (active) ctx.stopService(Intent(ctx, SidebarService::class.java))
                    else ctx.startService(Intent(ctx, SidebarService::class.java))
                    active = !active
                },
            ) { Text(if (active) "Arrêter la barre latérale" else "Lancer la barre latérale") }
            Text("Ouverture et fermeture par glissement sur la poignée orange ou par appui sur ☰ ; glisser ☰ le déplace.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

/* ---------- 4. gestionnaire de fenêtres ---------- */

@Composable
fun EcranFenetres() {
    val ctx = LocalContext.current
    val dispos = remember { LayoutManager.disponibilites(ctx) }
    val apps = remember { LauncherRepository.launchables(ctx) }
    var choix by remember { mutableStateOf(LayoutManager.Disposition.UNE) }
    var selection by remember { mutableStateOf(listOf<String>()) }
    var message by remember { mutableStateOf("") }
    Page("Gestionnaire de fenêtres") { pv ->
        LazyColumn(Modifier.fillMaxSize().padding(pv).padding(horizontal = 12.dp)) {
            item { Text("Dispositions natives disponibles sur cet appareil :", style = MaterialTheme.typography.titleSmall) }
            items(dispos) { d ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.RadioButton(selected = choix == d.disposition,
                        onClick = { choix = d.disposition; message = "" })
                    Column(Modifier.padding(start = 8.dp)) {
                        Text("${d.disposition.libelle} (${d.disposition.zones} zone${if (d.disposition.zones > 1) "s" else ""})")
                        if (!d.possible) Text(d.raison ?: "", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item { Text("Applications (touchez pour choisir, dans l'ordre) :", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp)) }
            items(apps.take(60)) { a ->
                val pris = selection.contains(a.paquet)
                OutlinedButton(
                    onClick = {
                        selection = if (pris) selection - a.paquet
                        else (selection + a.paquet).take(6)
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                ) { Text((if (pris) "✓ " else "") + a.nom) }
            }
            item {
                Button(onClick = {
                    val r = LayoutManager.appliquer(ctx, choix, selection)
                    message = when (r) {
                        is LayoutManager.Resultat.Lance -> "${r.zones} zone(s) ouverte(s) par le système."
                        is LayoutManager.Resultat.PanneauSecours -> "Panneau flottant de secours : ${r.raison}"
                        is LayoutManager.Resultat.Refuse -> r.raison
                    }
                }, modifier = Modifier.padding(vertical = 8.dp)) { Text("Appliquer la disposition") }
            }
            if (message.isNotEmpty()) item { Text(message, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

/* ---------- 5. lanceur ---------- */

@Composable
fun EcranLanceur() {
    val ctx = LocalContext.current
    var q by remember { mutableStateOf("") }
    var resultats by remember { mutableStateOf(LauncherRepository.launchables(ctx)) }
    val scope = rememberCoroutineScope()
    Page("Lanceur d'applications") { pv ->
        Column(Modifier.fillMaxSize().padding(pv).padding(horizontal = 12.dp)) {
            TextField(value = q, onValueChange = { nv ->
                q = nv
                scope.launch { resultats = LauncherRepository.search(ctx, nv) }
            }, placeholder = { Text("Rechercher") }, modifier = Modifier.fillMaxWidth())
            LazyColumn {
                items(resultats) { a ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        AndroidView(factory = { c -> ImageView(c).apply {
                            setImageDrawable(a.icone)
                            layoutParams = android.view.ViewGroup.LayoutParams(96, 96)
                        } }, modifier = Modifier.padding(end = 10.dp))
                        Button(onClick = {
                            LauncherRepository.launchIntent(ctx, a.paquet)?.let {
                                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                runCatching { ctx.startActivity(it) }
                            }
                        }) { Text(a.nom) }
                    }
                }
            }
        }
    }
}

/* ---------- 6. traducteur de texte ---------- */

@Composable
fun EcranTraducteurTexte() {
    val ctx = LocalContext.current
    var source by remember { mutableStateOf("") }
    var sortie by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    Page("Traducteur de texte") { pv ->
        Column(Modifier.fillMaxSize().padding(pv).padding(16.dp), Arrangement.spacedBy(10.dp)) {
            TextField(value = source, onValueChange = { source = it },
                placeholder = { Text("Texte à traduire (russe, chinois, anglais, français…)") },
                modifier = Modifier.fillMaxWidth().weight(1f))
            Button(onClick = {
                scope.launch {
                    val r = app.multiwidgets.ai.translation.traduireSelonRouteur(ctx, source)
                    sortie = r.traduction ?: r.message ?: "Traduction impossible."
                    if (r.traduction != null) app.multiwidgets.ai.history.HistoryRepository(ctx)
                        .ajouter(app.multiwidgets.ai.history.HistoryRepository.Entree(source, r.traduction, r.langueSource, System.currentTimeMillis()))
                }
            }) { Text("Traduire") }
            Text(sortie, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/* ---------- 7. traducteur d'écran ---------- */

@Composable
fun EcranTraducteurEcran() {
    val ctx = LocalContext.current
    var active by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val projection = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK && res.data != null) {
            val i = Intent(ctx, ScreenCaptureService::class.java)
                .putExtra(ScreenCaptureService.EXTRA_CODE, res.resultCode)
                .putExtra(ScreenCaptureService.EXTRA_DATA, res.data)
            runCatching { androidx.core.content.ContextCompat.startForegroundService(ctx, i) }
                .onFailure { message = "Démarrage refusé : ${it.message}" }
            ctx.startService(Intent(ctx, SubtitleOverlayService::class.java))
            active = true
            message = "Capture consentie active. Les fenêtres sécurisées (DRM, banques) restent noires : protection Android, pas un défaut."
        } else {
            message = "Consentement refusé : aucune capture n'a eu lieu."
        }
    }
    Page("Traducteur d'écran") { pv ->
        Column(Modifier.fillMaxSize().padding(pv).padding(16.dp), Arrangement.spacedBy(10.dp)) {
            Text(Permissions.explication("projection"))
            if (!DeviceCapabilities.mediaProjection()) Text("Capture d'écran non disponible avant Android 5.")
            Button(enabled = !active, onClick = {
                scope.launch {
                    if (!Permissions.overlayGranted(ctx)) {
                        message = "Autorisez d'abord la superposition (barre latérale) pour afficher les sous-titres."
                        return@launch
                    }
                    val mpm = ctx.getSystemService(Activity.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    projection.launch(mpm.createScreenCaptureIntent())
                }
            }) { Text("Demander le consentement et commencer") }
            Button(enabled = active, onClick = {
                ctx.stopService(Intent(ctx, ScreenCaptureService::class.java))
                ctx.stopService(Intent(ctx, SubtitleOverlayService::class.java))
                active = false
                message = "Capture arrêtée."
            }) { Text("Arrêter") }
            Text(message, style = MaterialTheme.typography.bodySmall)
        }
    }
}
