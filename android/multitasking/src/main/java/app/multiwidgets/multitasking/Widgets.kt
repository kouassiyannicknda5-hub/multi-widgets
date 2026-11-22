package app.multiwidgets.multitasking

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews

/**
 * Widget d'écran d'accueil natif : dessiné par le lanceur Android, en dehors
 * de toute application ouverte. Un toucher sur une case ouvre directement
 * l'application cible dans le navigateur : Multi-widgets ne s'ouvre jamais.
 */
class Widgets : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { mgr.updateAppWidget(it, build(context)) }
    }

    companion object {
        private val CELLES = intArrayOf(
            R.id.cell0, R.id.cell1, R.id.cell2, R.id.cell3,
            R.id.cell4, R.id.cell5, R.id.cell6, R.id.cell7,
        )

        fun update(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            mgr.getAppWidgetIds(ComponentName(context, Widgets::class.java))
                .forEach { mgr.updateAppWidget(it, build(context)) }
        }

        private fun build(context: Context): RemoteViews {
            val v = RemoteViews(context.packageName, R.layout.widget)
            val apps = Store.load(context)
            CELLES.forEachIndexed { i, id ->
                val a = apps.getOrNull(i)
                if (a != null && a.u.isNotEmpty()) {
                    v.setViewVisibility(id, View.VISIBLE)
                    v.setTextViewText(id, "${if (a.g.isEmpty()) "•" else a.g}\n${a.n}")
                    val it = Intent(Intent.ACTION_VIEW, Uri.parse(a.u))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    v.setOnClickPendingIntent(
                        id,
                        PendingIntent.getActivity(
                            context, i, it,
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    )
                } else {
                    v.setViewVisibility(id, View.GONE)
                }
            }
            return v
        }
    }
}
