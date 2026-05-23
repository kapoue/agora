# CLAUDE.md — Agora

Fichier de contexte pour les assistants IA. À lire en premier avant toute intervention sur ce projet.

---

## Description du projet

**Agora** est une application Android de quiz culturel, disponible sur le Google Play Store.

- **Application ID** : `fr.kapoue.agora`
- **Version actuelle** : 2.2.1 (versionCode 38)
- **Dépôts** : GitHub `kapoue/agora` (push HTTPS) + Codeberg `kapoue/agora` (push SSH)
- **Deux modes de jeu** :
  - **Solo** : quiz thématique avec progression, difficulté par paliers, images illustrant les questions
  - **Multijoueur** : partie organisée localement via QR codes (pas de serveur), jusqu'à 3 manches, classement en temps réel

### Thèmes disponibles (13)
Histoire, Géographie, Sciences & Nature, Cinéma, Musique, Sport, Littérature, Informatique, Art, Mathématiques, Mythologie, Animaux, Véhicules  
*(+ Culture Générale : présente dans la DB mais exclue du mode multijoueur)*

### Niveaux de difficulté (3)
Débutant, Moyen, Expert

---

## Stack technique

| Couche | Technologie |
|---|---|
| Langage | Kotlin 2.0.0 |
| UI | Jetpack Compose + Material3 (BOM 2024.06.00) |
| Architecture | MVVM + états UI (`XxxUiState`) + Repository pattern |
| Injection de dépendances | Hilt 2.51.1 (KAPT) |
| Base de données | Room 2.6.1 — version 3, 4 tables |
| Navigation | Navigation Compose 2.7.7 — NavGraph unique |
| Images | Coil 3.0.0 (coil-compose + coil-network-okhttp) |
| Réseau | OkHttp 4.12.0 (images Unsplash/Pexels/Pixabay uniquement) |
| Préférences | DataStore Preferences 1.1.1 |
| Sérialisation | Gson 2.11.0 |
| QR codes | ZXing Android Embedded 4.3.0 + ZXing Core 3.5.3 |
| SDK cible | Android 35, minSdk 26 (Android 8.0+), JDK 17 |
| Build | Gradle 8.13 + Kotlin DSL (.gradle.kts) + AGP 8.5.0 |
| Java (build) | `/Applications/Android Studio.app/Contents/jbr/Contents/Home` |

---

## Versioning automatique depuis Git

**La version de l'app est dérivée automatiquement du tag Git** — ne jamais éditer `versionCode`/`versionName` à la main dans `build.gradle.kts`.

- `versionCode` = `git rev-list --count HEAD` (nombre total de commits, toujours croissant)
- `versionName` = tag exact sur HEAD (ex: `v2.2.1` → `"2.2.1"`), ou description relative (`"2.2.1-3-g77ced78"`) si pas de tag exact

### Workflow de release

```bash
# 1. Coder + committer normalement
git add -A && git commit -m "feat: description"

# 2. Créer le tag de release
git tag v2.3.0

# 3. Pousser sur GitHub + Codeberg
git push origin main --tags
git push codeberg main --tags

# 4. Build .aab (TOUJOURS utiliser clean pour invalider le cache Gradle)
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew clean bundleRelease
# → app/build/outputs/bundle/release/app-release.aab
```

> ⚠️ Sans `clean`, le cache de configuration Gradle peut retenir les anciennes valeurs git — le `.aab` afficherait alors l'ancienne version.

### Git remotes

```
origin   → https://github.com/kapoue/agora.git  (push GitHub)
codeberg → git@codeberg.org:kapoue/agora.git     (push Codeberg SSH)
```

---

## Base de données Room — version 3

4 tables, migrations déclarées dans `AgoraDatabase.kt` :

| Table | Entité | Usage |
|---|---|---|
| `questions` | `QuestionEntity` | Questions importées depuis assets + colonnes `lot`, `attempts` |
| `progress` | `ProgressEntity` | Progression solo par thème×difficulté |
| `theme_progress` | `ThemeProgressEntity` | Statistiques globales par thème |
| `multiplayer_sessions` | `MultiplayerSessionEntity` | Sessions multijoueur persistées |

**Historique migrations** :
- v1 → v2 : ajout colonnes `lot`/`attempts` sur `questions`, création `theme_progress`
- v2 → v3 : création `multiplayer_sessions`

