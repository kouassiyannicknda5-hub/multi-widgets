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
- While an application is open in a widget: **`←`** returns to the list,
  **`↗`** opens it in a separate tab.
- Everything persists: widgets, app catalog, per-widget open application,
  theme. Removing all widgets leaves a minimal empty screen with a `+`,
  honoured as-is on next launch.
- Light/dark theme from the `/app` panel; follows the OS until you choose.

## Adding an application (`/app`)

1. Tap `/app` on any widget.
2. Fill a glyph (optional, e.g. 🎵), a name, an address
   (`deezer.com` is enough — `https://` is prepended).
3. "Add to all widgets": the app appears in every widget's list at once.

## Fair warning: sites that refuse the frame

Many major sites (WhatsApp, Google, Facebook, Instagram…) **forbid** being
displayed inside a frame, via `X-Frame-Options` or CSP — that is *their*
servers' decision, not a Multi-Widgets bug. The widget then shows their
refusal message, and **`↗`** opens the application in a tab. Sites that
allow embedding render normally inside the widget.

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

Functional tests (33 assertions, jsdom):

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
- **one tap on a cell opens the application directly** in the browser —
  Multi-Widgets itself never opens;
- the native app only ever acts as a native `/app`: add or remove
  applications, and the widget redraws immediately.

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
