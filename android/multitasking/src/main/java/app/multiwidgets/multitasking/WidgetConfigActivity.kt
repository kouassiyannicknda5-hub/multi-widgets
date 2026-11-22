package app.multiwidgets.multitasking

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView

/** /app natif : choisir les applications du widget d'écran d'accueil. */
class WidgetConfigActivity : Activity() {

    private lateinit var apps: MutableList<Store.App>
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        title = "Multi-Widgets — applications du widget"
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
        }
        root.addView(TextView(this).apply {
            text = "Applications du widget d'écran d'accueil. Toucher une ligne pour la retirer."
            textSize = 13f
            setPadding(0, 0, 0, 20)
        })
        apps = Store.load(this)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        val liste = ListView(this).apply { setAdapter(this@WidgetConfigActivity.adapter) }
        root.addView(liste, LinearLayout.LayoutParams(-1, 0, 1f))

        val ligne = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val g = EditText(this).apply { hint = "🎵"; setWidth(140) }
        val n = EditText(this).apply { hint = "Nom (ex. Deezer)" }
        ligne.addView(g); ligne.addView(n, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(ligne)
        val u = EditText(this).apply { hint = "Adresse (ex. https://www.deezer.com)" }
        root.addView(u)
        root.addView(Button(this).apply {
            text = "Ajouter au widget"
            setOnClickListener {
                val nom = n.text.toString().trim()
                var url = u.text.toString().trim()
                if (nom.isEmpty() || url.isEmpty()) return@setOnClickListener
                if (!url.startsWith("http://", true) && !url.startsWith("https://", true)) url = "https://$url"
                apps.add(Store.App(g.text.toString().trim(), nom, url))
                Store.save(this@WidgetConfigActivity, apps)
                refill()
                g.text.clear(); n.text.clear(); u.text.clear()
            }
        })
        liste.setOnItemClickListener { _, _, i, _ ->
            val a = apps.getOrNull(i) ?: return@setOnItemClickListener
            AlertDialog.Builder(this)
                .setTitle("Retirer ${a.n} ?")
                .setMessage("L'application disparaîtra du widget de l'écran d'accueil.")
                .setPositiveButton("Retirer") { _, _ ->
                    apps.removeAt(i); Store.save(this, apps); refill()
                }
                .setNegativeButton("Annuler", null)
                .show()
        }
        setContentView(root)
        refill()
    }

    private fun refill() {
        adapter.clear()
        apps.forEach { adapter.add("${if (it.g.isEmpty()) "•" else it.g}   ${it.n}") }
        adapter.notifyDataSetChanged()
    }
}
