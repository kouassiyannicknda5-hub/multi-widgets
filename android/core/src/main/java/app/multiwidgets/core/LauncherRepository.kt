package app.multiwidgets.core

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build

/** Applications installées et lançables : source du lanceur et de la barre latérale. */
object LauncherRepository {

    data class App(val nom: String, val paquet: String, val icone: Drawable?)

    fun launchables(context: Context): List<App> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            PackageManager.ResolveInfoFlags.of(0L)
        else null
        val infos = if (flags != null) pm.queryIntentActivities(intent, flags)
        else @Suppress("DEPRECATION") pm.queryIntentActivities(intent, 0)
        return infos.mapNotNull { ri ->
            val pa = ri.activityInfo?.packageName ?: return@mapNotNull null
            App(
                nom = ri.loadLabel(pm).toString(),
                paquet = pa,
                icone = runCatching { ri.loadIcon(pm) }.getOrNull(),
            )
        }.distinctBy { it.paquet }.sortedBy { it.nom.lowercase() }
    }

    fun search(context: Context, query: String): List<App> {
        val q = query.trim().lowercase()
        val all = launchables(context)
        return if (q.isEmpty()) all else all.filter { it.nom.lowercase().contains(q) }
    }

    fun launchIntent(context: Context, paquet: String): Intent? =
        context.packageManager.getLaunchIntentForPackage(paquet)
}
