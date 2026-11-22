/* Test fonctionnel de multi-widgets 3.0 sous jsdom.
 *
 * Lance :  npm install   puis   npm test
 *
 * Vérifie le visuel officiel recréé : tableau de bord 4 tuiles + navigation
 * basse, dock latéral + menu, 4 fenêtres simultanées, traduction, voix &
 * sous-titres & doublage, paramètres avancés, historique, /app, installation.
 * La barre latérale (dock) est permanente : jamais masquable, toujours visible,
 * et déplaçable par sa poignée ⠿ (position mémorisée).
 * 48 assertions. */
const { JSDOM } = require("jsdom");
const fs = require("fs");
const path = require("path");

const ROOT = path.join(__dirname, "..");
const html = fs.readFileSync(path.join(ROOT, "index.html"), "utf-8");

let failed = 0;
function check(label, cond) {
  console.log((cond ? "  ✅ " : "  ❌ ") + label);
  if (!cond) failed++;
}
const wait = (ms) => new Promise((r) => setTimeout(r, ms));
const click = (w, el) => el.dispatchEvent(new w.Event("click", { bubbles: true }));

function boot(storage) {
  return new JSDOM(html, {
    url: "https://exemple.test/",
    runScripts: "dangerously",
    pretendToBeVisual: true,
    beforeParse(w) {
      if (storage) Object.keys(storage).forEach((k) => w.localStorage.setItem(k, storage[k]));
    },
  });
}

