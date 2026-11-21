/* Test fonctionnel de l'application sous jsdom.
 *
 * Lance :  npm install   puis   npm test
 * (jsdom n'est qu'une dépendance de développement : l'application livrée
 *  reste sans aucune dépendance ni étape de build.)
 *
 * 26 assertions : démarrage, choix d'application, ajout/fermeture, thème,
 * persistance, restauration après relancement, accessibilité et clavier. */
const { JSDOM } = require("jsdom");
const fs = require("fs");
const path = require("path");

const ROOT = require("path").join(__dirname, "..");
const html = fs.readFileSync(path.join(ROOT, "index.html"), "utf-8");

let failed = 0;
function check(label, cond) {
  console.log((cond ? "  ✅ " : "  ❌ ") + label);
  if (!cond) failed++;
}

/* jsdom ne calcule aucune mise en page : clientWidth/clientHeight valent 0,
   ce qui ferait tout ramener à zéro par clamp(). On injecte donc une
   géométrie fictive, comme le ferait un vrai viewport de 800×600. */
function patchMetrics(w) {
  Object.defineProperty(w.Element.prototype, "clientWidth", { get() { return 800; } });
  Object.defineProperty(w.Element.prototype, "clientHeight", { get() { return 600; } });
}

const dom = new JSDOM(html, {
  url: "https://exemple.test/",
  runScripts: "dangerously",
  pretendToBeVisual: true,
  beforeParse: patchMetrics,
});
const { window } = dom;
const { document } = window;

window.addEventListener("error", (e) => {
  console.log("  ❌ erreur JS non rattrapée :", e.message);
  failed++;
});

setTimeout(() => {
  console.log("— Démarrage —");
  check("un widget est créé au lancement", document.querySelectorAll(".w").length === 1);
  check("le compteur est renseigné", document.getElementById("cnt").textContent.includes("1 widget sur 10"));
  check("le sélecteur d'application est affiché", !!document.querySelector(".w .pick"));
  check("10 applications proposées", document.querySelectorAll(".pick button").length === 10);

  console.log("— Choix d'une application —");
  const body = document.querySelector(".w .body");
  body.querySelector('button[data-i="3"]').dispatchEvent(new window.Event("click", { bubbles: true }));
  check("le widget affiche Google", document.querySelector(".w .n").textContent.includes("Google"));
  const go = document.querySelector(".w a.go");
  check("le lien part vers google.com", go && go.getAttribute("href") === "https://www.google.com");
  check("rel=noopener conservé", go.getAttribute("rel") === "noopener");

  console.log("— Ajout / fermeture —");
  const NEW = document.getElementById("new");
  NEW.dispatchEvent(new window.Event("click", { bubbles: true }));
  NEW.dispatchEvent(new window.Event("click", { bubbles: true }));
  check("trois widgets après deux ajouts", document.querySelectorAll(".w").length === 3);
  document.querySelectorAll(".w .cl")[2].dispatchEvent(new window.Event("click", { bubbles: true }));
  check("deux widgets après fermeture", document.querySelectorAll(".w").length === 2);

  console.log("— Thème —");
  const THEME = document.getElementById("theme");
  check("le bouton thème existe", !!THEME);
  const before = document.documentElement.getAttribute("data-theme");
  THEME.dispatchEvent(new window.Event("click", { bubbles: true }));
  const after = document.documentElement.getAttribute("data-theme");
  check("la bascule pose data-theme", before !== after && (after === "dark" || after === "light"));
  check("le thème est mémorisé", window.localStorage.getItem("multi-widgets.theme") === after);

  console.log("— Persistance —");
  const saved = JSON.parse(window.localStorage.getItem("multi-widgets.v1") || "null");
  check("la disposition est écrite dans localStorage", Array.isArray(saved) && saved.length === 2);
  check("le widget 0 garde son application (3 = Google)", saved && saved[0].app === 3);

  console.log("— Accessibilité —");
  const w0 = document.querySelector(".w");
  check("widget focusable (role=group + tabindex)", w0.getAttribute("role") === "group" && w0.getAttribute("tabindex") === "0");
  const rs = w0.querySelector(".rs");
  check("poignée = separator focusable", rs.getAttribute("role") === "separator" && rs.getAttribute("tabindex") === "0");
  check("région live présente", !!document.getElementById("live"));
  check("aria-label du bouton thème", /thème/i.test(THEME.getAttribute("aria-label") || ""));

  console.log("— Clavier —");
  const x0 = saved[0].x;
  w0.dispatchEvent(new window.KeyboardEvent("keydown", { key: "ArrowRight", altKey: true, bubbles: true }));
  const moved = JSON.parse(window.localStorage.getItem("multi-widgets.v1"))[0].x;
  check("Alt+→ déplace le widget", moved === x0 + 8);
  const h0 = JSON.parse(window.localStorage.getItem("multi-widgets.v1"))[0].h;
  w0.dispatchEvent(new window.KeyboardEvent("keydown", { key: "ArrowDown", altKey: true, shiftKey: true, bubbles: true }));
  const resized = JSON.parse(window.localStorage.getItem("multi-widgets.v1"))[0].h;
  check("Alt+Maj+↓ redimensionne", resized === h0 + 40);

  console.log("— Restauration —");
  const dom2 = new JSDOM(html, {
    url: "https://exemple.test/",
    runScripts: "dangerously",
    pretendToBeVisual: true,
    beforeParse(w2) {
      patchMetrics(w2);
      w2.localStorage.setItem("multi-widgets.v1", window.localStorage.getItem("multi-widgets.v1"));
      w2.localStorage.setItem("multi-widgets.theme", window.localStorage.getItem("multi-widgets.theme"));
    },
  });
  setTimeout(() => {
    const d2 = dom2.window.document;
    console.log("— Relancement avec données sauvegardées —");
    check("les 2 widgets sont restaurés", d2.querySelectorAll(".w").length === 2);
    check("le widget 0 retrouve Google", d2.querySelector(".w .n").textContent.includes("Google"));
    check("le thème est réappliqué", d2.documentElement.getAttribute("data-theme") === after);
    const d3 = JSON.parse(d2.defaultView.localStorage.getItem("multi-widgets.v1"));
    check("les positions sont conservées", d3[0].x === moved && d3[0].h === resized);

    console.log("");
    console.log(failed === 0 ? "TOUS LES TESTS PASSENT ✔" : failed + " TEST(S) EN ÉCHEC ✘");
    process.exit(failed === 0 ? 0 : 1);
  }, 120);
}, 120);