---

## Structure source complète

```
app/src/main/
├── assets/questions/               # JSONs des questions : <THEME>_<DIFFICULTY>.json
│
└── java/com/kapoue/agora/
    ├── AgoraApplication.kt         # @HiltAndroidApp
    ├── MainActivity.kt             # Point d'entrée unique, setContent { AgoraNavGraph() }
    │
    ├── data/
    │   ├── local/
    │   │   ├── AssetQuestionLoader.kt          # Lecture JSON assets → List<QuestionEntity>
    │   │   ├── datastore/UserPreferencesDataStore.kt
    │   │   └── db/
    │   │       ├── AgoraDatabase.kt            # Room DB v3, migrations
    │   │       ├── Converters.kt               # TypeConverters pour Room
    │   │       ├── dao/
    │   │       │   ├── QuestionDao.kt          # CRUD questions + queries seed-based
    │   │       │   ├── ProgressDao.kt
    │   │       │   ├── ThemeProgressDao.kt
    │   │       │   └── MultiplayerSessionDao.kt
    │   │       └── entity/
    │   │           ├── QuestionEntity.kt
    │   │           ├── ProgressEntity.kt
    │   │           ├── ThemeProgressEntity.kt
    │   │           └── MultiplayerSessionEntity.kt
    │   └── repository/
    │       ├── QuestionRepository.kt           # Interface
    │       └── QuestionRepositoryImpl.kt       # Sync assets → Room, reset combo si MAJ
    │
    ├── di/
    │   ├── AppModule.kt            # DataStore, AssetQuestionLoader
    │   ├── DatabaseModule.kt       # Room, tous les DAOs
    │   └── NetworkModule.kt        # ImageLoader Coil uniquement
    │
    ├── domain/
    │   ├── model/
    │   │   ├── Theme.kt            # enum Theme(displayName, unsplashQuery, placeholderRes)
    │   │   ├── Difficulty.kt       # enum Difficulty
    │   │   ├── Question.kt         # Modèle solo
    │   │   ├── Progress.kt
    │   │   ├── MultiplayerModels.kt  # QrPayload, PlayerResult, WrongAnswer
    │   │   └── MultiplayerQuestion.kt
    │   └── usecase/
    │       ├── GetQuestionsUseCase.kt
    │       ├── GetRandomQuestionsUseCase.kt
    │       ├── GetProgressUseCase.kt
    │       └── SaveProgressUseCase.kt
    │
    ├── multiplayer/
    │   └── MultiplayerSessionManager.kt   # @ActivityRetainedScoped — état en mémoire
    │
    └── ui/
        ├── navigation/
        │   ├── Screen.kt           # Routes (sealed class ou object)
        │   └── AgoraNavGraph.kt    # NavHost unique avec tous les écrans
        │
        ├── components/             # AnswerButton, BlurredBackground, ThemeCard, WarriorIcon
        │
        ├── screens/
        │   ├── home/               # HomeScreen, HomeViewModel
        │   ├── difficulty/         # DifficultyScreen, DifficultyViewModel
        │   ├── game/               # GameScreen, GameViewModel (mode solo)
        │   ├── about/              # AboutScreen (lien Codeberg cliquable)
        │   └── multiplayer/
        │       ├── MultiplayerHomeScreen.kt
        │       ├── MultiplayerGameViewModel.kt   # ViewModel partagé jeu multi
        │       ├── organizer/
        │       │   ├── OrganizerSetupScreen.kt   # Config partie (thèmes, nb questions, manches)
        │       │   ├── OrganizerSetupViewModel.kt
        │       │   ├── OrganizerQrScreen.kt      # Affiche QR code à scanner par participants
        │       │   ├── OrganizerGameScreen.kt    # Écran "en attente" pendant que les joueurs jouent
        │       │   ├── OrganizerScanScreen.kt    # Scan QR résultats participants
        │       │   ├── OrganizerScanViewModel.kt
        │       │   ├── OrganizerRoundResultsScreen.kt
        │       │   └── OrganizerFinalResultsScreen.kt
        │       └── participant/
        │           ├── ParticipantScanScreen.kt     # Scan QR organisateur → charge les questions
        │           ├── ParticipantScanViewModel.kt
        │           ├── ParticipantSetupScreen.kt    # Saisie du prénom
        │           ├── ParticipantSetupViewModel.kt
        │           ├── ParticipantGameScreen.kt     # Quiz multijoueur
        │           ├── ParticipantRoundResultScreen.kt  # Score + QR résultat à montrer à l'orga
        │           └── WrongAnswersScreen.kt
        │
        ├── theme/                  # Color.kt (AgoraGold, AgoraBackground…), Typography, Shape
        │
        └── util/
            ├── QrCodeGenerator.kt      # ZXing → android.graphics.Bitmap
            ├── QrPayloadEncoder.kt     # GZIP + Base64 encode/decode (QrPayload, PlayerResult)
            ├── VibrationHelper.kt      # vibrateOnScan (150ms), API 31+ VibratorManager
            ├── MultiplayerShuffler.kt
            └── UiState.kt
```