(async () => {
  const dom = boot(null);
  const { window } = dom, { document } = window;
  window.addEventListener("error", (e) => { console.log("  ❌ erreur JS :", e.message); failed++; });
  await wait(150);

  console.log("— Tableau de bord (panneau 1) —");
  check("en-tête : logo + titre + tagline", !!document.querySelector("#hd img") && document.querySelector("#hd b").textContent === "multi-widgets");
  check("4 tuiles : fenêtres, traduction, voix, paramètres", document.querySelectorAll("#tiles .tile").length === 4);
  check("navigation basse Accueil / Historique / Plus", document.querySelectorAll("#bnav button").length === 3);
  check("cartes modes hors ligne & en ligne présentes", document.querySelectorAll("#v-accueil .cardmode").length === 2);

  console.log("— Fenêtres simultanées (panneau 3) —");
  click(window, document.querySelector('#tiles .tile[data-v="fenetres"]'));
  await wait(20);
  check("la tuile ouvre la vue fenêtres", document.getElementById("v-fenetres").classList.contains("on"));
  check("une fenêtre initiale + tuile d'ajout", document.querySelectorAll("#wingrid .win").length === 1 && !!document.getElementById("wadd"));
  click(window, document.getElementById("wadd"));
  await wait(20);
  check("4 zones maximum : + ajoute jusqu'à 4", document.querySelectorAll("#wingrid .win").length === 2);
  const win0 = document.querySelectorAll("#wingrid .win")[0];
  click(window, win0.querySelector('.pick button[data-i="0"]'));
  await wait(20);
  check("le traducteur intégré s'ouvre dans la fenêtre", !!win0.querySelector(".tr") && !win0.querySelector("iframe"));
  click(window, win0.querySelector(".wmin"));
  await wait(20);
  check("− minimise la fenêtre", win0.classList.contains("min"));
  click(window, win0.querySelector(".wmin"));
  click(window, win0.querySelector('.emb .back'));
  await wait(20);
  click(window, win0.querySelector('.pick button[data-i="2"]'));
  await wait(20);
  check("une application mobile n'ouvre AUCUN lien (message honnête, pas d'iframe)", win0.querySelector("iframe") === null && !!win0.querySelector(".appnote"));
  check("barre de titre : nom + − + ×", win0.querySelector(".wt").textContent.includes("WhatsApp") && !!win0.querySelector(".wcl"));
  click(window, win0.querySelector(".wcl"));
  await wait(20);
  check("× ferme la fenêtre", document.querySelectorAll("#wingrid .win").length === 1);

  console.log("— Dock latéral (panneau 2) —");
  check("dock : 6 icônes d'apps + menu ⋯", document.querySelectorAll("#dock button").length === 7);
  check("poignée de déplacement ⠿ présente", !!document.querySelector("#dock .grip"));
  click(window, document.querySelector("#dock button:last-child"));
  await wait(20);
  check("le menu s'ouvre avec 5 entrées (sans « Masquer la barre »)", document.querySelectorAll("#dockmenu button").length === 5);
  check("aucune poignée pour masquer la barre", document.getElementById("dockhandle") === null);
  check("aucune entrée « Masquer la barre » dans le menu", document.querySelector('#dockmenu button[data-act="hide"]') === null);
  check("barre latérale visible au démarrage", document.getElementById("dock").hidden === false);
  click(window, document.querySelector('#dockmenu button[data-act="param"]'));
  await wait(20);
  check("le menu navigue vers Paramètres", document.getElementById("v-param").classList.contains("on"));

  console.log("— Paramètres avancés (panneaux 8-9) —");
  const rows = document.querySelectorAll("#v-param .prow");
  check("5 rangées : langues, hors ligne, performance, confidentialité, apparence", rows.length === 5);
  click(window, document.getElementById("row-lang"));
  check("langues préférées dépliables", document.getElementById("langdet").hidden === false);
  click(window, document.getElementById("sw-off"));
  check("mode hors ligne persisté", window.localStorage.getItem("mw.tr.offline") === "1");
  click(window, document.getElementById("sw-dark"));
  check("apparence bascule en clair", document.documentElement.getAttribute("data-theme") === "light");
  click(window, document.getElementById("sw-dark"));

  console.log("— Traduction (panneau 4) —");
  click(window, document.querySelector('#tiles .tile[data-v="trad"]'));
  await wait(20);
  check("carte de traduction : sélecteurs + zone + sortie", !!document.getElementById("tsrc") && !!document.getElementById("tin") && !!document.getElementById("tout"));
  check("bouton flottant présent sur la vue", !!document.getElementById("fab"));
  document.getElementById("tin").value = "The dream is within reach.";
  click(window, document.getElementById("tgo"));
  await wait(30);
  check("mode hors ligne : message honnête, pas de silence", document.getElementById("tout").textContent.indexOf("Erreur : mode hors ligne") === 0);
  click(window, document.querySelector('#bnav button[data-v="plus"]'));
  await wait(10);
  click(window, document.getElementById("p-apps"));
  await wait(20);
  check("panneau /app : catalogue + réglages fournisseurs", document.querySelectorAll("#shl .row button[data-del]").length === 12 && !!document.getElementById("s-prov"));
  click(window, document.getElementById("shc"));
  await wait(10);
  // coupe le mode hors ligne via l'interrupteur (cliquable même vue masquée)
  click(window, document.getElementById("sw-off"));
  await wait(10);
  check("mode hors ligne coupé", window.localStorage.getItem("mw.tr.offline") === "0");

  console.log("— Voix, sous-titres, doublage (panneaux 5-7) —");
  click(window, document.querySelector('#tiles .tile[data-v="voix"]'));
  await wait(20);
  check("assistant : micro + onde + statut + paire de langues", !!document.getElementById("mic") && document.querySelectorAll("#wave span").length === 24 && document.getElementById("vpair").textContent.includes("→"));
  check("sans SpeechRecognition, clic expliqué honnêtement", (click(window, document.getElementById("mic")), document.getElementById("vstatus").textContent.includes("Reconnaissance absente")));
  check("lecteur de doublage : ⏮ ▶ ⏭ + vitesse + Voix/Sous-titres", !!document.getElementById("pprev") && !!document.getElementById("pplay") && !!document.getElementById("pnext") && !!document.getElementById("pspeed") && !!document.getElementById("pvoice") && !!document.getElementById("psubs"));
  click(window, document.getElementById("pspeed"));
  check("la vitesse cycle (1.0x → 1.25x)", document.getElementById("pspeed").textContent === "1.25x");
  check("barre de sous-titres flottante masquée par défaut", document.getElementById("subsbar").hidden === true);

  console.log("— Historique & confidentialité —");
  click(window, document.querySelector('#bnav button[data-v="hist"]'));
  await wait(20);
  check("historique vide affiché proprement", document.getElementById("hlist").textContent.includes("Aucune traduction"));
  click(window, document.getElementById("sw-strict"));
  check("confidentialité stricte persistée", window.localStorage.getItem("mw.tr.strict") === "1");
  click(window, document.getElementById("sw-strict"));

  console.log("— /app : catalogue —");
  click(window, document.querySelector('#bnav button[data-v="plus"]'));
  click(window, document.getElementById("p-apps"));
  await wait(20);
  document.getElementById("f-n").value = "Deezer";
  document.getElementById("f-p").value = "deezer.android.app";
  document.getElementById("shf").dispatchEvent(new window.Event("submit", { bubbles: true, cancelable: true }));
  await wait(20);
  const st = JSON.parse(window.localStorage.getItem("multi-widgets.v3"));
  check("application ajoutée au catalogue (13)", st.catalog.length === 13);
  check("entrée enregistrée comme application (paquet), sans lien web", st.catalog[12].p === "deezer.android.app" && st.catalog[12].u === undefined);
  click(window, document.querySelector('#shl button[data-del="12"]'));
  await wait(20);
  check("× la retire partout", JSON.parse(window.localStorage.getItem("multi-widgets.v3")).catalog.length === 12);
  click(window, document.getElementById("shc"));

  console.log("— Installation (invite Chrome) —");
  const evt = new window.Event("beforeinstallprompt", { cancelable: true });
  let prompted = 0, resolveChoice = null;
  evt.prompt = () => { prompted++; };
  evt.userChoice = new Promise((r) => { resolveChoice = r; });
  window.dispatchEvent(evt);
  check("beforeinstallprompt intercepté", evt.defaultPrevented === true);
  check("bandeau d'installation affiché", document.getElementById("installbar").hidden === false);
  click(window, document.getElementById("iby"));
  await wait(30);
  check("prompt natif appelé", prompted === 1);
  resolveChoice({ outcome: "accepted" });
  await wait(30);
  window.dispatchEvent(new window.Event("appinstalled"));
  check("bandeau retiré après installation", document.getElementById("installbar").hidden === true);

  console.log("— Persistance & relancement —");
  const saved = window.localStorage.getItem("multi-widgets.v3");
  const dom2 = boot({ "multi-widgets.v3": saved, "mw.tr.dark": "1", "mw.tr.dock": "0" });
  const d2 = dom2.window.document;
  await wait(150);
  check("fenêtres restaurées au relancement", d2.querySelectorAll("#wingrid .win").length === 1);
  check("thème sombre restauré", d2.documentElement.getAttribute("data-theme") === "dark");
  check("barre latérale toujours visible même si un ancien réglage la masquait", d2.getElementById("dock").hidden === false);

  console.log("");
  console.log(failed === 0 ? "TOUS LES TESTS PASSENT ✔" : failed + " TEST(S) EN ÉCHEC ✘");
  process.exit(failed === 0 ? 0 : 1);
})();
