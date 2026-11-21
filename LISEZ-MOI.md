# Multi-Widgets

**Un clic sur l'icône exécute le widget : l'application ne s'ouvre pas
au-dessus.** L'écran EST le widget, rien d'autre — pas de tableau, pas de
fond d'écran, pas d'étape intermédiaire.

## Fonctionnement
- Au lancement, le **premier widget s'exécute directement** : il retrouve
  l'application qui y était ouverte, ou sa liste d'applications.
- Chaque widget **contient déjà ses applications** (celles fournies et celles
  que vous ajoutez). **Un clic sur une application l'ouvre DANS le widget.**
- Barre de chaque widget :
  - **`−`** retire ce widget ;
  - **`+`** ajoute un autre widget (10 maximum) ;
  - **`/app`** ouvre le panneau des applications : ajoutez-en encore plus,
    retirez-en avec `×`. La commande est présente sur **tous** les widgets et
    ses changements s'appliquent à **tous** les widgets.
- Quand une application est ouverte dans le widget : **`←`** revient à la
  liste, **`↗`** ouvre l'application dans un onglet séparé.
- Tout est mémorisé : widgets, catalogue d'applications, application ouverte
  dans chaque widget, thème. Retirer tous les widgets laisse un écran vide
  avec un `+`, respecté tel quel au relancement.
- Thème clair/sombre dans le panneau `/app` ; sans choix, suit le système.

## Ajouter une application (`/app`)
1. Toucher `/app` sur n'importe quel widget.
2. Renseigner un glyphe (facultatif, ex. 🎵), un nom, une adresse
   (`deezer.com` suffit : `https://` est ajouté).
3. « Ajouter à tous les widgets » : l'application apparaît immédiatement dans
   l'affichage de chaque widget.

## À savoir : sites qui refusent le cadre
Beaucoup de grands sites (WhatsApp, Google, Facebook, Instagram…)
**interdisent** d'être affichés dans un cadre, par en-tête
`X-Frame-Options` ou CSP — c'est une décision de *leur* serveur, pas un
défaut de Multi-Widgets. Le widget affiche alors leur message de refus ;
la flèche **`↗`** ouvre l'application dans un onglet. Les sites qui
autorisent l'intégration s'affichent normalement dans le widget.

## Utiliser sur le téléphone
1. Mettre le dossier sur un hébergement HTTPS (GitHub Pages, Render, Netlify…).
2. Ouvrir l'adresse dans Chrome (Android).
3. Le bandeau « Installer Multi-Widgets » propose l'installation ; après
   cela, **un seul clic sur l'icône exécute le premier widget**.
   (Menu ⋮ → « Installer l'application » fonctionne aussi.)
4. Sur iPhone/iPad : bouton Partager puis « Sur l'écran d'accueil ».

## Tester sur ordinateur
```bash
python3 -m http.server 8000
```
puis ouvrir http://localhost:8000.

## Contenu
- `index.html` : l'application entière (structure, styles, logique)
- `manifest.json` : métadonnées d'installation (icônes maskables)
- `sw.js` : service worker — hors ligne et mises à jour réseau d'abord
- `icons/` : icônes 192/512 et variantes maskables
- `tests/smoke.js` : 33 assertions fonctionnelles (`npm install && npm test`)
- `README.md` : documentation en anglais

## Licence
MIT — voir le fichier `LICENSE`.
