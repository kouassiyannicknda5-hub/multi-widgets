package app.multiwidgets.multitasking

import android.content.Context
import org.json.JSONArray

/**
 * Catalogue d'APPLICATIONS du widget d'écran d'accueil et de la barre latérale
 * (SharedPreferences, JSON). Chaque entrée référence une application installée
 * via son [paquet] et s'ouvre directement. AUCUN lien web n'est utilisé : le
 * champ [u] n'est conservé que pour relire d'anciennes sauvegardes ; il n'ouvre
 * plus rien. Une entrée sans [paquet] est simplement ignorée.
 */
object Store {

    data class App(val g: String, val n: String, val u: String, val paquet: String = "")

    private const val KEY = "catalog"

    private fun prefs(context: Context) = context.getSharedPreferences("mw", Context.MODE_PRIVATE)

    fun load(context: Context): MutableList<App> {
        val json = prefs(context).getString(KEY, null)
        if (json == null) return defaults().also { save(context, it) }
        return try {
            val a = JSONArray(json)
            MutableList(a.length()) { i ->
                val o = a.getJSONObject(i)
                App(o.optString("g", "•"), o.optString("n", ""), o.optString("u", ""), o.optString("paquet", ""))
            }
        } catch (e: Exception) {
            defaults()
        }
    }

    fun save(context: Context, apps: List<App>) {
        val a = JSONArray()
        apps.forEach { app ->
            a.put(org.json.JSONObject().apply {
                put("g", app.g); put("n", app.n); put("u", app.u); put("paquet", app.paquet)
            })
        }
        prefs(context).edit().putString(KEY, a.toString()).apply()
        Widgets.update(context)
    }

    fun defaults(): MutableList<App> = mutableListOf(
        App("💬", "WhatsApp", "", "com.whatsapp"),
        App("🐦", "X", "", "com.twitter.android"),
        App("👥", "Facebook", "", "com.facebook.katana"),
        App("🔎", "Google", "", "com.google.android.googlequicksearchbox"),
        App("📷", "Instagram", "", "com.instagram.android"),
        App("▶", "YouTube", "", "com.google.android.youtube"),
        App("✉", "Gmail", "", "com.google.android.gm"),
        App("📍", "Maps", "", "com.google.android.apps.maps"),
    )
}
