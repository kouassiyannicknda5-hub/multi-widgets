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

## Widget NATIF d'écran d'accueil (Android)
Une page web ne peut **jamais** dessiner sur l'écran d'accueil en dehors de sa
propre fenêtre : c'est une limite de sécurité des navigateurs. Le dossier
`android/` contient donc un compagnon natif qui fait exactement ce que le Web
ne peut pas faire :

- un **vrai widget Android** posé sur l'écran d'accueil (grille de 8 cases),
  vivant en dehors de toute application ouverte ;
- **un toucher sur une case ouvre directement l'application** dans le
  navigateur : Multi-Widgets ne s'ouvre jamais ;
- l'application native ne sert qu'une fois, de `/app` natif : ajouter ou
  retirer des applications ; le widget se redessine immédiatement.

### Obtenir l'APK sans installer Android Studio
1. GitHub → onglet **Actions** → workflow **APK** → dernière exécution verte.
2. Télécharger l'artefact `multi-widgets-widget` (contient `app-debug.apk`).
3. Copier l'APK sur le téléphone et l'installer (sources inconnues autorisées).

### Poser le widget
Appui long sur l'écran d'accueil → **Widgets** → « Multi-Widgets » → glisser
sur l'écran. C'est fini : les cases lancent les sites d'un toucher, sans
ouvrir aucune application.
