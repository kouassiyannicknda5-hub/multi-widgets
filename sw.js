/* Service worker Multi-Widgets
 *
 * Deux défauts de la v1 sont corrigés ici :
 *
 *  1. Aucune stratégie de mise à jour. Le cache s'appelait « multi-widgets-v1 »
 *     pour toujours, il n'y avait aucun gestionnaire « activate », et le
 *     « fetch » répondait en cache-first y compris pour les navigations.
 *     Résultat : après la première visite, index.html était servi depuis le
 *     cache indéfiniment et plus aucun redéploiement n'atteignait l'utilisateur.
 *
 *  2. caches.addAll() échoue en bloc. Une seule ressource injoignable au
 *     premier lancement faisait avorter toute l'installation, et le mode hors
 *     ligne n'était alors jamais disponible.
 *
 * Stratégie retenue :
 *   - navigations (le HTML)  → réseau d'abord, repli sur le cache
 *   - tout le reste (assets) → cache d'abord, repli sur le réseau,
 *                              puis mise à jour du cache en arrière-plan
 */

/* Incrémenter cette version à chaque modification des fichiers précachés
 * suffit à faire purger les anciens caches par le gestionnaire « activate ». */
var CACHE = "multi-widgets-v4";

/* « ./ » et « index.html » désignent le même document : ne garder que « ./ »
 * évite de stocker le HTML deux fois. */
var PRECACHE = [
  "./",
  "manifest.json",
  "icons/icon-192.png",
  "icons/icon-512.png",
  "icons/icon-maskable-192.png",
  "icons/icon-maskable-512.png"
];

self.addEventListener("install", function (e) {
  e.waitUntil(
    caches.open(CACHE).then(function (cache) {
      /* Chaque ressource est mise en cache individuellement : un échec isolé
       * est ignoré au lieu de faire échouer l'installation entière. */
      return Promise.all(
        PRECACHE.map(function (url) {
          return cache
            .add(new Request(url, { cache: "reload" }))
            .catch(function () {
              /* hors ligne au premier lancement : on réessaiera la fois suivante */
            });
        })
      );
    }).then(function () {
      /* Active immédiatement, sans attendre la fermeture des anciens onglets. */
      return self.skipWaiting();
    })
  );
});

self.addEventListener("activate", function (e) {
  e.waitUntil(
    caches.keys().then(function (keys) {
      return Promise.all(
        keys.filter(function (k) { return k !== CACHE; })
            .map(function (k) { return caches.delete(k); })
      );
    }).then(function () {
      /* Prend la main sur les pages déjà ouvertes : c'est ce qui permet à une
       * nouvelle version d'être visible sans que l'utilisateur relance l'app. */
      return self.clients.claim();
    })
  );
});

self.addEventListener("fetch", function (e) {
  if (e.request.method !== "GET") return;

  /* Navigations : réseau d'abord. C'est le cœur du correctif — sans cela,
   * l'application reste figée sur la première version téléchargée. */
  if (e.request.mode === "navigate") {
    e.respondWith(
      fetch(e.request)
        .then(function (res) {
          var copy = res.clone();
          caches.open(CACHE).then(function (c) { c.put("./", copy); });
          return res;
        })
        .catch(function () {
          /* hors ligne : on sert la dernière copie connue */
          return caches.match("./").then(function (r) {
            return r || caches.match("index.html");
          });
        })
    );
    return;
  }

  /* Le reste : cache d'abord, puis rafraîchissement en arrière-plan
   * (stale-while-revalidate) pour que les icônes évoluent sans blocage. */
  e.respondWith(
    caches.match(e.request).then(function (cached) {
      var network = fetch(e.request).then(function (res) {
        if (res && res.ok && new URL(e.request.url).origin === self.location.origin) {
          var copy = res.clone();
          caches.open(CACHE).then(function (c) { c.put(e.request, copy); });
        }
        return res;
      }).catch(function () { return cached; });
      return cached || network;
    })
  );
});