---

## Mode multijoueur — Architecture

### Principe général
Pas de serveur. La communication passe **uniquement par QR codes** scannés localement.

### Flux de jeu complet

```
[ORGANISATEUR]                          [PARTICIPANTS]
OrganizerSetup                          
  → génère seed = System.currentTimeMillis()
  → charge toutes les questions (seed-based)
  → affiche QrPayload encodé (GZIP+Base64)
                                        ParticipantScan
                                          → scanne le QR de l'orga
                                          → vérifie la version de l'app
                                          → synchro assets → Room
                                          → génère les mêmes questions (même seed)
                                          → charge sa manche

                                        ParticipantSetup (prénom)
                                        ParticipantGame (joue sa manche)
                                        ParticipantRoundResult
                                          → affiche QR résultat (PlayerResult encodé)

OrganizerScan
  → scanne les QR résultats un par un
  → vibre à chaque scan réussi
  → stocke dans SessionManager

OrganizerRoundResults (classement de la manche)
  → si autre manche : réaffiche QR orga avec même seed, roundNumber+1
  → sinon : OrganizerFinalResults
```

### QrPayload (QR de l'organisateur)

```kotlin
data class QrPayload(
    val sessionId: String,
    val roundNumber: Int,
    val totalRounds: Int,
    val questionsPerRound: Int,
    val seed: Long,                   // graine déterministe
    val difficulties: List<String>,   // ex: ["DEBUTANT", "MOYEN"]
    val excludedThemes: List<String>, // thèmes exclus + toujours CULTURE_GENERALE
    val appVersionCode: Int,          // vérification compatibilité
    val appVersionName: String
)
```

### Génération déterministe des questions

Algorithme identique côté organisateur ET participant :
```kotlin
val allIds = questionDao.getQuestionIdsByDifficultiesAndThemes(difficulties, excludedThemes)
val allSelectedIds = allIds.sorted().shuffled(Random(seed)).take(totalNeeded)
val roundIds = allSelectedIds.chunked(questionsPerRound)[roundNumber - 1]
```

**Clé** : `allIds.sorted()` + `shuffled(Random(seed))` → résultat identique sur toute machine ayant les mêmes données Room.

### Vérification de version
Si `payload.appVersionCode != BuildConfig.VERSION_CODE` → message d'erreur clair indiquant qui doit mettre à jour.

### MultiplayerSessionManager (`@ActivityRetainedScoped`)
Singleton par activité. Contient tout l'état en mémoire :
- `sessionId`, `organizerName`, `totalRounds`, `questionsPerRound`, `currentRound`
- `seed`, `difficulties`, `excludedThemes`
- `roundQuestions: MutableMap<Int, List<MultiplayerQuestion>>`
- `allResults`, `currentRoundResults`, `eliminatedPlayers`
- Persiste via `MultiplayerSessionDao` (snapshot Room en fin de manche)

### Options de configuration organisateur
- **Questions par manche** : 10, 20, 30
- **Manches** : 1, 2, 3
- **Thèmes** : sélection via FilterChip (chip "Tous" = tous les 13 thèmes)

---

## Mode solo — Architecture

- `HomeScreen` → sélection du thème
- `DifficultyScreen` → sélection du niveau
- `GameScreen` / `GameViewModel` → quiz principal
  - Questions chargées depuis Room (sync préalable depuis assets)
  - Progression sauvegardée (`ProgressEntity` : `currentIndex`, `score`, `lot`, `attempts`)
  - Si nouvelles questions en assets → reset progression + lot

---

