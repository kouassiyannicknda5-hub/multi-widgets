package app.multiwidgets.multitasking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import app.multiwidgets.core.LauncherRepository

/**
 * Barre latérale flottante, déplaçable et permanente.
 *
 * - Une poignée « ⠿ » déplace toute la barre ; la position est mémorisée puis
 *   restaurée au lancement suivant.
 * - Les applications ajoutées depuis le téléphone (catalogue [Store]) sont
 *   épinglées sur la barre : un toucher OUVRE L'APPLICATION NATIVE, jamais un
 *   lien web — le lien ne sert que de repli si l'app n'est pas installée.
 * - Le bouton « ☰ » ouvre un panneau listant TOUTES les applications installées
 *   avec recherche : un appui ouvre l'app, un appui long l'épingle sur la barre
 *   et dans le widget d'accueil.
 *
 * Service au premier plan (type specialUse) : la barre reste visible par-dessus
 * les autres applications, même quand Multi-widgets est quitté.
 *
 * Construite en vues Android classiques (pas de Compose dans une superposition
 * de service : le cycle de vie Compose y est fragile), volontairement sobre.
 */
class SidebarService : Service() {

    private var wm: WindowManager? = null
    private var barre: View? = null
    private var panneau: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        poserBarre()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == QUITTER) { stopSelf(); return START_NOT_STICKY }
        demarrerRappel()
        return START_STICKY
    }

    /**
     * Service au premier plan : c'est ce qui permet à la barre de rester
     * affichée par-dessus les autres applications quand l'utilisateur quitte
     * Multi-widgets. Depuis Android 14, une superposition de ce type doit
     * déclarer le type « specialUse » et l'application doit demander
     * l'autorisation de notifications pour que le rappel soit visible.
     */
    private fun demarrerRappel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CANAL, "Barre latérale", NotificationManager.IMPORTANCE_LOW)
        )
        val ouvrir = packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val builder = Notification.Builder(this, CANAL)
            .setContentTitle("Barre latérale Multi-widgets")
            .setContentText("Flotte par-dessus vos applications. Touchez pour ouvrir.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
        if (ouvrir != null) {
            builder.setContentIntent(
                PendingIntent.getActivity(this, 0, ouvrir, PendingIntent.FLAG_IMMUTABLE)
            )
        }
        builder.addAction(
            Notification.Action.Builder(
                android.R.drawable.ic_dialog_info, "Quitter",
                PendingIntent.getService(
                    this, 1,
                    Intent(this, SidebarService::class.java).setAction(QUITTER),
                    PendingIntent.FLAG_IMMUTABLE
                )
            ).build()
        )
        val n = builder.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    override fun onDestroy() {
        listOf(barre, panneau).forEach { runCatching { wm?.removeView(it) } }
        barre = null; panneau = null
        super.onDestroy()
    }

    private fun params(w: Int, h: Int, x: Int, y: Int) = WindowManager.LayoutParams(
        w, h, x, y,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply { gravity = Gravity.TOP or Gravity.START }

    private fun prefs() = getSharedPreferences("mw", Context.MODE_PRIVATE)

    /** La barre : poignée de déplacement + applications épinglées + bouton ☰. */
    private fun poserBarre() {
        val ctx = this
        val conteneur = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xF51B2128.toInt())
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        val poignee = TextView(ctx).apply {
            text = "⠿"; textSize = 15f; gravity = Gravity.CENTER
            setTextColor(0xFF9AA8D8.toInt()); setPadding(dp(6), dp(2), dp(6), dp(6))
        }
        conteneur.addView(poignee, LinearLayout.LayoutParams(MATCH, WRAP))

        Store.load(ctx).filter { it.paquet.isNotEmpty() }.take(6).forEach { a ->
            conteneur.addView(
                vueApp(a),
                LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(4) }
            )
        }

        val menu = TextView(ctx).apply {
            text = "☰"; textSize = 20f; gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt()); setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        menu.setOnClickListener { basculerPanneau() }
        conteneur.addView(menu, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(4) })

        var dx = 0f; var dy = 0f; var ox = 0; var oy = 0
        poignee.setOnTouchListener { _, e ->
            val p = conteneur.layoutParams as WindowManager.LayoutParams
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { dx = e.rawX; dy = e.rawY; ox = p.x; oy = p.y; true }
                MotionEvent.ACTION_MOVE -> {
                    p.x = ox + (e.rawX - dx).toInt(); p.y = oy + (e.rawY - dy).toInt()
                    wm?.updateViewLayout(conteneur, p); true
                }
                MotionEvent.ACTION_UP -> {
                    prefs().edit().putInt("barre_x", p.x).putInt("barre_y", p.y).apply(); true
                }
                else -> false
            }
        }

        barre = conteneur
        wm?.addView(
            conteneur,
            params(dp(58), WRAP, prefs().getInt("barre_x", dp(8)), prefs().getInt("barre_y", dp(120)))
        )
    }

    /** Une case de la barre : icône réelle de l'app si disponible, sinon glyphe. */
    private fun vueApp(a: Store.App): View {
        val icone = if (a.paquet.isNotEmpty())
            runCatching { packageManager.getApplicationIcon(a.paquet) }.getOrNull() else null
        val v: View = if (icone != null) {
            ImageView(this).apply { setImageDrawable(icone); setPadding(dp(6), dp(6), dp(6), dp(6)) }
        } else {
            TextView(this).apply {
                text = if (a.g.isNotEmpty()) a.g else a.n.take(1).uppercase()
                textSize = 18f; gravity = Gravity.CENTER; setTextColor(0xFFFFFFFF.toInt())
                setPadding(dp(6), dp(8), dp(6), dp(8))
            }
        }
        v.setBackgroundColor(0xFF232B63.toInt())
        v.contentDescription = a.n
        v.setOnClickListener { lancer(a) }
        return v
    }

    /** Ouvre l'application native installée. Aucun lien web : si l'app n'est
     *  pas installée, rien ne s'ouvre. */
    private fun lancer(a: Store.App) {
        val i = if (a.paquet.isNotEmpty()) LauncherRepository.launchIntent(this, a.paquet) else null
        if (i != null) runCatching { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    private fun reconstruireBarre() {
        barre?.let { runCatching { wm?.removeView(it) } }
        barre = null
        poserBarre()
    }

    private fun basculerPanneau() {
        if (panneau != null) { fermerPanneau(); return }
        ouvrirPanneau()
    }

    private fun fermerPanneau() {
        panneau?.let { runCatching { wm?.removeView(it) } }
        panneau = null
    }

    /** Le panneau : toutes les applications installées, avec recherche. */
    private fun ouvrirPanneau() {
        val ctx = this
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xF51B2128.toInt())
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        val titre = TextView(ctx).apply {
            text = "Applications du téléphone"
            setTextColor(0xFFFFFFFF.toInt()); textSize = 15f
        }
        val aide = TextView(ctx).apply {
            text = "Toucher = ouvrir · Toucher longtemps = épingler sur la barre"
            setTextColor(0xFF9AA8D8.toInt()); textSize = 11f
        }
        val recherche = EditText(ctx).apply {
            hint = "Rechercher une application"
            setTextColor(0xFFFFFFFF.toInt()); setHintTextColor(0x88FFFFFF.toInt())
        }
        val liste = ListView(ctx)
        val apps = LauncherRepository.launchables(ctx)
        val adapter = ArrayAdapter(ctx, android.R.layout.simple_list_item_1, apps.map { it.nom })
        liste.adapter = adapter
        recherche.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                val q = s?.toString() ?: ""
                val res = if (q.isBlank()) apps else apps.filter { it.nom.contains(q, true) }
                adapter.clear(); adapter.addAll(res.map { it.nom }); adapter.notifyDataSetChanged()
                liste.tag = res
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        liste.tag = apps
        liste.setOnItemClickListener { _, _, i, _ ->
            val res = liste.tag as? List<LauncherRepository.App> ?: apps
            val app = res.getOrNull(i) ?: return@setOnItemClickListener
            LauncherRepository.launchIntent(ctx, app.paquet)?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { startActivity(it) }
            }
            fermerPanneau()
        }
        liste.setOnItemLongClickListener { _, _, i, _ ->
            val res = liste.tag as? List<LauncherRepository.App> ?: apps
            val app = res.getOrNull(i) ?: return@setOnItemLongClickListener false
            val cat = Store.load(ctx)
            if (cat.none { it.paquet == app.paquet }) {
                cat.add(Store.App("", app.nom, "", app.paquet))
                Store.save(ctx, cat)
                reconstruireBarre()
                Toast.makeText(ctx, "${app.nom} épinglée sur la barre et le widget", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(ctx, "${app.nom} est déjà épinglée", Toast.LENGTH_SHORT).show()
            }
            true
        }
        val dispositions = Button(ctx).apply {
            text = "Dispositions & fenêtres"
            setOnClickListener {
                val i = packageManager.getLaunchIntentForPackage(packageName)
                i?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { startActivity(i) }
                fermerPanneau()
            }
        }
        val fermer = Button(ctx).apply { text = "Fermer"; setOnClickListener { fermerPanneau() } }

        root.addView(titre); root.addView(aide); root.addView(recherche)
        root.addView(liste, LinearLayout.LayoutParams(MATCH, 0, 1f))
        root.addView(dispositions); root.addView(fermer)
        panneau = root
        wm?.addView(
            root,
            params(dp(280), dp(460), resources.displayMetrics.widthPixels - dp(290), dp(60))
        )
    }

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density).toInt()

    private companion object {
        const val WRAP = WindowManager.LayoutParams.WRAP_CONTENT
        const val MATCH = WindowManager.LayoutParams.MATCH_PARENT
        const val CANAL = "barre"
        const val NOTIF_ID = 42
        const val QUITTER = "app.multiwidgets.multitasking.action.QUITTER"
    }
}
