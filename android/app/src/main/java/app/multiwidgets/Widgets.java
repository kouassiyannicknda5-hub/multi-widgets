package app.multiwidgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

import java.util.List;

/** Le widget d'écran d'accueil.
 *
 *  Il est dessiné par le lanceur Android lui-même, en dehors de toute
 *  application ouverte : c'est ce que le Web ne peut pas faire.
 *  Toucher une case ouvre directement l'application correspondante dans
 *  le navigateur (ACTION_VIEW) — Multi-Widgets ne s'ouvre jamais. */
public class Widgets extends AppWidgetProvider {

    private static final int[] CELLS = {
            R.id.cell0, R.id.cell1, R.id.cell2, R.id.cell3,
            R.id.cell4, R.id.cell5, R.id.cell6, R.id.cell7
    };

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) m.updateAppWidget(id, build(c));
    }

    /** Redessine tous les widgets posés (appelé après chaque /app natif). */
    public static void update(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, Widgets.class));
        for (int id : ids) m.updateAppWidget(id, build(c));
    }

    private static RemoteViews build(Context c) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget);
        List<Store.App> apps = Store.load(c);
        for (int i = 0; i < CELLS.length; i++) {
            if (i < apps.size() && apps.get(i).u.length() > 0) {
                Store.App a = apps.get(i);
                v.setViewVisibility(CELLS[i], View.VISIBLE);
                v.setTextViewText(CELLS[i], (a.g.length() > 0 ? a.g : "•") + "\n" + a.n);
                Intent it = new Intent(Intent.ACTION_VIEW, Uri.parse(a.u));
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                v.setOnClickPendingIntent(CELLS[i], PendingIntent.getActivity(
                        c, i, it,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
            } else {
                v.setViewVisibility(CELLS[i], View.GONE);
            }
        }
        return v;
    }
}