## Commandes

```bash
# JAVA requis pour Gradle
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# Build release signé (AAB Play Store) — toujours clean avant release
./gradlew clean bundleRelease
# → app/build/outputs/bundle/release/app-release.aab

# Build APK release
./gradlew assembleRelease

# Build debug
./gradlew assembleDebug

# Clean (supprime app/build/)
./gradlew clean

# Tests
./gradlew test
```

---

## Génération des questions (script Python)

Les questions sont générées par LLM et stockées en JSON dans `assets/questions/` :

```bash
cd agora/
python scripts/prepare_release.py                                        # Génère manquants + images + bump
python scripts/prepare_release.py --force                                # Régénère tout
python scripts/prepare_release.py --theme HISTOIRE --difficulty DEBUTANT # Thème/difficulté précis
python scripts/prepare_release.py --no-git                               # Sans commit/tag/push
```

- LLM : **Mistral** (`mistral-small-latest`) prioritaire, **Gemini** (`gemini-2.5-flash`) en fallback
- Images : Unsplash → Pexels → Pixabay (URLs stockées dans les JSON, chargées par Coil)
- Format : `assets/questions/<THEME>_<DIFFICULTY>.json`

---

## local.properties (git-ignoré, NE JAMAIS COMMITTER)

```
MISTRAL_API_KEY=...
GEMINI_API_KEY_1=...   # jusqu'à _9 (rotation automatique)
KEYSTORE_PATH=/chemin/absolu/vers/agora-release.jks
KEYSTORE_PASSWORD=...
KEY_ALIAS=agora
KEY_PASSWORD=...
UNSPLASH_ACCESS_KEY=...
PEXELS_API_KEY=...
PIXABAY_API_KEY=...
```

---

## Conventions de code

- **MVVM strict** : état UI dans `data class XxxUiState`, exposé via `StateFlow<XxxUiState>`
- **Composables** : paramètre `modifier: Modifier = Modifier` toujours en avant-dernière position (avant les lambdas)
- **Effets de bord** dans `LaunchedEffect` — jamais dans `remember {}` ni directement dans la composition
- **Nommage** : `PascalCase` classes, `camelCase` fonctions/vars, `SCREAMING_SNAKE_CASE` constantes/enums
- **Room** : schémas exportés dans `app/schemas/`, toute migration explicite dans `AgoraDatabase.kt`
- **Hilt** : `@HiltViewModel` pour ViewModels, `@ActivityRetainedScoped` pour `MultiplayerSessionManager`, `@Singleton` pour Repository/DAO

---

## Décisions architecturales

### 1. Questions en assets JSON (pas de réseau)
`AssetQuestionLoader` lit `assets/questions/<THEME>_<DIFFICULTY>.json` et insère dans Room.  
**Raison** : hors-ligne total, pas de backend.

### 2. Reset progression si nouvelles questions
Dans `QuestionRepositoryImpl.syncFromAssets` : si nouvelles questions détectées → reset `ProgressEntity` + `lot`.  
**Raison** : éviter incohérence d'index entre versions du JSON.

### 3. QR codes seed-based (depuis v2.2.0)
Le QR de l'organisateur ne contient **plus les questions** — seulement un seed + métadonnées.  
Les questions sont régénérées de façon déterministe sur chaque appareil.  
**Raison** : capacité QR limitée (~4296 chars alphanumériques pour QR v40). 20-30 questions en JSON dépasseraient la limite.

### 4. Pas de serveur multijoueur
La session passe uniquement par QR scannés localement.  
**Raison** : pas de coût d'infrastructure, fonctionne sans internet.

### 5. Pas de minification R8
`isMinifyEnabled = false` en release.  
**Raison** : règles ProGuard non définies pour Hilt + Room + ZXing. À activer prudemment.

### 6. Vibration : API conditionnelle
`VibrationHelper` utilise `VibratorManager` (API 31+) ou `Vibrator` déprécié (API 26-30) avec `@Suppress`.  
Vibration sur scan organisateur (`vibrateOnScan` 150ms) et sur scan QR organisateur par participant.

### 7. Système OTD supprimé (v1.5.1)
`OtdApiService` et `fetchAndCacheQuestions` définitivement remplacés par le pipeline LLM.

---

## Roadmap / Idées futures

