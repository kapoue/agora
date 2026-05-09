# CLAUDE.md — Agora

Fichier de contexte pour Claude Code. Lit ce fichier en premier avant toute intervention.

## Description du projet

**Agora** est une application Android de quiz culturel.  
- 13 thèmes : Histoire, Géographie, Sciences, Cinéma, Musique, Sport, Littérature, Informatique, Art, Mathématiques, Mythologie, Animaux, Véhicules  
- 3 niveaux de difficulté : Débutant, Moyen, Expert  
- Les questions sont générées par LLM (Mistral AI prioritaire, Gemini en fallback) et stockées en JSON dans les assets  
- Chaque question est illustrée par une image chargée depuis une URL (Unsplash / Pexels / Pixabay)  
- Application ID : `fr.kapoue.agora` — version actuelle : `1.5.2` (versionCode 18)

## Stack technique

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
