package app.multiwidgets.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.multiwidgets.core.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** État des réglages, collecté depuis DataStore et réactif. */
@Composable
fun rememberSettings(): MutableState<Map<String, String>> {
    val ctx = LocalContext.current
    val etat = remember { mutableStateOf(mapOf<String, String>()) }
    LaunchedEffect(Unit) { Settings(ctx).flux.collect { etat.value = it } }
    return etat
}

fun CoroutineScope.maj(ctx: Context, cle: String, valeur: String) =
    launch { Settings(ctx).set(cle, valeur) }