- **Nouveaux thèmes** : Gastronomie, Jeux vidéo (assets JSON à générer)
- **Minification R8** : définir les règles ProGuard pour activer `isMinifyEnabled = true`
- **Mode Expert en multijoueur** : actuellement seuls Débutant + Moyen sont utilisés
- **Persistance des questions multijoueur** entre relances (actuellement rechargées à chaque scan)


| Couche | Technologie |
|---|---|
| Langage | Kotlin 2.0.0 |
| UI | Jetpack Compose + Material3 (BOM 2024.06.00) |
| Architecture | MVVM + Clean Architecture (Use Cases + Repository) |
| Injection | Hilt 2.51.1 (KAPT) |
| Base de données | Room 2.6.1 (DB locale, version 2, exportSchema) |
| Navigation | Navigation Compose 2.7.7 |
| Images | Coil 3.0.0 (coil-compose + coil-network-okhttp) |
| Réseau | OkHttp 4.12.0 + Retrofit 2.11.0 (images uniquement) |
| Préférences | DataStore Preferences 1.1.1 |
| Sérialisation | Gson 2.11.0 |
| SDK cible | Android 35, minSdk 26, JDK 17 |
| Build | Gradle 8.13 avec Kotlin DSL (.gradle.kts) |

## Structure des dossiers importants

```
agora/
├── app/src/main/
│   ├── assets/questions/          # JSON des questions (theme_difficulty.json)
│   ├── java/com/kapoue/agora/
│   │   ├── AgoraApplication.kt    # @HiltAndroidApp
│   │   ├── MainActivity.kt        # Point d'entrée, navigation Compose
│   │   ├── data/
│   │   │   ├── local/
│   │   │   │   ├── db/            # Room : AgoraDatabase, DAOs, Entities
│   │   │   │   │   ├── dao/       # QuestionDao, ProgressDao, ThemeProgressDao
│   │   │   │   │   └── entity/    # QuestionEntity, ProgressEntity, ThemeProgressEntity
│   │   │   │   ├── datastore/     # Préférences utilisateur
│   │   │   │   └── AssetQuestionLoader.kt  # Lecture des JSON depuis assets/
│   │   │   └── repository/        # QuestionRepositoryImpl, ProgressRepositoryImpl
│   │   ├── di/
│   │   │   ├── DatabaseModule.kt  # Room + DAOs
│   │   │   └── NetworkModule.kt   # ImageLoader Coil uniquement (plus d'OTD)
│   │   ├── domain/
│   │   │   ├── model/             # Theme.kt, Difficulty.kt, Question.kt, Progress.kt
│   │   │   └── usecase/           # GetQuestionsUseCase, GetProgressUseCase, SaveProgressUseCase
│   │   └── ui/
│   │       ├── screens/
│   │       │   ├── home/          # Sélection du thème
│   │       │   ├── difficulty/    # Sélection du niveau
│   │       │   ├── game/          # GameScreen + GameViewModel (cœur du jeu)
│   │       │   └── about/         # À propos
│   │       ├── components/        # Composables réutilisables
│   │       ├── navigation/        # NavGraph, routes
│   │       ├── theme/             # Couleurs, typographie Material3
│   │       └── util/              # Extensions, helpers UI
│   └── res/
│       ├── drawable/              # Icône vecteur + placeholders SVG par thème
│       └── mipmap-*/              # Icônes launcher (.webp)
├── scripts/
│   ├── prepare_release.py         # Génération questions + images + bump version + git
│   └── generate_questions.py      # Utilitaire standalone
├── local.properties               # Clés API + keystore (git-ignoré, NE PAS COMMITTER)
├── PRIVACY_POLICY.md              # Politique de confidentialité (Play Store)
└── gradle/libs.versions.toml      # Version catalog
```

## Commandes build / run / test

```bash
# Build debug
./gradlew assembleDebug

# Build release signé (APK)
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk

# Build release signé (AAB — Play Store)
./gradlew bundleRelease
# → app/build/outputs/bundle/release/app-release.aab

# Clean
./gradlew clean

# Tests unitaires
./gradlew test

# Tests instrumentés
./gradlew connectedAndroidTest
```

### Script de release complet (questions + images + version + git)

