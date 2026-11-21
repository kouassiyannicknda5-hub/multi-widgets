# Analyse du dépôt `multi-widgets`

> Audit réalisé le 2026-11-21 sur la branche `arena/da22698e-multi-widgets`
> (base : `68511b8`).

---

## 1. Vue d'ensemble

**Ce que c'est :** une PWA (Progressive Web App) monopage, 100 % statique, qui
simule un écran d'accueil Android avec jusqu'à 10 widgets déplaçables et
redimensionnables. Chaque widget affiche un aperçu *simulé* d'une application
(WhatsApp, Chrome, Facebook…) et un bouton « Ouvrir le site » qui part vers la
vraie version web.

**Empilement technique :** HTML + CSS + JavaScript vanilla dans un seul fichier,
aucune dépendance, aucun build, aucun `package.json`.

| Fichier | Rôle | Lignes | Taille |
|---|---|---|---|
| `index.html` | L'application entière (structure + styles + logique) | 98 | 7,4 Ko |
| `manifest.json` | Métadonnées d'installation PWA | 1 | 355 o |
| `sw.js` | Service worker : précache + mode hors ligne | 3 | 349 o |
| `LISEZ-MOI.md` | Documentation d'usage (en français) | 20 | 840 o |
| `icons/icon-192.png` | Icône 192×192 (valide, RVB 8 bits) | — | 822 o |
| `icons/icon-512.png` | Icône 512×512 `any maskable` (valide) | — | 2,4 Ko |
| `icons/o` | **Fichier parasite** — 1 octet, un simple `\n` | — | 1 o |

**Total : 122 lignes de code pour ~12 Ko.** C'est remarquablement compact.

---

## 2. État de santé immédiat

| Contrôle | Résultat |
|---|---|
| Arborescence Git propre (aucune modification non committée) | ✅ |
| Syntaxe JavaScript valide (`node --check`) | ✅ |
| Les 2 PNG sont des images valides aux dimensions annoncées | ✅ |
| `manifest.json` est un JSON valide | ✅ |
| Les 10 entrées de `APPS` correspondent aux 10 entrées de `URLS` | ✅ |
| Toutes les ressources précachées par `sw.js` existent sur le disque | ✅ |
| Servie en HTTP : `/`, `index.html`, `manifest.json`, `sw.js`, les 2 icônes → **200** | ✅ |
| Licences, `.gitignore`, CI, README en anglais | ❌ absents |
| Persistance des données | ❌ aucune |
| Accessibilité clavier | ❌ aucune |

**L'application fonctionne.** Elle se lance, crée un widget au démarrage, et
tous ses assets se chargent.

---

## 3. Points forts

1. **Zéro dépendance, zéro build.** Rien à installer, rien à compiler, rien qui
   puisse pourrir. Le dépôt restera ouvrable dans 10 ans.
2. **Surface d'attaque quasi nulle.** Aucun formulaire, aucun appel réseau
   sortant, aucune donnée utilisateur. Tout le HTML injecté via `innerHTML`
   provient de tableaux *codés en dur* — pas d'XSS possible en l'état.
   Les liens externes portent correctement `rel="noopener"`.
3. **Le maniement tactile est bien pensé.** Les `Pointer Events` avec
   `setPointerCapture` couvrent souris, stylet et doigt dans un seul code.
   `touch-action:none` est posé exactement où il faut (barre de titre, poignée
   de redimensionnement). Le `clamp()` empêche de perdre un widget hors écran.
4. **Détails mobiles soignés.** `viewport-fit=cover`, `env(safe-area-inset-*)`,
   `orientation:portrait`, couleurs de thème cohérentes entre le CSS, le
   manifeste et la barre d'état.
5. **Le README est honnête.** Il dit explicitement que les tuiles sont des
   « aperçus simulés » et que les vraies fenêtres flottantes exigeraient un
   projet Android natif. Pas de survente.

---

## 4. Problèmes détectés, par gravité

### 🔴 Critique — le service worker gèle l'application dans le temps

```js
var C="multi-widgets-v1", F=[...];
// fetch:
return r || fetch(e.request)   // ← cache d'abord, toujours
```

La stratégie est **cache-first sans aucun gestionnaire `activate`**
(`grep -c activate sw.js` → **0**), et le nom de cache `multi-widgets-v1`
n'est jamais incrémenté.

