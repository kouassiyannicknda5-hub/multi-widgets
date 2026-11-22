# Installation sur le téléphone

## Obtenir l'APK
1. GitHub → **Actions** → workflow **APK** → dernière exécution verte.
2. Télécharger l'artefact `multi-widgets-widget`, dézipper `app-debug.apk`.
3. Copier l'APK sur le téléphone, l'ouvrir, autoriser « sources inconnues »
   pour le gestionnaire de fichiers, installer.

## Premier lancement
1. Ouvrir **Multi-widgets** : écran de démarrage puis tableau de bord.
2. Le tableau de bord affiche les capacités RÉELLES de l'appareil
   (fabricant, RAM libre, fenêtres libres ou non) : c'est lui qui détermine
   quelles dispositions multitâches seront proposées.
3. **Barre latérale** : « Comprendre et autoriser » explique puis demande la
   superposition, ensuite « Lancer la barre latérale ».
4. **Traducteur d'écran** : le bouton déclenche la boîte de consentement
   système MediaProjection ; rien n'est capturé sans ce oui, à chaque session.
5. **Traducteur vocal** : demande le micro au premier usage, avec explication.

## Widget d'écran d'accueil
Appui long sur l'écran d'accueil → **Widgets** → « Multi-widgets » → glisser.
Chaque case ouvre directement son site dans le navigateur, sans ouvrir
l'application. Réglage des cases : tableau de bord → widget, ou l'activité de
configuration du widget.

## Permissions et pourquoi
| Permission | Moment | Raison affichée |
|---|---|---|
| Superposition | avant barre latérale/sous-titres | afficher par-dessus les autres apps |
| Micro | première session vocale | écoute uniquement pendant la session |
| MediaProjection | chaque session de traduction d'écran | consentement système explicite |
| Notifications | premier service au premier plan | rappel permanent de capture active |

Aucune permission n'est demandée au lancement « pour plus tard ».
