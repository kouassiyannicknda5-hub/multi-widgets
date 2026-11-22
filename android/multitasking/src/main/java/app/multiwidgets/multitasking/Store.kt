package app.multiwidgets.multitasking

import android.content.Context
import org.json.JSONArray

/** Catalogue d'applications du widget d'écran d'accueil (SharedPreferences, JSON). */
object Store {

    data class App(val g: String, val n: String, val u: String)

    private const val KEY = "catalog"

    private fun prefs(context: Context) = context.getSharedPreferences("mw", Context.MODE_PRIVATE)

    fun load(context: Context): MutableList<App> {
        val json = prefs(context).getString(KEY, null)
        if (json == null) return defaults().also { save(context, it) }
        return try {
            val a = JSONArray(json)
            MutableList(a.length()) { i ->
                val o = a.getJSONObject(i)
                App(o.optString("g", "•"), o.optString("n", ""), o.optString("u", ""))
            }
        } catch (e: Exception) {
            defaults()
        }
    }

    fun save(context: Context, apps: List<App>) {
        val a = JSONArray()
        apps.forEach { app ->
            a.put(org.json.JSONObject().apply {
                put("g", app.g); put("n", app.n); put("u", app.u)
            })
        }
        prefs(context).edit().putString(KEY, a.toString()).apply()
        Widgets.update(context)
    }

    fun defaults(): MutableList<App> = mutableListOf(
        App("💬", "WhatsApp", "https://web.whatsapp.com"),
        App("🐦", "X", "https://x.com"),
        App("👥", "Facebook", "https://m.facebook.com"),
        App("🔎", "Google", "https://www.google.com"),
        App("📷", "Instagram", "https://www.instagram.com"),
        App("▶", "YouTube", "https://m.youtube.com"),
        App("✉", "Gmail", "https://mail.google.com"),
        App("📍", "Maps", "https://maps.google.com"),
    )
}
