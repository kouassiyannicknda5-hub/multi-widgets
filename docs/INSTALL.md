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
3. **Barre latérale** : « Comprendre et autoriser la superposition » explique
   puis demande l'affichage par-dessus les autres applications ; sur Android
   13+, « Autoriser les notifications » rend visible le rappel permanent qui
   garde la barre active. Ensuite « Lancer la barre latérale » : elle tourne
   comme service au premier plan, flotte par-dessus vos autres applications et
   **reste visible même quand vous quittez Multi-widgets**.
4. **Traducteur d'écran** : le bouton déclenche la boîte de consentement
   système MediaProjection ; rien n'est capturé sans ce oui, à chaque session.
5. **Traducteur vocal** : demande le micro au premier usage, avec explication.

## Widget d'écran d'accueil
Appui long sur l'écran d'accueil → **Widgets** → « Multi-widgets » → glisser.
Chaque case ouvre **l'application native installée** directement, sans ouvrir
Multi-widgets et **sans aucun lien web** : si l'application n'est pas installée,
la case est simplement masquée.

**Ajouter vos applications du téléphone** : dans l'app, écran **« Barre
latérale »** → section **« Applications de la barre et du widget »** →
**« ➕ Ajouter une application du téléphone »** (ou l'activité `/app`) : un
sélecteur liste toutes les applications installées avec recherche ; toucher
une app l'ajoute au widget **et** à la barre. Aucun lien web n'est proposé.

**Barre latérale** : déplaçable par sa poignée ⠿ (la position est mémorisée).
Le bouton ☰ ouvre un panneau listant toutes les applications installées :
un appui ouvre l'app, un **appui long l'épingle** sur la barre et le widget.

## Permissions et pourquoi
| Permission | Moment | Raison affichée |
|---|---|---|
| Superposition | avant barre latérale/sous-titres | afficher par-dessus les autres apps |
| Micro | première session vocale | écoute uniquement pendant la session |
| MediaProjection | chaque session de traduction d'écran | consentement système explicite |
| Notifications (Android 13+) | avant barre latérale / capture | rappel permanent qui garde la barre et les sous-titres actifs hors de l'application |

Aucune permission n'est demandée au lancement « pour plus tard ».
