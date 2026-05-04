# Agora — Quiz culturel Android

Application Android de quiz culturel en français, développée en Kotlin avec Jetpack Compose.

## Fonctionnalités

- **13 thèmes** : Histoire, Géographie, Sciences, Cinéma, Musique, Sport, Littérature, Informatique, Art, Mathématiques, Mythologie, Animaux, Véhicules
- **3 niveaux de difficulté** par thème (Débutant, Moyen, Expert)
- **Progression sauvegardée** : reprend là où on s'est arrêté
- **Système de lots/séries** : compteur de parties complètes par thème, badge visible dès 3 séries
- **Statistiques de session** : nombre d'erreurs avec message bienveillant affiché sur l'écran de félicitations
- **Images illustratives** associées à chaque question
- **Rejouer** : une fois les 3 difficultés complétées, réinitialise le thème pour une nouvelle série

## Stack technique

- Kotlin 2.0.0 · Jetpack Compose BOM 2024.06.00 · Material3
- Architecture MVVM + Clean Architecture (Use Cases, Repository pattern)
- Hilt 2.51.1 (injection de dépendances)
- Room 2.6.1 (base de données locale avec migrations)
- Retrofit 2.11.0 + OkHttp (API Open Trivia Database)
- Coil 3 (chargement d'images)
- Navigation Compose

## Mise à jour du contenu

Les questions et images sont gérées via un script Python (`scripts/prepare_release.py`).  
Voir [GUIDE_MAJ_CONTENU.md](GUIDE_MAJ_CONTENU.md) pour le workflow complet.

## Build

```bash
./gradlew assembleDebug    # build debug
./gradlew assembleRelease  # build release
```

Requiert : Android SDK 35, JDK 17, `local.properties` avec les clés API.

## Licence

[MIT](LICENSE)
