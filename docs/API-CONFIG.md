# Configuration des API et fournisseurs

## Principe de sécurité
Aucune clé secrète n'est intégrée au code source, ni commitée. Les
identifiants saisis vivent dans le DataStore privé de l'application
(`/data/data/app.multiwidgets/…`), accessibles uniquement par elle.

## Fournisseur de traduction cloud (interchangeable)
Protocole pris en charge : **LibreTranslate** (POST `/translate`, JSON
`{q, source, target, format, api_key?}` → `{translatedText}`).

Configuration dans l'application (écran Performance/Confidentialité, champs
fournisseur) ou en DataStore :
- `fournisseur_url` : ex. `https://libretranslate.mondomaine.example`
- `fournisseur_cle` : facultatif selon l'instance

Comportements couverts :
- erreurs réseau : message explicite, repli local si modèle présent ;
- quotas / clés invalides : le code HTTP et un extrait de la réponse sont
  affichés, jamais la clé ;
- confidentialité stricte : le cloud est court-circuité même configuré.

## Ajouter un autre fournisseur
Implémenter `app.multiwidgets.ai.translation.CloudTranslator` (une méthode
`traduire`), l'enregistrer dans `fournisseurConfigure(url)` selon le schéma
d'URL choisi. Le routeur (`ProcessingRouter`) n'appelle que l'interface.

## Moteurs OCR et ASR interchangeables
- OCR : implémenter `MoteurOcr` et l'enregistrer dans `OcrEngine.moteurs`.
- ASR : implémenter `AsrEngine`. Whisper (whisper.cpp + NDK) s'insérera ici
  lorsque son verrou d'honnêteté (`WhisperGate`) sera levé sur l'appareil :
  ARM64 vérifié, modèle gguf présent, benchmark de débit passé.

## Modèles hors ligne
ML Kit Translation : téléchargement à la demande, écran **Modèles IA**.
Aucun modèle n'est embarqué dans l'APK (taille et RAM obligent).
