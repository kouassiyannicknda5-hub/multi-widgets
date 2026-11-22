package app.multiwidgets.ui

import android.content.Intent
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
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.multiwidgets.ai.history.HistoryRepository
import app.multiwidgets.ai.speech.AndroidAsr
import app.multiwidgets.ai.speech.WhisperGate
import app.multiwidgets.ai.subtitles.Segment
import app.multiwidgets.ai.subtitles.SubtitleBus
import app.multiwidgets.ai.subtitles.SubtitleOverlayService
import app.multiwidgets.ai.translation.TranslationEngine
import app.multiwidgets.ai.voice.TtsEngine
import app.multiwidgets.core.DeviceCapabilities
import app.multiwidgets.core.Permissions
import kotlinx.coroutines.launch
import java.util.Locale

/* ---------- 8. traducteur vocal ---------- */

@Composable
fun EcranTraducteurVocal() {
    val ctx = LocalContext.current
    var partiel by remember { mutableStateOf("") }
    var sortie by remember { mutableStateOf("") }
    var ecoute by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val micro = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (!ok) { sortie = "Micro refusé : aucune écoute n'a eu lieu."; return@rememberLauncherForActivityResult }
        ecoute = true
        scope.launch {
            val tts = TtsEngine(ctx).also { it.initialiser() }
            val texte = AndroidAsr(ctx).ecouter(Locale.getDefault().language) { partiel = it }
            ecoute = false
            if (texte.isBlank()) { sortie = "Rien n'a été entendu."; tts.libérer(); return@launch }
            val r = app.multiwidgets.ai.translation.traduireSelonRouteur(ctx, texte)
            sortie = "« $texte » (${r.langueSource}) → " + (r.traduction ?: r.message ?: "indisponible")
            r.traduction?.let { t ->
                SubtitleBus.publier(Segment(texte, r.langueSource, t))
                val settings = app.multiwidgets.core.Settings(ctx)
                tts.dire(t, Locale(settings.get("langue_cible").ifBlank { "fr" }),
                    settings.get("tts_genre"), settings.get("tts_vitesse").toFloatOrNull() ?: 1f)
                HistoryRepository(ctx).ajouter(HistoryRepository.Entree(texte, t, r.langueSource, System.currentTimeMillis()))
            }
        }
    }
    Page("Traducteur vocal") { pv ->
        Column(Modifier.fillMaxSize().padding(pv).padding(16.dp), Arrangement.spacedBy(10.dp)) {
            Text(Permissions.explication("mic"))
            Button(enabled = !ecoute, onClick = {
                if (Permissions.micGranted(ctx)) micro.launch(Permissions.MIC)
                else micro.launch(Permissions.MIC)
            }) { Text(if (ecoute) "Écoute en cours…" else "Parler, puis traduire et doubler") }
            if (partiel.isNotEmpty()) Text(partiel, style = MaterialTheme.typography.bodyMedium)
            Text(sortie, style = MaterialTheme.typography.bodyLarge)
            Text("Whisper tiny prendra le relais lorsque son verrou d'honnêteté sera levé sur cet appareil (écran Modèles IA).",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

/* ---------- 9. sous-titres ---------- */

@Composable
fun EcranSousTitres() {
    val ctx = LocalContext.current
    val segments by SubtitleBus.segments.collectAsState()
    var overlay by remember { mutableStateOf(false) }
    Page("Sous-titres") { pv ->
        Column(Modifier.fillMaxSize().padding(pv).padding(16.dp), Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Fenêtre flottante : ")
                Switch(checked = overlay, onCheckedChange = { on ->
                    overlay = on
                    if (on) ctx.startService(Intent(ctx, SubtitleOverlayService::class.java))
                    else ctx.stopService(Intent(ctx, SubtitleOverlayService::class.java))
                })
            }
            Button(onClick = { SubtitleBus.vider() }) { Text("Vider la file") }
            LazyColumn {
                items(segments.reversed()) { s ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(s.traduction, style = MaterialTheme.typography.bodyLarge)
                        Text("[${s.langue}] ${s.source}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

/* ---------- 10. langues ---------- */

@Composable
fun EcranLangues() {
    val ctx = LocalContext.current
    val regl = rememberSettings()
    val scope = rememberCoroutineScope()
    val codes = listOf("auto", "fr", "en", "zh", "ja", "ko", "ru", "es", "de", "ar", "hi", "pt", "it")
    Page("Paramètres de langue") { pv ->
        Column(Modifier.fillMaxSize().padding(pv).padding(16.dp), Arrangement.spacedBy(10.dp)) {
            Text("Langue source : ${regl.value["langue_source"]}")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                codes.take(6).forEach { c ->
                    OutlinedButton(onClick = { scope.maj(ctx, "langue_source", c) }) { Text(c) }
                }
            }
            Text("Langue cible : ${regl.value["langue_cible"]}")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("fr", "en", "es", "de", "pt", "zh").forEach { c ->
                    OutlinedButton(onClick = { scope.maj(ctx, "langue_cible", c) }) { Text(c) }
                }
            }
            Text("La détection automatique reste prioritaire pour la source ; le choix source force le sens de traduction.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

/* ---------- 11. modèles IA ---------- */

@Composable
fun EcranModeles() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var etat by remember { mutableStateOf(TranslationEngine.langues().take(20).map { it to TranslationEngine.modelePresent(it) }) }
    val gate = remember { WhisperGate.evaluer(ctx, WhisperGate.fichierModele(ctx).exists(), false) }
    Page("Gestion des modèles IA") { pv ->
        LazyColumn(Modifier.fillMaxSize().padding(pv).padding(horizontal = 12.dp)) {
            item { Text("Traduction hors ligne (ML Kit) — téléchargement à la demande :", style = MaterialTheme.typography.titleSmall) }
            items(etat) { (langue, present) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(langue, modifier = Modifier.padding(end = 10.dp))
                    Text(if (present) "installé" else "absent", style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = {
                        scope.launch {
                            if (present) TranslationEngine.supprimer(langue) else TranslationEngine.telecharger(langue)
                            etat = TranslationEngine.langues().take(20).map { it to TranslationEngine.modelePresent(it) }
                        }
                    }) { Text(if (present) "Supprimer" else "Télécharger") }
                }
            }
            item {
                Column(Modifier.padding(top = 14.dp)) {
                    Text("Whisper (ASR local)", style = MaterialTheme.typography.titleSmall)
                    Text(if (gate.disponible) "Disponible." else "Verrouillé, car : " + gate.raisons.joinToString(" "),
                        style = MaterialTheme.typography.bodySmall)
                    Text("Gemma E2B quantifié : option expérimentale NON intégrée — aucun benchmark réel n'a été passé.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/* ---------- 12. confidentialité ---------- */

@Composable
fun EcranConfidentialite() {
    val ctx = LocalContext.current
    val regl = rememberSettings()
    val scope = rememberCoroutineScope()
    val histo = remember { HistoryRepository(ctx) }
    Page("Confidentialité") { pv ->
        Column(Modifier.fillMaxSize().padding(pv).padding(16.dp), Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = regl.value["confidentialite_stricte"] == "true",
                    onCheckedChange = { scope.maj(ctx, "confidentialite_stricte", it.toString()) })
                Text(" Mode strict : rien ne quitte l'appareil")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = regl.value["historique"] == "true",
                    onCheckedChange = { on ->
                        scope.maj(ctx, "historique", on.toString())
                        histo.activer(on)
                    })
                Text(" Historique des traductions (désactivé par défaut)")
            }
            Button(onClick = { histo.toutSupprimer() }) { Text("Supprimer tout l'historique") }
            Text(
                "Aucune capture d'écran ni écoute sans consentement système explicite, à chaque session. " +
                "Les services d'accessibilité ne sont pas utilisés. Les clés API restent dans le stockage privé " +
                "de l'application, jamais dans le code. Les fenêtres sécurisées restent inaccessibles, par design Android.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/* ---------- 13. performance ---------- */

@Composable
fun EcranPerformance() {
    val ctx = LocalContext.current
    val regl = rememberSettings()
    val scope = rememberCoroutineScope()
    Page("Performance") { pv ->
        Column(Modifier.fillMaxSize().padding(pv).padding(16.dp), Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = regl.value["eco"] == "true", onCheckedChange = { scope.maj(ctx, "eco", it.toString()) })
                Text(" Mode économie (analyses espacées, modèles légers)")
            }
            TextField(
                value = regl.value["ocr_frequence_ms"] ?: "2500",
                onValueChange = { if (it.all(Char::isDigit)) scope.maj(ctx, "ocr_frequence_ms", it) },
                label = { Text("Cadence OCR (ms)") },
            )
            TextField(
                value = regl.value["tts_vitesse"] ?: "1.0",
                onValueChange = { scope.maj(ctx, "tts_vitesse", it) },
                label = { Text("Vitesse de la voix (0,5 – 2)") },
            )
            TextField(
                value = regl.value["sous_titres_sync_ms"] ?: "0",
                onValueChange = { scope.maj(ctx, "sous_titres_sync_ms", it) },
                label = { Text("Décalage des sous-titres (ms)") },
            )
            Text("Règles mémoire : un seul gros modèle à la fois, libération après traitement, buffers bornés, " +
                "langues téléchargées à la demande, rien sur le thread principal.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

/* ---------- 14. appareil ---------- */

@Composable
fun EcranAppareil() {
    val ctx = LocalContext.current
    val cap = remember { DeviceCapabilities.snapshot(ctx) }
    Page("Informations sur l'appareil") { pv ->
        Column(Modifier.fillMaxSize().padding(pv).padding(16.dp), Arrangement.spacedBy(6.dp)) {
            Text("Fabricant / modèle : ${cap.manufacturer} ${cap.model}")
            Text("Android SDK : ${cap.androidSdk}")
            Text("Architectures : ${cap.abis.joinToString()} (ARM64 : ${cap.arm64})")
            Text("RAM totale / libre : ${cap.totalRamMb} / ${cap.availableRamMb} Mo")
            Text("Fenêtres libres (freeform) : ${cap.freeformWindows}")
            Text("Écran partagé natif : ${cap.splitScreen}")
            Text("Capture d'écran consentie : ${cap.mediaProjection}")
            Text("Capture du son d'autres apps : ${cap.audioPlaybackCapture} (soumise aux politiques de l'app source)")
            Text("Thermique : ${cap.thermalHeadroom}")
            Text("Ces valeurs décident des fonctions proposées : rien n'est promis sans capacité vérifiée.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

/* ---------- 15. historique ---------- */

@Composable
fun EcranHistorique() {
    val ctx = LocalContext.current
    val histo = remember { HistoryRepository(ctx) }
    var entrees by remember { mutableStateOf(histo.lister()) }
    Page("Historique") { pv ->
        Column(Modifier.fillMaxSize().padding(pv).padding(16.dp), Arrangement.spacedBy(8.dp)) {
            Button(onClick = { histo.toutSupprimer(); entrees = emptyList() }) { Text("Tout supprimer") }
            if (entrees.isEmpty()) Text("Aucun historique : il n'est écrit que si vous l'activez dans Confidentialité.")
            LazyColumn {
                items(entrees) { e ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(e.traduction, style = MaterialTheme.typography.bodyLarge)
                        Text("[${e.langue}] ${e.source}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
