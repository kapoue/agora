# Guide — Mise à jour du contenu Agora

## Principe

Le script `scripts/prepare_release.py` gère tout le contenu de l'application :

1. **Questions** — génération via Gemini AI (fichiers JSON dans `app/src/main/assets/questions/`)
2. **Images** — récupération d'URLs via Unsplash → Pexels → Pixabay, injectées dans chaque question
3. **Version** — monte automatiquement le patch (`x.y.z → x.y.z+1`) dans `app/build.gradle.kts`
4. **Git** — fait le commit + tag + push automatiquement

Les clés API (Unsplash, Pexels, Pixabay, Gemini) ne sont **jamais** dans le code.  
Les clés Gemini sont dans le script. Les clés images sont lues depuis `local.properties`.

---

## Prérequis

```
c:\Users\kapou\www\.venv\Scripts\python.exe  ← venv Python
c:\Users\kapou\www\agora\local.properties    ← clés API images
```

`local.properties` doit contenir :
```
UNSPLASH_API_KEY=votre_clé
PEXELS_API_KEY=votre_clé
PIXABAY_API_KEY=votre_clé
```

---

## Commandes courantes

### Mise à jour standard (images seulement, version bump + git)
```powershell
cd c:\Users\kapou\www
.\.venv\Scripts\python.exe agora\scripts\prepare_release.py --images-only
```
→ Rafraîchit toutes les images (30 URLs par thème × difficulté)  
→ Monte la version patch : ex. `1.3.0 → 1.3.1`  
→ Commit + tag `v1.3.1` + push

### Mise à jour images sans git (pour tester d'abord)
```powershell
.\.venv\Scripts\python.exe agora\scripts\prepare_release.py --images-only --no-git
```

### Régénérer questions + images d'un seul thème
```powershell
.\.venv\Scripts\python.exe agora\scripts\prepare_release.py --theme HISTOIRE --force
```

### Régénérer tout (toutes les questions + images)
```powershell
.\.venv\Scripts\python.exe agora\scripts\prepare_release.py --force
```
⚠ Long (~20 min) — consomme les quotas Gemini

### Sans montée de version ni git (test local)
```powershell
.\.venv\Scripts\python.exe agora\scripts\prepare_release.py --images-only --no-git
```

---

## Ce que fait le script en détail

```
Pour chaque thème × difficulté (ex: HISTOIRE × DEBUTANT) :
  1. Si manquant ou --force → génère 30 questions via Gemini
  2. Fetche 30 URLs d'images (page différente par difficulté)
     Unsplash → Pexels (fallback) → Pixabay (fallback)
  3. Injecte imageUrl dans chaque question du JSON

Si tout réussit :
  - versionCode += 1
  - versionName x.y.z → x.y.(z+1)
  - git add -A
  - git commit -m "content: inject image URLs + bump version to x.y.(z+1)"
  - git tag vx.y.(z+1)
  - git push origin main + tag
```

---

## Thèmes disponibles

| Clé              | Affichage              |
|------------------|------------------------|
| HISTOIRE         | Histoire               |
| GEOGRAPHIE       | Géographie             |
| SCIENCES         | Sciences & Nature      |
| CINEMA           | Cinéma                 |
| MUSIQUE          | Musique                |
| SPORT            | Sport                  |
| LITTERATURE      | Littérature            |
| ART              | Art & Architecture     |
| GASTRONOMIE      | Gastronomie            |
| TECHNOLOGIE      | Technologie            |
| MATHEMATIQUES    | Mathématiques          |
| MYTHOLOGIE       | Mythologie             |
| ANIMAUX          | Animaux                |
| VEHICULES        | Véhicules              |

## Difficultés disponibles
`DEBUTANT` · `INTERMEDIAIRE` · `EXPERT`

---

## Construire l'APK après mise à jour

```powershell
cd c:\Users\kapou\www\agora
.\gradlew.bat assembleRelease
```

APK généré : `app\build\outputs\apk\release\app-release.apk`

## Installer sur l'appareil (Wi-Fi ADB)

```powershell
adb connect 192.168.1.42:32827
adb install -r "app\build\outputs\apk\release\app-release.apk"
```
