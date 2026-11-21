package app.multiwidgets;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Le catalogue d'applications du widget, en SharedPreferences (JSON).
 *  Même rôle que /app dans la version web : ajouter ou retirer des
 *  applications accessibles depuis l'écran d'accueil. */
public class Store {

    public static class App {
        public String g, n, u;
        public App(String g, String n, String u) { this.g = g; this.n = n; this.u = u; }
    }

    private static final String KEY = "catalog";

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("mw", Context.MODE_PRIVATE);
    }

    public static List<App> load(Context c) {
        List<App> out = new ArrayList<>();
        String json = prefs(c).getString(KEY, null);
        if (json == null) {
            out = defaults();
            save(c, out);
            return out;
        }
        try {
            JSONArray a = new JSONArray(json);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                out.add(new App(o.optString("g", "•"), o.optString("n", ""), o.optString("u", "")));
            }
        } catch (Exception e) {
            out = defaults();
        }
        return out;
    }

    public static void save(Context c, List<App> apps) {
        JSONArray a = new JSONArray();
        for (App ap : apps) {
            JSONObject o = new JSONObject();
            try {
                o.put("g", ap.g);
                o.put("n", ap.n);
                o.put("u", ap.u);
            } catch (Exception ignored) { }
            a.put(o);
        }
        prefs(c).edit().putString(KEY, a.toString()).apply();
        Widgets.update(c);   // le widget se redessine immédiatement
    }

    /** Les mêmes applications que la version web, pour commencer. */
    public static List<App> defaults() {
        List<App> d = new ArrayList<>();
        d.add(new App("💬", "WhatsApp", "https://web.whatsapp.com"));
        d.add(new App("🐦", "X", "https://x.com"));
        d.add(new App("👥", "Facebook", "https://m.facebook.com"));
        d.add(new App("🔎", "Google", "https://www.google.com"));
        d.add(new App("📷", "Instagram", "https://www.instagram.com"));
        d.add(new App("▶", "YouTube", "https://m.youtube.com"));
        d.add(new App("✉", "Gmail", "https://mail.google.com"));
        d.add(new App("📍", "Maps", "https://maps.google.com"));
        return d;
    }
}
