# Compilation de Multi-widgets (Android)

## Prérequis locaux
- JDK 17
- Gradle 8.9 (ou laisser la CI faire : aucun Gradle local n'est obligatoire)
- Android SDK 35 (platforms;android-35, build-tools 34+)
- Variables `ANDROID_HOME` ou `local.properties` (`sdk.dir=…`)

Versions épinglées (combinaison vérifiée en CI) :
AGP 8.7.3 · Gradle 8.9 · Kotlin 2.0.21 · compileSdk 35 · minSdk 26 · targetSdk 35.

## Commandes
```bash
cd android
gradle assembleDebug          # APK debug
gradle testDebugUnitTest      # tests unitaires JVM (routeur, cloud, sous-titres)
gradle assembleRelease        # non signé (voir ci-dessous)
```
APK produit : `android/app/build/outputs/apk/debug/app-debug.apk`

## Compilation sans Android Studio (CI GitHub)
Le workflow **APK** (`.github/workflows/android.yml`) installe Gradle 8.9,
compile, exécute les tests unitaires et publie l'artefact
`multi-widgets-widget` à chaque push touchant `android/`. En cas d'échec, la
fin du journal Gradle est publiée en commentaire de l'issue de diagnostics.

## APK release signé
Fournir un keystore hors dépôt, puis :
```bash
gradle assembleRelease \
  -Pandroid.injected.signing.store.file=$CHEMIN/key.jks \
  -Pandroid.injected.signing.store.password=$MOTDEPASSE \
  -Pandroid.injected.signing.key.alias=$ALIAS \
  -Pandroid.injected.signing.key.password=$MOTDEPASSE
```
Aucune clé n'est présente dans le dépôt, et aucune ne doit y entrer.

## Modules Gradle
| Module | Rôle |
|---|---|
| `:app` | activité unique, navigation Compose, 15 écrans, thème M3 |
| `:core` | capacités appareil, réglages DataStore, permissions, lanceur |
| `:multitasking` | barre latérale, dispositions natives, widget d'écran d'accueil |
| `:ai` | OCR, langue, traduction locale/cloud, routeur, ASR, TTS, sous-titres, capture, historique |

Les domaines de la spécification (permissions/, network/, storage/, settings/,
sidebar/, launcher/, windows/, layouts/, router/, language/, translation/,
speech/, ocr/, subtitles/, voice/, synchronization/, providers/, cloud/,
local/, models/, privacy/, ui/, tests/) sont organisés en paquets Kotlin à
l'intérieur de ces quatre modules : chaque paquet porte le nom du domaine.