**Conséquence :** dès la première visite, `index.html` est mis en cache. Après
cela, *plus aucune mise à jour n'atteindra l'utilisateur*, même si vous
redéployez. Vous corrigerez un bug et vos utilisateurs verront l'ancienne
version indéfiniment. C'est le piège classique et le plus coûteux des PWA.

**Correctif :** ajouter un `activate` qui purge les anciens caches, et passer
la navigation HTML en **network-first** :

```js
self.addEventListener("activate", function (e) {
  e.waitUntil(
    caches.keys().then(function (ks) {
      return Promise.all(ks.filter(function (k) { return k !== C })
                            .map(function (k) { return caches.delete(k) }));
    }).then(function () { return self.clients.claim() })
  );
});

self.addEventListener("fetch", function (e) {
  if (e.request.mode === "navigate") {              // HTML → réseau d'abord
    e.respondWith(fetch(e.request).catch(function () {
      return caches.match("index.html");
    }));
    return;
  }
  e.respondWith(caches.match(e.request).then(function (r) {
    return r || fetch(e.request);                   // assets → cache d'abord
  }));
});
```

---

### 🔴 Critique — rien n'est sauvegardé

Aucun `localStorage`, `sessionStorage` ni `indexedDB` dans tout le fichier.
Chaque rechargement **efface tous les widgets** : leur nombre, leur application,
leur position, leur taille. Pour une application dont le propos est d'être un
*tableau de bord personnel*, c'est la fonctionnalité manquante la plus visible.

**Correctif :** persister `list` à chaque mutation, recharger au démarrage.

```js
function save() {
  try {
    localStorage.setItem("mw", JSON.stringify(
      list.map(function (w) { return { app: w.app, x: w.x, y: w.y, w: w.w, h: w.h }; })
    ));
  } catch (e) {}
}
function load() {
  try { return JSON.parse(localStorage.getItem("mw")) || null } catch (e) { return null }
}
```

Appeler `save()` dans `geo()`, à la fermeture d'un widget et après `fill()` ;
remplacer le `add(null)` final par une restauration, avec repli sur un widget
vide si rien n'est enregistré.

---

### 🟠 Majeur — le thème sombre manuel est un chantier inachevé

Le CSS définit deux jeux de variables :

```css
@media (prefers-color-scheme:dark){ :root:not([data-theme="light"]){ … } }
:root[data-theme="dark"]{ … }
```

Mais **aucun JavaScript ne pose jamais `data-theme`** (occurrences dans le
script : **0**) et **aucun bouton de bascule n'existe dans le DOM**. Les deux
sélecteurs `[data-theme]` sont donc du code mort. Seul le réglage système
fonctionne — ce qui est déjà correct, mais le CSS promet plus qu'il ne tient.

**À choisir :** soit ajouter un bouton ☀/🌙 qui fait
`document.documentElement.dataset.theme = …` (et le persister), soit supprimer
les deux règles `[data-theme]` pour ne pas laisser de code trompeur.

**Second point :** le mode sombre ne redéfinit **pas** `--wall1` ni `--wall2`.
Le dégradé d'arrière-plan reste donc identique en clair et en sombre. Probable
oubli.

---

### 🟠 Majeur — trois déclarations CSS écrites au mauvais endroit

```css
:root{ --wall1:…; --tile:#fff;
box-sizing:border-box;padding-top:env(safe-area-inset-top,0px);padding-bottom:env(safe-area-inset-bottom,0px)}
```

Ces trois propriétés sont **piégées à l'intérieur du bloc `:root`**, collées à
la suite des variables. Elles s'appliquent donc à l'élément racine `<html>`,
pas aux widgets :

- `box-sizing` y est sans effet (et de toute façon déjà couvert par `*{box-sizing:border-box}` à la ligne suivante) ;
- les deux `padding` `env(safe-area-inset-*)` **ne protègent rien du tout** — un widget en haut d'un écran à encoche passera dessous.

Visiblement, l'intention était de les mettre sur `.w` ou `#screen`.
**Correctif :** sortir ces trois lignes de `:root` et les déplacer, par exemple :

```css
#screen{ padding-top:env(safe-area-inset-top,0px); padding-bottom:env(safe-area-inset-bottom,0px); }
```

---

### 🟡 Modéré — accessibilité : zéro

`grep -c 'keydown\|tabindex\|role='` → **0**.

