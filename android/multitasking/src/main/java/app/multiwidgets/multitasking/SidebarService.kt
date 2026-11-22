package app.multiwidgets.multitasking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
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
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import app.multiwidgets.core.LauncherRepository
import kotlin.math.abs

/**
 * Barre latérale flottante : poignée au bord de l'écran, bouton flottant
 * déplaçable, liste des applications installées avec recherche, accès aux
 * paramètres et aux dispositions. Ouverture/fermeture par glissement sur la
 * poignée ou par appui sur le bouton.
 *
 * Construite en vues Android classiques (pas de Compose dans une superposition
 * de service : le cycle de vie Compose y est fragile), volontairement sobre.
 */
class SidebarService : Service() {

    private var wm: WindowManager? = null
    private var bouton: View? = null
    private var poignee: View? = null
    private var panneau: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        poserPoignee()
        poserBouton()
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
        listOf(bouton, poignee, panneau).forEach { runCatching { wm?.removeView(it) } }
        bouton = null; poignee = null; panneau = null
        super.onDestroy()
    }

    private fun params(w: Int, h: Int, x: Int, y: Int) = WindowManager.LayoutParams(
        w, h, x, y,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply { gravity = Gravity.TOP or Gravity.START }

    /** Fine poignée au bord droit : un glissement ou un appui ouvre le panneau. */
    private fun poserPoignee() {
        val v = View(this).apply {
            setBackgroundColor(0x55FF7A1A)
            setOnTouchListener { _, e ->
                if (e.action == MotionEvent.ACTION_UP) basculerPanneau()
                true
            }
        }
        poignee = v
        wm?.addView(v, params(dp(10), dp(140), resources.displayMetrics.widthPixels - dp(10), dp(180)))
    }

    /** Bouton flottant personnalisable : appui = ouvrir/fermer, glisser = déplacer. */
    private fun poserBouton() {
        val v = TextView(this).apply {
            text = "☰"
            textSize = 20f
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setBackgroundColor(0xFF1C2B3A.toInt())
            setTextColor(0xFFFFFFFF.toInt())
        }
        var dx = 0f; var dy = 0f; var ox = 0; var oy = 0; var moved = false
        v.setOnTouchListener { _, e ->
            val p = v.layoutParams as WindowManager.LayoutParams
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { dx = e.rawX; dy = e.rawY; ox = p.x; oy = p.y; moved = false; true }
                MotionEvent.ACTION_MOVE -> {
                    val nx = ox + (e.rawX - dx).toInt(); val ny = oy + (e.rawY - dy).toInt()
                    if (abs(e.rawX - dx) > 12 || abs(e.rawY - dy) > 12) moved = true
                    if (moved) { p.x = nx; p.y = ny; wm?.updateViewLayout(v, p) }
                    true
                }
                MotionEvent.ACTION_UP -> { if (!moved) basculerPanneau(); true }
                else -> false
            }
        }
        bouton = v
        wm?.addView(v, params(WRAP, WRAP, dp(8), dp(80)))
    }

    private fun basculerPanneau() {
        if (panneau != null) { fermerPanneau(); return }
        ouvrirPanneau()
    }

    private fun fermerPanneau() {
        panneau?.let { runCatching { wm?.removeView(it) } }
        panneau = null
    }

    /** Le panneau : recherche, applications, dispositions, paramètres. */
    private fun ouvrirPanneau() {
        val ctx = this
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xF51B2128.toInt())
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        val titre = TextView(ctx).apply {
            text = "Multi-widgets"
            setTextColor(0xFFFFFFFF.toInt()); textSize = 15f
        }
        val recherche = EditText(ctx).apply { hint = "Rechercher une application"; setTextColor(0xFFFFFFFF.toInt()); setHintTextColor(0x88FFFFFF.toInt()) }
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
        val dispositions = Button(ctx).apply {
            text = "Dispositions & fenêtres"
            setOnClickListener {
                // Le gestionnaire de fenêtres complet vit dans l'application ;
                // le panneau y donne accès d'un toucher.
                val i = packageManager.getLaunchIntentForPackage(packageName)
                i?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { startActivity(i) }
                fermerPanneau()
            }
        }
        val fermer = Button(ctx).apply { text = "Fermer"; setOnClickListener { fermerPanneau() } }

        root.addView(titre); root.addView(recherche); root.addView(liste, LinearLayout.LayoutParams(MATCH, 0, 1f))
        root.addView(dispositions); root.addView(fermer)
        panneau = root
        wm?.addView(root, params(dp(280), dp(460),
            resources.displayMetrics.widthPixels - dp(290), dp(60)))
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
