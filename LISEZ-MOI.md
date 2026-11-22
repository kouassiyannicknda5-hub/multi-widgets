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
- Quand une application est ouverte dans la fenêtre : **`←`** revient à la
  liste. Les entrées sont des **applications mobiles** : sur le web, un
  navigateur ne peut pas les lancer, la fenêtre affiche un message honnête
  (aucun lien ouvert) ; l'ouverture réelle se fait dans l'APK Android.
- Tout est mémorisé : widgets, catalogue d'applications, application ouverte
  dans chaque widget, thème. Retirer tous les widgets laisse un écran vide
  avec un `+`, respecté tel quel au relancement.
- Thème clair/sombre dans le panneau `/app` ; sans choix, suit le système.

## Ajouter une application (`/app`)
1. Toucher `/app` (volet « Plus »).
2. Renseigner un glyphe (facultatif, ex. 🎵), un nom, et le **paquet Android**
   de l'application (facultatif sur le web). **Aucune adresse web** : ce sont
   des applications mobiles, pas des liens.
3. « Ajouter à toutes les fenêtres » : l'application apparaît immédiatement.
   Dans l'APK Android, l'écran « Barre latérale » propose un **sélecteur de vos
   applications installées** (avec recherche) — c'est là que vous ajoutez vos apps.

## À savoir : applications mobiles, pas de liens
Multi-Widgets n'ouvre **aucun lien web** : les entrées du catalogue sont des
**applications mobiles**. Dans un navigateur, il est impossible de lancer vos
applications natives (sandbox) — la fenêtre affiche donc un message honnête et
rien n'est ouvert. C'est dans l'**APK Android** que vos applications installées
s'ouvrent directement (widget d'accueil et barre latérale), sans navigateur.

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
- `tests/smoke.js` : 48 assertions fonctionnelles (`npm install && npm test`)
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
- **un toucher sur une case ouvre l'application native installée**, sans lien
  web ni navigateur : Multi-Widgets ne s'ouvre jamais ;
- ajout/retrait de vos applications installées depuis l'app (écran « Barre
  latérale » ou `/app` natif) ; le widget et la barre se redessinent aussitôt.

### Obtenir l'APK sans installer Android Studio
1. GitHub → onglet **Actions** → workflow **APK** → dernière exécution verte.
2. Télécharger l'artefact `multi-widgets-widget` (contient `app-debug.apk`).
3. Copier l'APK sur le téléphone et l'installer (sources inconnues autorisées).

### Poser le widget
Appui long sur l'écran d'accueil → **Widgets** → « Multi-Widgets » → glisser
sur l'écran. C'est fini : les cases lancent les sites d'un toucher, sans
ouvrir aucune application.

## Application Android complète (v2) : Multi-widgets — AI Multitasking & Universal Translator
Le dossier `android/` est désormais un véritable projet Android multi-modules
(Kotlin, Jetpack Compose, Material 3, SDK 35) réunissant trois moteurs :

1. **Multi-widgets Engine** — barre latérale flottante, dispositions natives
   (1, 2, 3, 4, 6 zones selon le firmware), lanceur d'applications, widget
   d'écran d'accueil ;
2. **AI Translation Engine** — OCR ML Kit (latin, chinois, japonais, coréen,
   devanagari), détection de langue, traduction hors ligne à modèles
   téléchargés à la demande, fournisseur cloud interchangeable ;
3. **AI Voice Engine** — reconnaissance vocale, sous-titres traduits flottants,
   doublage TTS segmenté ; verrou d'honnêteté Whisper tant que le benchmark
   réel n'est pas passé.

Le routeur de traitement choisit local/cloud/éco/secours selon les capacités
réelles, la connexion, la thermique, la mémoire et vos préférences de
confidentialité.

Documentation complète dans `docs/` : BUILD.md, INSTALL.md, API-CONFIG.md,
FEATURE_REPORT.md (fonctions terminées et non terminées, sans rien promettre
non vérifié).

## Traduction et voix DANS le navigateur (nouveauté web)
Deux applications intégrées rejoignent le catalogue de chaque widget :

- **🌐 Traducteur** : collez un texte (russe, chinois, anglais, français…),
  la langue est détectée (écriture puis mots-outils, toujours affichée pour
  vérification), la traduction s'affiche dans le widget, avec **Copier** et
  **🔊 Écouter** (synthèse vocale du navigateur).
- **🎙 Voix** : parlez, le navigateur reconnaît (Web Speech API, Chrome),
  traduit, affiche la traduction comme sous-titre et la dit à voix haute ;
  réglage de vitesse, bouton Stop. Si le navigateur ne fournit pas la
  reconnaissance, un message honnête l'explique.

Réglages dans **/app** → section « Traduction & voix » :
- fournisseur **MyMemory** (sans clé, par défaut) ou **LibreTranslate**
  (URL et clé facultatives, gardées dans le stockage local privé, jamais
  dans le code) ;
- langue cible (fr, en, es, de, pt, it, zh, ja, ko, ru, ar, hi).

Limites assumées : la traduction web passe par le réseau (le navigateur ne
porte pas les modèles hors ligne de l'APK Android) ; la reconnaissance vocale
dépend du navigateur (Chrome la fournit, Firefox non).
