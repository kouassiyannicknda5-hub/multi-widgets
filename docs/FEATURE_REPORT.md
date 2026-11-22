# Rapport des fonctions terminées et non terminées

Règle appliquée : rien n'est présenté comme fonctionnel sans compilation et
tests en CI ; rien n'est promis sans capacité vérifiée sur l'appareil.

## TERMINÉ (compilé, testé en CI, artefact APK produit)
- Projet Android multi-modules Kotlin (app, core, multitasking, ai), Compose + Material 3, SDK 35, minSdk 26.
- 15 écrans Compose : démarrage, tableau de bord, barre latérale, fenêtres, lanceur, traducteurs texte/écran/vocal, sous-titres, langues, modèles, confidentialité, performance, appareil, historique.
- Barre latérale flottante **hors de l'application** : service au premier plan (type `specialUse`, Android 14+) avec notification permanente — elle reste affichée par-dessus les autres applications même quand Multi-widgets est quitté. **Barre déplaçable** par sa poignée ⠿ (position mémorisée et restaurée), applications du téléphone **épinglées** sur la barre qui s'ouvrent en **natif** (icône réelle de l'app), bouton ☰ ouvrant un panneau de recherche de toutes les apps installées (appui = ouvrir, **appui long = épingler** sur la barre et le widget), accès dispositions/paramètres. Autorisations demandées à l'écran « Barre latérale » : superposition (`SYSTEM_ALERT_WINDOW`) puis notifications (Android 13+), chacune expliquée avant la demande système.
- Dispositions natives : 1 app ; 2 côte à côte ; 2 superposées (écran partagé Android 7+) ; 3/4/6 zones **uniquement si le firmware fournit les fenêtres libres**, sinon panneau flottant de secours avec explication.
- Widget d'écran d'accueil natif 8 cases : un toucher ouvre **l'application native installée**, **sans aucun lien web** (case masquée si l'app est absente) ; Multi-widgets ne s'ouvre jamais. Ajout des apps du téléphone via la section « Applications de la barre et du widget » (écran Barre latérale) et le sélecteur `/app` (recherche parmi les applications installées).
- OCR local ML Kit : latin, chinois, japonais, coréen, devanagari ; architecture multi-moteurs.
- Détection de langue ML Kit, traduction hors ligne ML Kit à modèles téléchargés à la demande.
- Fournisseur cloud interchangeable (LibreTranslate), clé uniquement en stockage privé, erreurs réseau/quota gérées.
- AI Processing Router : local / cloud / éco / secours, confidentialité stricte, thermique, mémoire.
- Traduction d'écran : MediaProjection après consentement système, cadence réglable, buffers bornés, sous-titres flottants, fenêtres sécurisées respectées (noires par design Android).
- Voix : ASR système Android, pipeline écoute → détection → traduction → doublage TTS segmenté non bloquant + sous-titres.
- TTS Android : genre proposé **seulement si le moteur installé le fournit**, vitesse, interruption.
- Sous-titres : file bornée à 12 segments, copie par appui long, décalage réglable.
- Historique facultatif, désactivé par défaut, supprimable, borné.
- Confidentialité : permissions au bon moment avec explication (superposition, notifications, micro, MediaProjection), aucun service d'accessibilité, aucune capture secrète, rappel notification permanent pour la barre latérale et pendant la capture.
- Optimisations RAM : un modèle à la fois, libération après traitement, langues à la demande, rien sur le thread principal, mode économie.
- Tests unitaires JVM : routeur (9), corps de requête cloud (2), file de sous-titres (2). CI : compilation + tests + APK.
- Documentation : BUILD, INSTALL, API-CONFIG, ce rapport.

## NON TERMINÉ / VERROUILLÉ (volontairement, selon la spécification)
- **Whisper tiny/base (whisper.cpp + NDK)** : verrou `WhisperGate` — exige ARM64 vérifié, modèle gguf téléchargé, benchmark de débit passé sur l'appareil réel. L'écran Modèles IA affiche précisément ce qui manque. Aucun modèle lourd n'est intégré sans benchmark.
- **Gemma E2B quantifié** : option expérimentale non intégrée.
- **AudioPlaybackCapture** : capacité détectée (Android 10+) mais pipeline non implémenté : les politiques des applications sources interdisent souvent la capture ; message explicatif et saisie manuelle proposés.
- **Dispositions 3/4/6 zones réelles** : dépendent du firmware (freeform). Sur ZTE/Tecno/Itel/Infinix et beaucoup de Redmi/Samsung : panneau flottant de secours, conformément à la spécification.
- **ASR/TTS cloud** : interfaces prévues, aucun fournisseur implémenté pour l'instant (local uniquement).
- **Suppression physique des modèles ML Kit du stockage** : non exposée par cette version du SDK ; le retrait passe par le registre local (l'application cesse d'utiliser la langue), consigné ici plutôt que simulé.
- **APK release signé** : nécessite les clés de signature du propriétaire ; procédure documentée dans BUILD.md.
- **Tests sur appareil réel** (rotation, thermique longue durée, firmwares OEM) : tests instrumentés à exécuter sur device ; la CI fournit compilation + tests JVM.
