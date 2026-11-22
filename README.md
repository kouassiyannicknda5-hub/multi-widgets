# Multi-Widgets

**One tap on the icon runs the widget: the app never opens over it.**
The screen *is* the widget — no board, no wallpaper, no intermediate step.

> 🇫🇷 Documentation détaillée en français : [LISEZ-MOI.md](LISEZ-MOI.md).

## How it works

- On launch the **first widget runs directly**, restoring whichever
  application was open inside it — or showing its application list.
- Every widget **already carries its applications** (the built-in ones plus
  any you add). **One tap on an application opens it inside the widget.**
- Each widget bar holds:
  - **`−`** remove this widget;
  - **`+`** add another widget (up to 10);
  - **`/app`** the applications panel: add more apps, remove them with `×`.
    The command sits on **every** widget and its changes apply to **all**
    widgets at once.
- While an application is open in a window: **`←`** returns to the list. The
  entries are **mobile applications**: a browser cannot launch your native
  apps, so the window shows an honest message (no link is opened); the real
  opening happens in the Android APK.
- Everything persists: widgets, app catalog, per-widget open application,
  theme. Removing all widgets leaves a minimal empty screen with a `+`,
  honoured as-is on next launch.
- Light/dark theme from the `/app` panel; follows the OS until you choose.

## Adding an application (`/app`)

1. Tap `/app` (in the "More" tab).
2. Fill a glyph (optional, e.g. 🎵), a name, and the **Android package** of the
   app (optional on the web). **No web address**: these are mobile
   applications, not links.
3. "Add to every window": the app appears at once. In the Android APK, the
   "Sidebar" screen offers a **picker of your installed applications** (with
   search) — that is where you add your own apps.

## Fair warning: mobile applications, no links

Multi-Widgets opens **no web link**: the catalog entries are **mobile
applications**. In a browser it is impossible to launch your native apps
(sandbox) — the window therefore shows an honest message and nothing is
opened. It is in the **Android APK** that your installed applications open
directly (home-screen widget and sidebar), without any browser.

## Install on a phone

1. Host this folder on any HTTPS host (GitHub Pages, Netlify, Render…).
2. Open it in Chrome (Android); the "Install Multi-Widgets" banner offers
   installation (Menu ⋮ → *Install app* works too).
3. Once installed, **one tap on the icon runs the first widget**.
4. iPhone/iPad: Share → *Add to Home Screen*.

## Test on a computer

```bash
python3 -m http.server 8000
```

Functional tests (48 assertions, jsdom):

```bash
npm install && npm test
```

## Layout

| Path | Role |
|---|---|
| `index.html` | The whole app: structure, styles and logic in one file |
| `manifest.json` | PWA install metadata |
| `sw.js` | Service worker: precache, offline, network-first updates |
| `icons/` | 192 & 512 px icons plus maskable variants |
| `tests/smoke.js` | Functional test suite |

## License

MIT — see [LICENSE](LICENSE).

## Native Android home-screen widget

A web page can **never** draw on the home screen outside its own window —
that is a browser security boundary. The `android/` folder therefore holds a
native companion doing exactly what the Web cannot:

- a **real Android widget** (8-cell grid) sitting on the home screen,
  outside of any opened application;
- **one tap on a cell opens the installed native application**, with no web
  link and no browser — Multi-Widgets itself never opens;
- add/remove your installed applications from the app ("Sidebar" screen or
  native `/app`); the widget and the bar redraw immediately.

### Get the APK without Android Studio
1. GitHub → **Actions** → **APK** workflow → latest green run.
2. Download the `multi-widgets-widget` artifact (`app-debug.apk`).
3. Install it on the phone (allow unknown sources).

### Place the widget
Long-press the home screen → **Widgets** → "Multi-Widgets" → drag it out.
Done: cells launch sites with one tap, without opening any app.

## Full Android app (v2): Multi-widgets — AI Multitasking & Universal Translator

`android/` is now a real multi-module Android project (Kotlin, Jetpack
Compose, Material 3, SDK 35) with three engines:

1. **Multi-widgets Engine** — floating sidebar, native layouts (1/2/3/4/6
   zones, gated by real firmware capabilities), app launcher, home-screen
   widget;
2. **AI Translation Engine** — ML Kit OCR (latin, Chinese, Japanese, Korean,
   Devanagari), language ID, offline translation with on-demand models,
   swappable cloud provider;
3. **AI Voice Engine** — speech recognition, floating translated subtitles,
   segmented TTS dubbing; Whisper stays locked behind an honesty gate until
   a real-device benchmark passes.

The AI Processing Router picks local/cloud/eco/fallback from real
capabilities, connectivity, thermal state, memory and privacy preferences.

Full documentation in `docs/`: BUILD.md, INSTALL.md, API-CONFIG.md,
FEATURE_REPORT.md (finished vs unfinished, nothing promised unverified).

## In-browser translation & voice (web novelty)

Two built-in apps join every widget's catalog:

- **🌐 Translator**: paste text (Russian, Chinese, English, French…), the
  language is detected (script then stopwords, always shown for human
  verification), the translation renders inside the widget with **Copy** and
  **🔊 Listen** (browser speech synthesis).
- **🎙 Voice**: speak, the browser recognises (Web Speech API, Chrome),
  translates, shows the translation as a subtitle and speaks it; speed
  control, Stop button. Where recognition is unavailable, an honest message
  says so.

Settings in **/app** → "Translation & voice": **MyMemory** provider (no key,
default) or **LibreTranslate** (optional URL and key, kept in private local
storage, never in code); target language (fr, en, es, de, pt, it, zh, ja, ko,
ru, ar, hi).

Assumed limits: web translation needs network (browsers do not carry the
Android APK's offline models); speech recognition depends on the browser
(Chrome provides it, Firefox does not).
