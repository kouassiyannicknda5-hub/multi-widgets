# Rapport des fonctions terminées et non terminées

Règle appliquée : rien n'est présenté comme fonctionnel sans compilation et
tests en CI ; rien n'est promis sans capacité vérifiée sur l'appareil.

## TERMINÉ (compilé, testé en CI, artefact APK produit)
- Projet Android multi-modules Kotlin (app, core, multitasking, ai), Compose + Material 3, SDK 35, minSdk 26.
- 15 écrans Compose : démarrage, tableau de bord, barre latérale, fenêtres, lanceur, traducteurs texte/écran/vocal, sous-titres, langues, modèles, confidentialité, performance, appareil, historique.
- Barre latérale flottante : poignée de bord, bouton ☰ déplaçable, recherche d'applications, accès dispositions/paramètres.
- Dispositions natives : 1 app ; 2 côte à côte ; 2 superposées (écran partagé Android 7+) ; 3/4/6 zones **uniquement si le firmware fournit les fenêtres libres**, sinon panneau flottant de secours avec explication.
- Widget d'écran d'accueil natif 8 cases : un toucher ouvre le site, l'application ne s'ouvre jamais.
- OCR local ML Kit : latin, chinois, japonais, coréen, devanagari ; architecture multi-moteurs.
- Détection de langue ML Kit, traduction hors ligne ML Kit à modèles téléchargés à la demande.
- Fournisseur cloud interchangeable (LibreTranslate), clé uniquement en stockage privé, erreurs réseau/quota gérées.
- AI Processing Router : local / cloud / éco / secours, confidentialité stricte, thermique, mémoire.
- Traduction d'écran : MediaProjection après consentement système, cadence réglable, buffers bornés, sous-titres flottants, fenêtres sécurisées respectées (noires par design Android).
- Voix : ASR système Android, pipeline écoute → détection → traduction → doublage TTS segmenté non bloquant + sous-titres.
- TTS Android : genre proposé **seulement si le moteur installé le fournit**, vitesse, interruption.
- Sous-titres : file bornée à 12 segments, copie par appui long, décalage réglable.
- Historique facultatif, désactivé par défaut, supprimable, borné.
- Confidentialité : permissions au bon moment avec explication, aucun service d'accessibilité, aucune capture secrète, rappel notification pendant la capture.
- Optimisations RAM : un modèle à la fois, libération après traitement, langues à la demande, rien sur le thread principal, mode économie.
- Tests unitaires JVM : routeur (9), corps de requête cloud (2), file de sous-titres (2). CI : compilation + tests + APK.
- Documentation : BUILD, INSTALL, API-CONFIG, ce rapport.

## NON TERMINÉ / VERROUILLÉ (volontairement, selon la spécification)
- **Whisper tiny/base (whisper.cpp + NDK)** : verrou `WhisperGate` — exige ARM64 vérifié, modèle gguf téléchargé, benchmark de débit passé sur l'appareil réel. L'écran Modèles IA affiche précisément ce qui manque. Aucun modèle lourd n'est intégré sans benchmark.
- **Gemma E2B quantifié** : option expérimentale non intégrée.
- **AudioPlaybackCapture** : capacité détectée (Android 10+) mais pipeline non implémenté : les politiques des applications sources interdisent souvent la capture ; message explicatif et saisie manuelle proposés.
- **Dispositions 3/4/6 zones réelles** : dépendent du firmware (freeform). Sur ZTE/Tecno/Itel/Infinix et beaucoup de Redmi/Samsung : panneau flottant de secours, conformément à la spécification.
- **ASR/TTS cloud** : interfaces prévues, aucun fournisseur implémenté pour l'instant (local uniquement).
- **APK release signé** : nécessite les clés de signature du propriétaire ; procédure documentée dans BUILD.md.
- **Tests sur appareil réel** (rotation, thermique longue durée, firmwares OEM) : tests instrumentés à exécuter sur device ; la CI fournit compilation + tests JVM.
