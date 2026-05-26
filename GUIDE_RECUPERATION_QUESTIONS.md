# Guide — Récupération des questions et réponses dans Agora

## Vue d'ensemble du pipeline

```
scripts/generate_questions.py
         │
         ▼ génération (hors ligne, à la demande)
app/src/main/assets/questions/{THEME}_{DIFFICULTE}.json
         │
         ▼ au lancement d'une partie
AssetQuestionLoader  ──────────────►  QuestionRepositoryImpl.syncFromAssets()
                                                │
                                                ▼ diff (nouvelles questions uniquement)
                                          Room (QuestionEntity)
                                                │
                                                ▼ au démarrage de GameViewModel
                                          pendingQueue (ArrayDeque<Question>)
                                                │
                                                ▼ une par une
                                          GameScreen (UI)
```

---

## Étape 1 — Génération des fichiers JSON (hors application)

Les questions sont générées **en dehors de l'APK**, par le script Python :

```
agora/scripts/generate_questions.py
```

Il appelle l'API **Mistral** (`mistral-small-latest`) pour produire des QCM en français, avec **Gemini** (`gemini-2.5-flash`) en fallback automatique.
La sortie est un tableau JSON par combinaison thème × difficulté.

**Emplacement des fichiers générés :**

```
app/src/main/assets/questions/
├── HISTOIRE_DEBUTANT.json
├── HISTOIRE_MOYEN.json
├── HISTOIRE_EXPERT.json
├── GEOGRAPHIE_DEBUTANT.json
... (39 fichiers au total — 13 thèmes × 3 difficultés)
```

**Format de chaque fichier :**

```json
[
  {
    "question": "Quelle est la capitale de la France ?",
    "correct_answer": "Paris",
    "incorrect_answers": ["Lyon", "Marseille", "Bordeaux"]
  }
]
```

30 questions par fichier.

---

## Étape 2 — Chargement dans l'application : `AssetQuestionLoader`

**Fichier :** `data/local/AssetQuestionLoader.kt`

Au démarrage d'une partie, `AssetQuestionLoader.loadQuestions(theme, difficulty)` :
1. Ouvre le fichier `assets/questions/{THEME}_{DIFFICULTE}.json` depuis les assets Android
2. Parse le JSON via Gson (`List<AssetQuestionDto>`)
3. Convertit chaque objet en `QuestionEntity` pour Room :
   - `unsplashQuery` → `theme.unsplashQuery` (ex: `"ancient history monument"`)
   - `positionInPool` → index dans le tableau JSON
   - `imageUrl` → `null` (sera remplie plus tard par le provider d'images)
   - `isAnsweredCorrectly` → `false` par défaut

---

## Étape 3 — Synchronisation différentielle : `syncFromAssets`

**Fichier :** `data/repository/QuestionRepositoryImpl.kt`

`syncFromAssets(theme, difficulty)` est appelé à chaque démarrage de partie. Il est **différentiel** : il ne réinsère pas les questions déjà présentes dans Room.

Logique :
1. `assetQuestionLoader.loadQuestions()` → liste complète des questions du fichier JSON
2. `questionDao.getQuestionTexts()` → ensemble des questions déjà en base (par texte)
3. Filtre : `toInsert = newEntities.filter { it.questionText !in existingTexts }`
4. `questionDao.insertQuestions(toInsert)` → seules les nouvelles questions sont insérées
5. `questionDao.normalizeUnsplashQuery()` → uniformise la query Unsplash sur les lignes sans image

**Avantage :** La progression (`isAnsweredCorrectly`) des questions existantes est préservée lors d'une mise à jour de l'APK avec de nouvelles questions.

---

## Étape 4 — Construction du pool de jeu : `pendingQueue`

**Fichier :** `ui/screens/game/GameViewModel.kt`

Après `syncFromAssets`, le `GameViewModel` construit une file de jeu :

```kotlin
val allQuestions = questionRepository.getQuestions(theme, difficulty).first()
pendingQueue = ArrayDeque(allQuestions.filter { !it.isAnsweredCorrectly })
```

Seules les questions **non encore réussies** entrent dans la file.

**Règles de circulation dans le pool :**

| Action de l'utilisateur | Comportement |
|---|---|
| Bonne réponse | Question marquée `isAnsweredCorrectly = true` en base → retirée définitivement de la file |
| Mauvaise réponse | Question remise en fin de file (`pendingQueue.addLast(question)`) → reviendra plus tard |
| File vide | `isCompleted = true` → écran "Félicitations" affiché |

---

## Étape 5 — Images contextuelles : rotation Unsplash → Pexels → Pixabay

**Fichier :** `data/repository/ImageRepositoryImpl.kt`

Pour chaque question, `fetchImageForQuestion()` est appelé depuis le `GameViewModel` :

```
tryUnsplash(query)
    ├── OK (HTTP 200 + résultats) → cache 10 URLs, retourne 1 aléatoire
    └── Erreur (403, 429, réseau) → tryPexels(query)
                                        ├── OK → cache 10 URLs, retourne 1 aléatoire
                                        └── Erreur → tryPixabay(query)
                                                        ├── OK → cache 10 URLs, retourne 1 aléatoire
                                                        └── Erreur → null (placeholder affiché)
```

**Cache `urlListCache` (en mémoire, par session) :**
- Clé : la `unsplashQuery` du thème (ex: `"ancient history monument"`)
- Valeur : liste de 10 URLs
- À chaque question : `urls.randomOrNull()` → image différente sans nouvel appel API
- **1 appel API par thème maximum** pour toute la session de jeu

**Clés API (jamais dans l'APK) :**

| Provider | Clé dans | Header / Param |
|---|---|---|
| Unsplash | `local.properties` → `BuildConfig.UNSPLASH_API_KEY` | `Authorization: Client-ID {key}` |
| Pexels | `local.properties` → `BuildConfig.PEXELS_API_KEY` | `Authorization: {key}` |
| Pixabay | `local.properties` → `BuildConfig.PIXABAY_API_KEY` | `?key={key}` (query param) |

---

## Récapitulatif des classes impliquées

| Classe | Rôle |
|---|---|
| `generate_questions.py` | Génération hors ligne via Mistral (Gemini en fallback) |
| `AssetQuestionLoader` | Lecture des JSON depuis les assets |
| `QuestionRepositoryImpl` | Sync différentielle assets → Room, gestion OTD (legacy) |
| `QuestionDao` | Accès base de données Room |
| `GameViewModel` | Construction et gestion du `pendingQueue` |
| `ImageRepositoryImpl` | Rotation Unsplash → Pexels → Pixabay avec cache |
| `GameScreen` | Affichage question, réponses, image de fond floutée |

---

## Schéma Room — table `questions`

| Colonne | Type | Description |
|---|---|---|
| `id` | Long (PK autoincrement) | Identifiant unique |
| `theme` | String | Nom de l'enum `Theme` (ex: `HISTOIRE`) |
| `difficulty` | String | Nom de l'enum `Difficulty` (ex: `DEBUTANT`) |
| `questionText` | String | Texte de la question |
| `correctAnswer` | String | Bonne réponse |
| `incorrectAnswers` | String (JSON) | 3 mauvaises réponses sérialisées |
| `imageUrl` | String? | URL image (null si pas encore chargée) |
| `unsplashQuery` | String | Requête utilisée pour chercher l'image |
| `isAnsweredCorrectly` | Boolean | `true` si déjà réussie → exclue du pool |
| `positionInPool` | Int | Ordre initial dans le fichier JSON |