- Déplacer ou redimensionner un widget est **impossible au clavier**. Un
  utilisateur au lecteur d'écran ou sans souris ne peut rien faire.
- Les poignées de redimensionnement sont des `<div class="rs" aria-label="…">`.
  Un `aria-label` sur un `<div>` sans `role` **n'est pas annoncé** par les
  lecteurs d'écran — l'étiquette est perdue.
- Pas de `role="dialog"`, pas de gestion de focus, pas d'`aria-live` sur le
  compteur de widgets.

**Minimum viable :** transformer `.rs` en `<button role="separator"
aria-orientation="vertical">`, et ajouter des raccourcis clavier (flèches pour
déplacer avec `Alt`, `Maj+flèches` pour redimensionner).

---

### 🟡 Modéré — `manifest.json` incomplet

| Champ | État | Impact |
|---|---|---|
| `start_url` | `./index.html` | Devrait être `./` : l'URL affichée sera plus propre |
| `scope` | absent | À ajouter (`./`) pour borner la navigation |
| `id` | absent | Sans lui, Chrome peut traiter un renommage comme une *nouvelle* application et faire perdre les données |
| `description` | absent | Non repris dans les magasins / l'écran d'installation |
| icône 192 | `purpose` absent | Vaut `any` par défaut. Sur Android 8+, l'icône sera **rogée en carré** au lieu d'être masquée |
| `categories`, `screenshots`, `shortcuts` | absents | Requis pour un bon score Lighthouse PWA et pour les `shortcuts` du lanceur |

---

### 🟡 Modéré — le fichier parasite `icons/o`

Un fichier d'**un seul octet** (`\n`), suivi par Git, servi en
`application/octet-stream`. C'est presque certainement le résidu d'une commande
shell détournée — un `> o` accidentel, ou une sortie redirigée vers un fichier
appelé `o`. Il ne casse rien, mais il traînera pour toujours dans
l'arborescence.

```bash
git rm icons/o
```

---

### 🔵 Mineur — incohérences et détails

1. **Le titre contredit le README.** `<title>Multi-Widgets – version 2</title>`
   alors que le README annonce *« Multi-Widgets (première version) »*. À
   aligner.
2. **Aucun historique Git.** Un seul commit, `Add files via upload` — dépôt
   téléversé via l'interface web de GitHub. Aucun suivi d'évolution possible,
   aucun `blame` exploitable, pas de branches de travail.
3. **Pas de `.gitignore`.** Rien à ignorer aujourd'hui, mais à prévoir dès
   qu'un outil s'ajoute.
4. **Pas de LICENSE.** Juridiquement, l'absence de licence signifie *« tous
   droits réservés »* : personne ne peut légalement réutiliser votre code.
   À trancher volontairement.
5. **Le contenu des widgets ne défile pas correctement au doigt.** `.body` a
   `overflow:auto` mais aucun `touch-action`. Sur mobile, un geste vertical
   dans la grille de 10 applications risque d'être interprété comme un
   déplacement du widget plutôt que comme un défilement. Ajouter
   `.body{touch-action:pan-y}`.
6. **`caches.addAll()` n'a aucun repli.** Si une seule ressource échoue au
   premier lancement (réseau instable), l'installation du service worker
   avorte en bloc et le mode hors ligne ne sera jamais disponible.
7. **L'URL « Chrome » pointe vers une page marketing.**
   `https://www.google.com/chrome` n'est pas une application utilisable en
   ligne, contrairement aux neuf autres. À remplacer (par ex. par un vrai
   service) ou à retirer de la liste.
