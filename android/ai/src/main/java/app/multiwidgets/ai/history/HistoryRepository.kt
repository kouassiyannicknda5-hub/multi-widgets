package app.multiwidgets.ai.history

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Historique FACULTATIF des traductions : désactivé par défaut.
 * Stocké en fichier JSON privé, borné, supprimable d'un geste.
 * Aucune conversation vocale, aucun enregistrement audio n'y est écrit.
 */
class HistoryRepository(context: Context) {

    private val fichier = File(context.filesDir, "historique.json")

    data class Entree(val source: String, val traduction: String, val langue: String, val ts: Long)

    fun activer(actif: Boolean) {
        if (!actif) fichier.delete()
    }

    fun ajouter(e: Entree) {
        if (!fichier.exists()) return   // historique désactivé : rien n'est écrit
        val a = JSONArray(fichier.readText().ifBlank { "[]" })
        a.put(JSONObject().apply {
            put("source", e.source); put("traduction", e.traduction)
            put("langue", e.langue); put("ts", e.ts)
        })
        val borne = JSONArray()
        val liste = MutableList(a.length()) { a.getJSONObject(it) }.takeLast(200)
        liste.forEach { borne.put(it) }
        fichier.writeText(borne.toString())
    }

    fun lister(): List<Entree> {
        if (!fichier.exists()) return emptyList()
        val a = JSONArray(fichier.readText().ifBlank { "[]" })
        return MutableList(a.length()) { i ->
            val o = a.getJSONObject(i)
            Entree(o.optString("source"), o.optString("traduction"), o.optString("langue"), o.optLong("ts"))
        }.reversed()
    }

    fun toutSupprimer() = runCatching { fichier.delete() }
}
