# Guide de mise à jour des questions — Agora

## Vue d'ensemble

Les questions de l'application sont générées via l'API Gemini (IA Google) et stockées sous forme de fichiers JSON dans les assets de l'APK. Elles sont chargées dans la base de données Room au premier lancement de chaque thème.

**Structure** : `app/src/main/assets/questions/{THEME}_{DIFFICULTE}.json`

---

## Prérequis

- Python 3.8+ installé
- Environnement virtuel `.venv` à la racine de `c:\Users\kapou\www`
- Clé API Gemini (voir section dédiée)

### Installation des dépendances (une seule fois)
```powershell
cd c:\Users\kapou\www
.\.venv\Scripts\pip.exe install google-genai
```

---

## Régénérer toutes les questions

```powershell
cd c:\Users\kapou\www
.\.venv\Scripts\python.exe agora\scripts\generate_questions.py
```

> Les fichiers déjà existants sont ignorés automatiquement.

### Forcer la régénération complète
```powershell
.\.venv\Scripts\python.exe agora\scripts\generate_questions.py --force
```

---

## Régénérer un thème ou une difficulté spécifique

```powershell
# Un seul thème (toutes difficultés)
.\.venv\Scripts\python.exe agora\scripts\generate_questions.py --theme HISTOIRE --force

# Une seule difficulté (tous thèmes)
.\.venv\Scripts\python.exe agora\scripts\generate_questions.py --difficulty EXPERT --force

# Combinaison précise
.\.venv\Scripts\python.exe agora\scripts\generate_questions.py --theme SPORT --difficulty MOYEN --force
```

### Noms valides pour `--theme`
| Argument | Thème affiché |
|----------|---------------|
| `HISTOIRE` | Histoire |
| `GEOGRAPHIE` | Géographie |
| `SCIENCES` | Sciences & Nature |
| `CINEMA` | Cinéma |
| `MUSIQUE` | Musique |
| `SPORT` | Sport |
| `LITTERATURE` | Littérature |
| `INFORMATIQUE` | Informatique |
| `ART` | Art |
| `MATHEMATIQUES` | Mathématiques |
| `MYTHOLOGIE` | Mythologie |
| `ANIMAUX` | Animaux |
| `VEHICULES` | Véhicules |

### Noms valides pour `--difficulty`
| Argument | Niveau affiché |
|----------|----------------|
| `DEBUTANT` | Débutant |
| `MOYEN` | Moyen |
| `EXPERT` | Expert |

---

## Après la génération : builder et installer

```powershell
cd c:\Users\kapou\www\agora
.\gradlew.bat assembleDebug
adb install -r "app\build\outputs\apk\debug\app-debug.apk"
```

> **Important** : Les nouvelles questions ne s'affichent qu'après désinstallation/réinstallation, OU si la base de données Room est vidée. En mode debug, désinstaller l'appli puis réinstaller suffit.

---

## Ajouter un nouveau thème

### 1. Ajouter le thème dans le script Python

Dans `agora/scripts/generate_questions.py`, ajouter une entrée dans le dictionnaire `THEMES` :

```python
THEMES = {
    ...
    "CUISINE": "Cuisine (recettes du monde, ingrédients, techniques culinaires, chefs célèbres, gastronomie)",
}
```

### 2. Ajouter le thème dans l'application Android

Dans `app/src/main/java/com/kapoue/agora/domain/model/Theme.kt` :

```kotlin
enum class Theme(
    val displayName: String,
    val emoji: String,
    val unsplashQuery: String,
    val otdCategoryId: Int
) {
    ...
    CUISINE("Cuisine", "🍳", "food cooking kitchen", 0),
}
```

### 3. Générer les questions du nouveau thème

```powershell
.\.venv\Scripts\python.exe agora\scripts\generate_questions.py --theme CUISINE
```

### 4. Builder et installer

```powershell
cd c:\Users\kapou\www\agora
.\gradlew.bat assembleDebug
adb install -r "app\build\outputs\apk\debug\app-debug.apk"
```

---

## Changer le nombre de questions par combinaison

Dans `agora/scripts/generate_questions.py`, modifier la constante en haut du fichier :

```python
QUESTIONS_PER_COMBO = 30  # ← changer cette valeur
```

Puis régénérer avec `--force` les combinaisons souhaitées.

---

## Changer la clé API Gemini

La clé API est dans `agora/scripts/generate_questions.py` :

```python
GEMINI_API_KEY = "AIzaSy..."  # ← remplacer ici
```

> **Sécurité** : La clé n'est utilisée que par le script Python local. Elle n'est jamais intégrée dans l'APK.

Pour obtenir une nouvelle clé : [Google AI Studio](https://aistudio.google.com/apikey)

---

## Format des fichiers JSON

Chaque fichier `{THEME}_{DIFFICULTE}.json` contient un tableau d'objets :

```json
[
  {
    "question": "Quelle est la capitale de la France ?",
    "correct_answer": "Paris",
    "incorrect_answers": ["Lyon", "Marseille", "Bordeaux"]
  }
]
```

---

## Fonctionnement interne

1. Le script Python appelle l'API Gemini et sauvegarde les JSON dans `app/src/main/assets/questions/`
2. Au premier lancement d'un thème+difficulté, `AssetQuestionLoader` lit le JSON depuis les assets
3. Les questions sont insérées dans Room (base locale) via `QuestionRepositoryImpl.seedFromAssets()`
4. Les lancements suivants lisent directement depuis Room (pas de rechargement des assets)
5. Les images Unsplash sont chargées à la demande et mises en cache dans Room

---

## Dépannage

### Le script retourne une erreur 429
L'API Gemini est rate-limitée. Le script retry automatiquement. Si ça persiste, attendre quelques minutes et relancer.

### "Aucune question trouvée" dans l'application
Vérifier que le fichier JSON correspondant existe dans `app/src/main/assets/questions/`. Si non, relancer le script de génération puis rebuilder l'APK.

### Les questions ne se mettent pas à jour après régénération
Les questions sont cachées dans Room. Désinstaller l'application et réinstaller pour forcer le rechargement depuis les assets.