```bash
cd agora/
python scripts/prepare_release.py                    # Génère manquants + toutes images + bump patch
python scripts/prepare_release.py --force            # Régénère tout
python scripts/prepare_release.py --images-only      # Images seulement
python scripts/prepare_release.py --theme HISTOIRE --difficulty DEBUTANT
python scripts/prepare_release.py --no-git           # Sans commit/tag/push
```

Le script bumpe automatiquement le versionCode/versionName, commit, tag `vX.Y.Z` et push vers GitHub + Codeberg.

### Git remotes

```bash
git remote -v
# origin  git@github.com:kapoue/agora.git (fetch)
# origin  https://github.com/kapoue/agora.git (push)   ← GitHub
# origin  git@codeberg.org:kapoue/agora.git (push)     ← Codeberg (SSH)
# codeberg git@codeberg.org:kapoue/agora.git (fetch/push)
```

Un simple `git push` envoie simultanément sur GitHub et Codeberg.

## local.properties (git-ignoré)

Ce fichier contient les secrets — ne jamais committer :

```
MISTRAL_API_KEY=...
GEMINI_API_KEY_1=...   # jusqu'à _9
KEYSTORE_PATH=/chemin/vers/agora-release.jks
KEYSTORE_PASSWORD=...
KEY_ALIAS=agora
KEY_PASSWORD=...
UNSPLASH_ACCESS_KEY=...
PEXELS_API_KEY=...
PIXABAY_API_KEY=...
```

## Conventions de code

- **Kotlin idiomatique** : data classes, sealed classes, flow, coroutines
- **Un fichier = une responsabilité** : pas de classes God object
- **Nommage** : `PascalCase` pour classes, `camelCase` pour fonctions/variables, `SCREAMING_SNAKE` pour constantes et enums
- **Composables** : préfixe descriptif du contenu (`GameScreen`, `QuestionCard`, `ThemeGrid`), paramètre `modifier: Modifier = Modifier` toujours en dernière position avant les lambdas
- **ViewModels** : état UI dans une `data class` suffixée `UiState`, exposée via `StateFlow<XxxUiState>`
- **Use Cases** : une seule méthode publique `operator fun invoke(...)`, injectés dans les ViewModels
- **Room** : schémas exportés dans `app/schemas/`, migrations explicites dans `AgoraDatabase`

## Décisions architecturales notables

### 1. Questions stockées en assets JSON (pas en réseau)
Les questions sont dans `assets/questions/<THEME>_<DIFFICULTY>.json`.  
`AssetQuestionLoader` les lit au démarrage et `syncFromAssets` les insère dans Room.  
**Raison** : fonctionnement hors-ligne total, pas de backend à maintenir.

### 2. Reset du combo lors d'une MAJ de questions
Dans `QuestionRepositoryImpl.syncFromAssets` : si de nouvelles questions sont détectées pour une combinaison thème×difficulté, la progression (`ProgressEntity`) et le combo de questions actif (`lot`) sont remis à zéro.  
**Raison** : éviter des incohérences d'index entre ancienne et nouvelle version du JSON.

### 3. Génération LLM : Mistral prioritaire, Gemini en fallback
Le script `prepare_release.py` utilise `MistralProvider` (mistral-small-latest) en premier.  
Si erreur 429/403/401, il bascule sur `GeminiProvider` (gemini-2.5-flash) avec rotation automatique des clés.  
**Raison** : quota Gemini limité par projet GCP ; Mistral plus fiable et moins restrictif.

### 4. Pas de minification R8
`isMinifyEnabled = false` en release.  
**Raison** : configuration ProGuard non testée pour Hilt + Room + Coil. À activer prudemment avec règles `-keep` appropriées.

### 5. Système OTD supprimé
L'ancien système "Open Trivia Database" (`OtdApiService`, `fetchAndCacheQuestions`, `incrementAttempts`) a été entièrement supprimé en v1.5.1.  
Ne pas le réintroduire — le pipeline LLM le remplace définitivement.

### 6. Base de données Room version 2
Migration 1→2 : ajout colonne `lot` et `attempts` sur `questions`, création table `theme_progress`.  
Toute nouvelle migration doit être déclarée dans `AgoraDatabase.kt` et son SQL testé.

### 7. Images non embarquées
Les images sont des URLs chargées à la volée par Coil depuis Unsplash/Pexels/Pixabay.  
En cas d'absence de réseau, le placeholder vectoriel du thème (`R.drawable.placeholder_<theme>`) est affiché.