8. **Le compteur de `z-index` ne se réinitialise jamais.** `++z` croît à chaque
   clic. Sans danger en pratique (une session n'atteindra pas 2 147 483 647),
   mais un `z = 10 + list.length` au rechargement serait plus propre.

---

## 5. Priorités recommandées

Si vous ne faites que trois choses :

| # | Action | Effort | Bénéfice |
|---|---|---|---|
| **1** | Corriger `sw.js` : handler `activate` + navigation en network-first | ~10 lignes | **Évite que vos utilisateurs soient bloqués sur une version périmée** |
| **2** | Ajouter la persistance `localStorage` | ~20 lignes | Transforme une démo en application réellement utilisable |
| **3** | Sortir les 3 déclarations CSS de `:root` | 3 lignes | Rétablit la protection des encoches, aujourd'hui inopérante |

Puis, dans l'ordre : `git rm icons/o`, finir ou retirer le thème manuel,
compléter le `manifest.json`, ajouter une LICENSE, et poser les bases de
l'accessibilité clavier.

---

## 6. Verdict

Un projet **petit, propre et honnête sur ses limites**, avec un vrai soin porté
au tactile et au mobile. Le code est lisible malgré sa densité, et il tourne
effectivement.

Mais il est à un stade de **prototype fonctionnel**, pas de produit : rien ne
survit à un rechargement, le service worker figera la première version pour
toujours, et le CSS contient un bloc de déclarations égarées qui annule une
protection d'encoche pourtant voulue. Trois défauts, tous mineurs en apparence,
dont deux rendraient l'application inutilisable à long terme si elle était
déployée en l'état.

Bonne nouvelle : **tout se corrige en moins de 50 lignes.**

---

## 7. Correctifs appliqués sur cette branche

Tous les points des sections 4 et 5 ont été corrigés et vérifiés par
**26 assertions fonctionnelles** (`tests/smoke.js`, exécutées sous jsdom)
ainsi que par une CI GitHub Actions (`.github/workflows/ci.yml`).

| # | Problème | Correctif livré |
|---|---|---|
| 1 | Service worker figeant l'app | `sw.js` réécrit : navigations **réseau d'abord** avec repli cache, handler `activate` qui purge les anciens caches et appelle `clients.claim()`, `skipWaiting()`, assets en cache-first avec rafraîchissement en arrière-plan |
| 2 | Précache fragile (`addAll`) | Chaque ressource est mise en cache individuellement ; un échec isolé n'annule plus l'installation |
| 3 | Aucune persistance | Disposition, application de chaque widget et thème écrits dans `localStorage`, restaurés au démarrage ; entrées illisibles écartées sans casser la restauration |
| 4 | CSS `safe-area` perdu dans `:root` | Déplacé sur `#screen` (et `#top`/`#hint` pour leurs bords) |
| 5 | Thème manuel mort | Bascule 🌙/☀️ réelle, mémorisée ; **ordre des règles CSS corrigé** — le thème clair explicite était écrasé la nuit à spécificité égale ; le mode sombre redéfinit enfin `--wall1/--wall2` |
| 6 | Accessibilité nulle | `Alt+flèches` déplace, `Alt+Maj+flèches` redimensionne ; widgets `role="group"` focusables ; poignée `div aria-label` muette → `role="separator"` focusable ; région `aria-live` ; styles `:focus-visible` |
| 7 | Défilement tactile cassé | `.body{touch-action:pan-y}` |
| 8 | Écouteur orphelin | `up()` retirait `pointerup`/`pointermove` mais pas `pointercancel` : corrigé |
| 9 | Compteur `z-index` infini | Réinitialisé à `10 + list.length` après chaque restauration |
| 10 | Trois listes parallèles `APPS`/`URLS` | Fusionnées en un seul tableau `[nom, couleur, glyphe, url]` : plus de désynchronisation possible |
| 11 | « Chrome » → page marketing | Remplacé par X (`https://x.com`), une application réellement utilisable |
| 12 | `icons/o` parasite | `git rm` |
| 13 | Manifeste incomplet | Ajout de `id`, `scope`, `start_url:"./"`, `description`, `categories` ; icônes **maskables** générées (contenu recentré dans la zone sûre de 80 %, fond = couleur de thème) |
| 14 | Titre vs README | `<title>Multi-Widgets</title>`, aligné sur les deux documentations |
| 15 | Pas de LICENSE / .gitignore / CI / README anglais | `LICENSE` (MIT), `.gitignore`, `README.md`, `.github/workflows/ci.yml`, `package.json` (jsdom en devDependency seulement) |

### Vérifications effectuées après correctifs
- `node --check` sur `sw.js` et sur le script extrait de `index.html` : OK
- `tests/smoke.js` : 26/26 assertions vertes, dont la **restauration après
  relancement** (deux instances jsdom enchaînées partageant le même stockage)
- Servie en HTTP : toutes les ressources en 200, `icons/o` désormais en 404
- CI : syntaxe, cohérence manifeste/précache, stratégie de mise à jour du
  service worker et tests fonctionnels à chaque push et pull request
