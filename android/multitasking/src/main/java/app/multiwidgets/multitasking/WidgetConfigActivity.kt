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
import app.multiwidgets.core.LauncherRepository

/** /app natif : ajouter vos applications installées au widget et à la barre.
 *  Aucun lien web — uniquement des applications du téléphone. */
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
            text = "Applications du widget d'écran d'accueil et de la barre. Ajoutez vos applications installées : elles s'ouvriront directement, sans lien web. Toucher une ligne pour la retirer."
            textSize = 13f
            setPadding(0, 0, 0, 20)
        })
        apps = Store.load(this)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        val liste = ListView(this).apply { setAdapter(this@WidgetConfigActivity.adapter) }
        root.addView(liste, LinearLayout.LayoutParams(-1, 0, 1f))

        root.addView(Button(this).apply {
            text = "➕ Ajouter une application du téléphone"
            setOnClickListener { choisirApp() }
        })

        liste.setOnItemClickListener { _, _, i, _ ->
            val a = apps.getOrNull(i) ?: return@setOnItemClickListener
            AlertDialog.Builder(this)
                .setTitle("Retirer ${a.n} ?")
                .setMessage("L'application disparaîtra du widget et de la barre latérale.")
                .setPositiveButton("Retirer") { _, _ ->
                    apps.removeAt(i); Store.save(this, apps); refill()
                }
                .setNegativeButton("Annuler", null)
                .show()
        }
        setContentView(root)
        refill()
    }

    /** Sélecteur des applications installées sur le téléphone : toucher une
     *  app l'ajoute au widget et à la barre, avec son paquet (ouverture native). */
    private fun choisirApp() {
        val ctx = this
        val toutes = LauncherRepository.launchables(ctx)
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 20, 28, 8)
        }
        val recherche = EditText(ctx).apply { hint = "Rechercher une application installée" }
        val liste = ListView(ctx)
        box.addView(recherche)
        box.addView(liste, LinearLayout.LayoutParams(-1, (360 * resources.displayMetrics.density).toInt()))
        var filtrees = toutes
        val ad = ArrayAdapter(ctx, android.R.layout.simple_list_item_1, filtrees.map { it.nom })
        liste.adapter = ad
        recherche.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                val q = s?.toString()?.trim()?.lowercase() ?: ""
                filtrees = if (q.isEmpty()) toutes else toutes.filter { it.nom.lowercase().contains(q) }
                ad.clear(); ad.addAll(filtrees.map { it.nom }); ad.notifyDataSetChanged()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        val dlg = AlertDialog.Builder(ctx)
            .setTitle("Applications du téléphone")
            .setView(box)
            .setNegativeButton("Fermer", null)
            .create()
        liste.setOnItemClickListener { _, _, i, _ ->
            val app = filtrees.getOrNull(i) ?: return@setOnItemClickListener
            if (apps.none { it.paquet == app.paquet }) {
                apps.add(Store.App("", app.nom, "", app.paquet))
                Store.save(ctx, apps)
                refill()
            }
            dlg.dismiss()
        }
        dlg.show()
    }

    private fun refill() {
        adapter.clear()
        apps.forEach { adapter.add("${if (it.g.isEmpty()) "•" else it.g}   ${it.n}") }
        adapter.notifyDataSetChanged()
    }
}
