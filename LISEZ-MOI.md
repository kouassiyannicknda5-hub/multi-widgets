# Multi-Widgets

Jusqu'à 10 widgets déplaçables et redimensionnables, chacun ouvrant une seule
application. Les tuiles sont des aperçus simulés ; le bouton « Ouvrir le site »
lance la vraie version web.

## Utiliser sur le téléphone
1. Mettre le dossier sur un hébergement HTTPS (GitHub Pages, Render, Netlify…).
2. Ouvrir l'adresse dans Chrome (Android).
3. Menu ⋮ puis « Installer l'application » (ou « Ajouter à l'écran d'accueil »).

## Tester sur ordinateur
Servir le dossier (l'installation et le service worker demandent HTTPS) :

```bash
python3 -m http.server 8000
```

puis ouvrir http://localhost:8000.

## Contenu
- `index.html` : l'application entière (structure, styles, logique)
- `manifest.json` : métadonnées d'installation (avec icônes maskables)
- `sw.js` : service worker — précache, mode hors ligne et stratégie de mise à jour
- `icons/` : icônes 192 et 512, plus leurs variantes maskables (zone sûre 80 %)
- `README.md` : documentation en anglais

## Ce qui est mémorisé
Tout survit au rechargement : nombre de widgets, application choisie dans
chacun, positions, tailles et thème. Les données vivent dans le
`localStorage` du navigateur ; vider le stockage du site repart de zéro.

## Commandes
- **Souris / doigt** : glisser la barre de titre pour déplacer, tirer le coin
  bas-droit pour redimensionner.
- **Clavier** : `Tab` pour atteindre un widget ou sa poignée, puis
  `Alt` + flèches pour déplacer, `Alt` + `Maj` + flèches pour redimensionner.
- **Thème** : bouton 🌙 / ☀️ en haut à droite. Sans choix de votre part,
  l'application suit le réglage du système.

## Mises à jour
Le service worker sert le HTML en réseau d'abord et purge les anciens caches à
chaque activation : un redéploiement atteint les utilisateurs sans manipulation
de leur part. Pour forcer une purge manuelle du précache, incrémenter `CACHE`
dans `sw.js`.

## À savoir
Les fenêtres flottantes par-dessus les autres applications demandent un projet
Android natif (étape suivante).

## Licence
MIT — voir le fichier `LICENSE`.
