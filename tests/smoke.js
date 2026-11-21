/* Test fonctionnel de l'application sous jsdom.
 *
 * Lance :  npm install   puis   npm test
 * (jsdom n'est qu'une dépendance de développement : l'application livrée
 *  reste sans aucune dépendance ni étape de build.)
 *
 * Modèle « widget direct » : un clic sur l'icône exécute le premier widget,
 * chaque widget porte ses applications (+ / − / /app), un clic sur une
 * application l'ouvre dans le widget. 33 assertions. */
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
  await wait(120);

  console.log("— Lancement : l'application ne s'ouvre pas, le widget, lui —");
  check("un seul widget exécuté d'emblée", document.querySelectorAll(".w").length === 1);
  check("aucun tableau décoratif : l'écran est la pile de widgets", document.getElementById("screen").children.length === 1);
  check("le widget contient déjà 10 applications", document.querySelectorAll(".w .pick button").length === 10);
  const w0 = document.querySelector(".w");
  check("barre : bouton − présent", !!w0.querySelector(".minus"));
  check("barre : bouton + présent", !!w0.querySelector(".plus"));
  check("barre : commande /app présente", w0.querySelector(".appcmd").textContent === "/app");

  console.log("— Un clic sur une application l'ouvre DANS le widget —");
  click(window, w0.querySelector('.pick button[data-i="0"]'));
  await wait(20);
  const frame = w0.querySelector(".body iframe");
  check("l'application s'ouvre dans un cadre, pas dans un onglet", !!frame);
  check("le cadre pointe vers WhatsApp Web", frame && frame.getAttribute("src") === "https://web.whatsapp.com");
  check("barre d'application visible avec retour ←", !w0.querySelector(".emb").hidden && !!w0.querySelector(".emb .back"));
  check("lien ↗ nouvel onglet présent", w0.querySelector(".emb .out").getAttribute("href") === "https://web.whatsapp.com");
  check("le titre du widget nomme l'application", w0.querySelector(".n").textContent.includes("WhatsApp"));
  click(window, w0.querySelector(".emb .back"));
  await wait(20);
  check("← revient à la liste des applications", !!w0.querySelector(".pick") && w0.querySelector(".emb").hidden);

  console.log("— + ajoute un widget, − le retire —");
  click(window, w0.querySelector(".plus"));
  await wait(20);
  check("deux widgets après +", document.querySelectorAll(".w").length === 2);
  check("le nouveau widget a aussi +, − et /app", (() => {
    const w1 = document.querySelectorAll(".w")[1];
    return w1.querySelector(".plus") && w1.querySelector(".minus") && w1.querySelector(".appcmd");
  })());
  click(window, document.querySelectorAll(".w")[1].querySelector(".minus"));
  await wait(20);
  check("un widget après −", document.querySelectorAll(".w").length === 1);

  console.log("— /app ajoute des applications à tous les widgets —");
  click(window, w0.querySelector(".appcmd"));
  await wait(20);
  const sheet = document.getElementById("sheet");
  check("le panneau /app s'ouvre", sheet.hidden === false);
  check("le panneau liste les 10 applications", sheet.querySelectorAll(".row button[data-del]").length === 10);
  document.getElementById("f-g").value = "🎵";
  document.getElementById("f-n").value = "Deezer";
  document.getElementById("f-u").value = "www.deezer.com";
  document.getElementById("shf").dispatchEvent(new window.Event("submit", { bubbles: true, cancelable: true }));
  await wait(20);
  const st = JSON.parse(window.localStorage.getItem("multi-widgets.v2"));
  check("l'application est ajoutée au catalogue", st.catalog.length === 11);
  check("l'adresse sans schéma est normalisée en https", st.catalog[10].u === "https://www.deezer.com");
  check("elle apparaît dans l'affichage du widget", document.querySelectorAll(".w .pick button").length === 11);
  click(window, sheet.querySelector('button[data-del="10"]'));
  await wait(20);
  check("× la retire de tous les widgets", document.querySelectorAll(".w .pick button").length === 10);
  click(window, document.getElementById("shc"));
  check("le panneau se referme", sheet.hidden === true);

  console.log("— Installation (invite Chrome) —");
  const IB = document.getElementById("installbar"), INST = document.getElementById("inst");
  check("bandeau masqué tant que rien n'est installable", IB.hidden === true && INST.hidden === true);
  const evt = new window.Event("beforeinstallprompt", { cancelable: true });
  let prompted = 0, resolveChoice = null;
  evt.prompt = () => { prompted++; };
  evt.userChoice = new Promise((r) => { resolveChoice = r; });
  window.dispatchEvent(evt);
  check("beforeinstallprompt intercepté", evt.defaultPrevented === true);
  check("bandeau et bouton ⤓ affichés", IB.hidden === false && INST.hidden === false);
  click(window, document.getElementById("iby"));
  await wait(30);
  check("le prompt natif est appelé", prompted === 1);
  resolveChoice({ outcome: "accepted" });
  await wait(30);
  window.dispatchEvent(new window.Event("appinstalled"));
  check("tout disparaît une fois installée", IB.hidden === true && INST.hidden === true);

  console.log("— Persistance et relancement —");
  click(window, document.querySelector(".w .plus"));
  await wait(20);
  click(window, document.querySelectorAll(".w")[1].querySelector('.pick button[data-i="6"]'));
  await wait(20);
  const saved = window.localStorage.getItem("multi-widgets.v2");
  const dom2 = boot({ "multi-widgets.v2": saved });
  const d2 = dom2.window.document;
  await wait(120);
  check("les deux widgets sont restaurés", d2.querySelectorAll(".w").length === 2);
  check("le widget 1 ré-exécute directement son application (Gmail)", (() => {
    const f = d2.querySelectorAll(".w")[1].querySelector(".body iframe");
    return f && f.getAttribute("src") === "https://mail.google.com";
  })());
  check("le widget 0 retrouve sa liste", !!d2.querySelector(".w .pick"));

  console.log("— Écran vide —");
  const dom3 = boot({ "multi-widgets.v2": JSON.stringify({ widgets: [], catalog: [{ g: "🔎", n: "Google", u: "https://www.google.com" }] }) });
  const d3 = dom3.window.document;
  await wait(120);
  check("sans widget, un écran d'accueil minimal propose +", !!d3.getElementById("empty"));
  click(dom3.window, d3.querySelector("#empty button"));
  await wait(20);
  check("+ recrée le premier widget", d3.querySelectorAll(".w").length === 1);

  console.log("");
  console.log(failed === 0 ? "TOUS LES TESTS PASSENT ✔" : failed + " TEST(S) EN ÉCHEC ✘");
  process.exit(failed === 0 ? 0 : 1);
})();
