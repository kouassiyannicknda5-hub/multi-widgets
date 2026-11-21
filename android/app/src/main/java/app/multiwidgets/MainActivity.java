package app.multiwidgets;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/** Réglage du widget : l'équivalent natif de la commande /app.
 *
 *  Cette activité ne sert qu'à choisir les applications du widget.
 *  Une fois le widget posé sur l'écran d'accueil, elle n'a plus besoin
 *  d'être ouverte : les cases du widget lancent directement les sites. */
public class MainActivity extends Activity {

    private List<Store.App> apps;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        setTitle("Multi-Widgets — applications du widget");

        apps = Store.load(this);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        ListView list = findViewById(R.id.list);
        list.setAdapter(adapter);
        refill();

        list.setOnItemClickListener((p, v, i, l) -> {
            if (i >= apps.size()) return;
            Store.App a = apps.get(i);
            new AlertDialog.Builder(this)
                    .setTitle("Retirer " + a.n + " ?")
                    .setMessage("L'application disparaîtra du widget de l'écran d'accueil.")
                    .setPositiveButton("Retirer", (d, w) -> {
                        apps.remove(i);
                        Store.save(this, apps);
                        refill();
                    })
                    .setNegativeButton("Annuler", null)
                    .show();
        });

        findViewById(R.id.add).setOnClickListener(v -> {
            EditText g = findViewById(R.id.g);
            EditText n = findViewById(R.id.n);
            EditText u = findViewById(R.id.u);
            String name = n.getText().toString().trim();
            String url = u.getText().toString().trim();
            if (name.isEmpty() || url.isEmpty()) return;
            if (!url.regionMatches(true, 0, "http://", 0, 7)
                    && !url.regionMatches(true, 0, "https://", 0, 8)) {
                url = "https://" + url;
            }
            apps.add(new Store.App(g.getText().toString().trim(), name, url));
            Store.save(this, apps);
            refill();
            g.setText("");
            n.setText("");
            u.setText("");
        });
    }

    private void refill() {
        adapter.clear();
        for (Store.App a : apps) {
            adapter.add((a.g == null || a.g.isEmpty() ? "•" : a.g) + "   " + a.n);
        }
        adapter.notifyDataSetChanged();
    }
}
