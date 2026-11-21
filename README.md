# Multi-Widgets

A zero-dependency, zero-build **Progressive Web App**: a simulated Android-style
home screen holding up to 10 draggable, resizable widgets. Each widget opens
exactly one application — the tile is a simulated preview, and its
"Open site" button launches the real web version.

> 🇫🇷 La documentation détaillée en français est dans [LISEZ-MOI.md](LISEZ-MOI.md).

## Features

- Up to 10 floating widgets, draggable by their title bar and resizable by
  their bottom-right handle (pointer events: mouse, touch and pen).
- **Everything persists.** Layout, sizes, chosen apps and theme are saved to
  `localStorage` and restored on reload.
- Light / dark theme with a manual toggle; follows the OS preference until you
  choose one yourself.
- Keyboard support: `Alt` + arrows moves the focused widget,
  `Alt` + `Shift` + arrows resizes it.
- Installable and fully offline (service worker + web manifest).

## Install on a phone

1. Host this folder on any HTTPS host (GitHub Pages, Netlify, Render…).
2. Open the address in Chrome (Android).
3. Menu ⋮ → *Install app* (or *Add to Home screen*).

## Test on a computer

Open `index.html` in a browser, or serve the folder:

```bash
python3 -m http.server 8000
```

Installation requires HTTPS; plain `file://` runs the app but not the
service worker.

## Layout

| Path | Role |
|---|---|
| `index.html` | The whole app: structure, styles and logic in one file |
| `manifest.json` | PWA install metadata |
| `sw.js` | Service worker: precache, offline, update strategy |
| `icons/` | 192 & 512 px icons, plus maskable variants (80 % safe zone) |
| `LISEZ-MOI.md` | French documentation |

## Updating the app

The service worker serves the HTML **network-first** and purges old caches on
activation, so a redeploy reaches users without any extra step. If you ever
need a hard reset of the precache, bump `CACHE` in `sw.js`.

## License

MIT — see [LICENSE](LICENSE).
