package app.multiwidgets.multitasking

import android.content.Context
import org.json.JSONArray

/**
 * Catalogue d'applications du widget d'écran d'accueil et de la barre latérale
 * (SharedPreferences, JSON). Une entrée référence de préférence une
 * APPLICATION INSTALLÉE via son [paquet] : elle s'ouvre alors directement, sans
 * lien web. L'URL [u] ne sert que de repli si l'application n'est pas installée.
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
        App("💬", "WhatsApp", "https://web.whatsapp.com", "com.whatsapp"),
        App("🐦", "X", "https://x.com", "com.twitter.android"),
        App("👥", "Facebook", "https://m.facebook.com", "com.facebook.katana"),
        App("🔎", "Google", "https://www.google.com", "com.google.android.googlequicksearchbox"),
        App("📷", "Instagram", "https://www.instagram.com", "com.instagram.android"),
        App("▶", "YouTube", "https://m.youtube.com", "com.google.android.youtube"),
        App("✉", "Gmail", "https://mail.google.com", "com.google.android.gm"),
        App("📍", "Maps", "https://maps.google.com", "com.google.android.apps.maps"),
    )
}
