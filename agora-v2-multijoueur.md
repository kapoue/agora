# AGORA v2.0.0 — Mode Multijoueur Local
## Document de spécification technique

> À remettre à Claude Code au démarrage du développement v2.0.0.
> Ce document intègre les corrections et décisions prises lors de l'analyse du prompt initial (Lot 4).

---

## 1. Contexte

### Projet de base
- Application : **Agora** — quiz culturel Android natif
- Package : `com.kapoue.agora` (**NB : le prompt initial indiquait incorrectement `fr.kapoue.agora`**)
- Version de départ : **1.7.0** (versionCode 22) — 14 catégories dont "Culture Générale" (v1.6.0), sessions de 20 questions (v1.6.2), 60 questions/thème/difficulté avec images (v1.7.0)
- Stack : Kotlin 2.0.0 + Jetpack Compose + Material3 + Hilt + Room 2.6.1
- Référence complète : voir `CLAUDE.md` à la racine du projet

### Ce que v2.0.0 ajoute
Mode multijoueur **100% local, sans serveur, sans réseau entre les appareils**. Communication via QR codes uniquement. Un joueur organisateur crée la session, génère un QR code par manche. Les participants scannent, jouent, puis montrent leur QR code résultat à l'organisateur qui collecte les scores.

### Terminologie — stricte dans tout le code et l'UI
- **Session** : l'ensemble de la partie (ex : 3 manches de 10 questions)
- **Manche** : une série de questions
- **Organisateur** : crée la session, génère les QR codes, collecte les résultats
- **Participant** : rejoint via QR code
- Dans l'UI : "Organiser une partie" / "Rejoindre une partie", "manche" (jamais "round" ni "partie" pour désigner une manche)

---

## 2. Décisions architecturales

### 2.1 Passage de données entre écrans
**Problème** : Navigation Compose ne peut pas transporter des objets complexes. Le flux organisateur traverse ~6 écrans.  
**Solution** : `MultiplayerSessionManager` — un singleton Hilt `@ActivityRetainedScoped` qui maintient l'état de session en mémoire **ET** persiste en Room.

```kotlin
@ActivityRetainedScoped
class MultiplayerSessionManager @Inject constructor(
    private val sessionDao: MultiplayerSessionDao
) {
    var currentSession: MultiplayerSessionState? = null
    // Chaque écran lit/écrit via ce manager
}
```

### 2.2 Persistance de session
La session est sauvegardée en Room dès sa création. Si l'organisateur ferme l'app et revient, la session en cours est restaurée. Un participant peut aussi quitter et reprendre (son score est recalculé depuis les données locales).

### 2.3 Questions multijoueur
- Source : Room, toutes catégories confondues **sauf `CULTURE_GENERALE`**
- Difficultés : `DEBUTANT` + `MOYEN` uniquement (pas `EXPERT`)
- Pool disponible : **60 questions** par thème × difficulté (2230 questions au total dans les assets)
- Le mode solo utilise des sessions de 20 questions sur ce pool (bouton "Nouvelles questions") — ce mécanisme **ne s'applique pas** en multijoueur : toutes les questions de la manche sont jouées d'un coup sans pagination
- Méthode DAO déjà disponible depuis v1.6.x : `getRandomUnansweredQuestionsAllThemes()`

### 2.4 Images
Pas d'URL dans les QR codes (trop lourd). L'app participant utilise le cache Room local par catégorie. Méthode disponible depuis v1.6.x : `getRandomImageUrl()`.

---

## 3. Nouvelles dépendances

Ajouter dans `gradle/libs.versions.toml` (vérifier les versions exactes sur Maven Central au moment de l'implémentation) :

```toml
[versions]
zxing-core = "3.5.3"           # vérifier dernière version stable
zxing-android = "4.3.0"       # com.journeyapps:zxing-android-embedded

[libraries]
zxing-core = { group = "com.google.zxing", name = "core", version.ref = "zxing-core" }
zxing-android-embedded = { group = "com.journeyapps", name = "zxing-android-embedded", version.ref = "zxing-android" }
```

**Note** : ZXing Android Embedded est une lib View-based. Utiliser un wrapper `AndroidView` dans Compose pour l'écran de scan.

Permissions dans `AndroidManifest.xml` :
```xml
<uses-permission android:name="android.permission.CAMERA" />
<!-- VIBRATE déjà présente depuis v1.6.1, ne pas dupliquer -->
```

---

## 4. Modèles de données

### `MultiplayerQuestion.kt`
```kotlin
data class MultiplayerQuestion(
    val id: Long,
    val text: String,               // max 150 caractères — tronquer si nécessaire
    val correct: String,            // max 80 caractères
    val wrong: List<String>,        // 3 mauvaises réponses, max 80 caractères chacune
    val category: String            // Nom de l'enum Theme (ex : "HISTOIRE")
)
```

### `QrPayload.kt` — distribué par l'organisateur
```kotlin
data class QrPayload(
    val sessionId: String,          // UUID — identique sur toutes les manches de la session
    val roundNumber: Int,
    val totalRounds: Int,
    val questionsPerRound: Int,
    val questions: List<MultiplayerQuestion>
)
```

### `PlayerResult.kt` — généré par chaque participant
```kotlin
data class PlayerResult(
    val sessionId: String,
    val roundNumber: Int,
    val playerName: String,
    val score: Int,
    val totalQuestions: Int,
    val timeMillis: Long,
    val wrongAnswers: List<WrongAnswer>
)
```

### `WrongAnswer.kt`
```kotlin
data class WrongAnswer(
    val questionText: String,
    val correctAnswer: String,
    val givenAnswer: String
)
```

### `MultiplayerSessionEntity.kt` (Room — persistance)
```kotlin
@Entity(tableName = "multiplayer_sessions")
data class MultiplayerSessionEntity(
    @PrimaryKey val sessionId: String,
    val organizerName: String,
    val totalRounds: Int,
    val questionsPerRound: Int,
    val currentRound: Int,
    val status: String,             // "IN_PROGRESS" | "COMPLETED"
    val resultsJson: String,        // JSON de List<PlayerResult> cumulés
    val createdAt: Long
)
```

---

## 5. Encodage QR code

Gzip + Base64 pour minimiser la taille. Un QR code standard supporte ~3 000 caractères.

```kotlin
object QrPayloadEncoder {
    fun encodePayload(payload: QrPayload): String { /* gzip + base64 */ }
    fun decodePayload(encoded: String): QrPayload { /* base64 + gunzip */ }
    fun encodeResult(result: PlayerResult): String { /* gzip + base64 */ }
    fun decodeResult(encoded: String): PlayerResult { /* base64 + gunzip */ }
}
```

**Test obligatoire avant validation** : tester la taille du payload pour 10 et 20 questions.
Avec 20 questions, le payload gzip+base64 est estimé à ~1 150 chars — largement sous la capacité QR (~3 000 chars). **Maximum fixé à 20 questions par manche** — pas de QR multi-pages nécessaire.

---

## 6. Ordre aléatoire par participant

Même questions pour tous, ordre différent via seed déterministe :

```kotlin
fun shuffleQuestionsForPlayer(
    questions: List<MultiplayerQuestion>,
    playerName: String,
    sessionId: String,
    roundNumber: Int
): List<MultiplayerQuestion> {
    val seed = (playerName + sessionId + roundNumber).hashCode().toLong()
    return questions.shuffled(Random(seed))
}

fun shuffleAnswersForPlayer(
    question: MultiplayerQuestion,
    playerName: String,
    sessionId: String,
    questionIndex: Int
): List<String> {
    val seed = (playerName + sessionId + questionIndex).hashCode().toLong()
    return (listOf(question.correct) + question.wrong).shuffled(Random(seed))
}
```

---

## 7. Structure des fichiers à créer

```
app/src/main/java/com/kapoue/agora/
├── data/
│   └── local/db/
│       ├── dao/MultiplayerSessionDao.kt
│       └── entity/MultiplayerSessionEntity.kt
├── domain/model/
│   ├── MultiplayerQuestion.kt
│   ├── QrPayload.kt
│   ├── PlayerResult.kt
│   └── WrongAnswer.kt
├── multiplayer/
│   └── MultiplayerSessionManager.kt   ← singleton @ActivityRetainedScoped
├── ui/screens/multiplayer/
│   ├── MultiplayerHomeScreen.kt
│   ├── MultiplayerHomeViewModel.kt
│   ├── organizer/
│   │   ├── OrganizerSetupScreen.kt
│   │   ├── OrganizerSetupViewModel.kt
│   │   ├── OrganizerQrScreen.kt
│   │   ├── OrganizerGameScreen.kt
│   │   ├── OrganizerGameViewModel.kt
│   │   ├── OrganizerScanScreen.kt
│   │   ├── OrganizerScanViewModel.kt
│   │   ├── OrganizerRoundResultsScreen.kt
│   │   └── OrganizerFinalResultsScreen.kt
│   └── participant/
│       ├── ParticipantScanScreen.kt
│       ├── ParticipantScanViewModel.kt
│       ├── ParticipantSetupScreen.kt
│       ├── ParticipantSetupViewModel.kt
│       ├── ParticipantGameScreen.kt
│       ├── ParticipantGameViewModel.kt
│       └── ParticipantRoundResultScreen.kt
└── ui/util/
    ├── QrCodeGenerator.kt
    ├── QrCodeScanner.kt
    ├── QrPayloadEncoder.kt
    └── VibrationHelper.kt
```

---

## 8. Navigation — nouvelles routes

Ajouter dans `Screen.kt` :
```kotlin
object MultiplayerHome : Screen("multiplayer_home")
object OrganizerSetup : Screen("organizer_setup")
object OrganizerQr : Screen("organizer_qr/{roundNumber}/{totalRounds}") {
    fun createRoute(roundNumber: Int, totalRounds: Int) = "organizer_qr/$roundNumber/$totalRounds"
}
object OrganizerGame : Screen("organizer_game")
object OrganizerScan : Screen("organizer_scan/{roundNumber}/{totalRounds}") {
    fun createRoute(roundNumber: Int, totalRounds: Int) = "organizer_scan/$roundNumber/$totalRounds"
}
object OrganizerRoundResults : Screen("organizer_round_results/{roundNumber}/{totalRounds}") {
    fun createRoute(roundNumber: Int, totalRounds: Int) = "organizer_round_results/$roundNumber/$totalRounds"
}
object OrganizerFinalResults : Screen("organizer_final_results")
object ParticipantScan : Screen("participant_scan")
object ParticipantSetup : Screen("participant_setup")
object ParticipantGame : Screen("participant_game")
object ParticipantRoundResult : Screen("participant_round_result/{roundNumber}/{totalRounds}") {
    fun createRoute(roundNumber: Int, totalRounds: Int) = "participant_round_result/$roundNumber/$totalRounds"
}
object WrongAnswers : Screen("wrong_answers/{roundNumber}") {
    fun createRoute(roundNumber: Int) = "wrong_answers/$roundNumber"
}
```

---

## 9. Modification de HomeScreen

Ajouter une icône groupe (`Icons.Outlined.Group`) dans le header, à gauche de l'icône (i), avec 24dp d'espace entre les deux :

```
[AGORA]                    [👥]  [i]
```

- Couleur icône : `AgoraStone`, taille 24dp
- Au clic : naviguer vers `MultiplayerHomeScreen`
- Ajouter `onMultiplayerClick: () -> Unit` à la signature de `HomeScreen`
- Mettre à jour `AgoraNavGraph.kt` en conséquence

---

## 10. Flux organisateur (résumé)

1. **MultiplayerHomeScreen** → card "Organiser une partie" → `OrganizerSetupScreen`
2. **OrganizerSetupScreen** : prénom (DataStore, pré-rempli), choix **10/20 questions** par manche, choix 1/2/3 manches → génère UUID session + sélectionne questions depuis Room (DEBUTANT+MOYEN, toutes catégories sauf CULTURE_GENERALE) → sauvegarde en Room
3. **OrganizerQrScreen** : QR code affiché fond blanc, "Manche N / T" → bouton "Je suis prêt à jouer"
4. **OrganizerGameScreen** : jeu identique à `ParticipantGameScreen` — chrono démarre au clic "Démarrer", s'arrête à la dernière réponse
5. **OrganizerScanScreen** : son propre score affiché immédiatement (ajouté au tableau en 1ère position), bouton "+" pour scanner QR codes résultats participants, tableau mis à jour en temps réel, bouton "Fin des scans"
6. **OrganizerRoundResultsScreen** (si pas dernière manche) : classement manche, bouton "Lancer manche N+1"
7. **OrganizerFinalResultsScreen** (dernière manche) : podium visuel (style grec antique) + tableau cumulé, boutons "Nouvelle partie" et "Accueil"

---

## 11. Flux participant (résumé)

1. **MultiplayerHomeScreen** → card "Rejoindre une partie" → `ParticipantScanScreen`
2. **ParticipantScanScreen** : scan caméra → décode `QrPayload`, vibration courte → `ParticipantSetupScreen`
3. **ParticipantSetupScreen** : prénom (DataStore, pré-rempli), texte "Ce n'est pas toi ? Modifie ton prénom." → prefetch 5 premières images → bouton "Prêt !"
4. **ParticipantGameScreen** :
   - Image de fond par catégorie (cache Room), floutée, overlay sombre
   - "Question X / N" en Cinzel AgoraGold
   - Bouton "Démarrer" → chrono start
   - 4 réponses — passage automatique après 600ms (vert=correct / rouge=faux) — **pas de bouton Suivant**
   - Chrono s'arrête à la dernière réponse
   - Prefetch N+1 et N+2 pendant chaque question
5. **ParticipantRoundResultScreen** : score + temps + QR code résultat (double vibration à l'apparition), bouton "Voir mes erreurs", bouton "Scanner manche N+1" ou message "Partie terminée"

---

## 12. Règles comportement jeu multijoueur

- **Pas de bouton Suivant** — passage automatique après 600ms
- **Chrono** : démarre au clic "Démarrer", s'arrête quand le participant répond à la dernière question
- **Chrono non affiché** pendant le jeu
- **Questions mal répondues ne reviennent pas** (contrairement au mode solo)
- **Pas d'explication de réponse** — on va vite
- **Pas d'appel réseau Unsplash/Pexels/Pixabay** pendant le jeu — cache local uniquement

---

## 13. Gestion des éliminés

Un participant est éliminé **définitivement** si :
1. Il ne scanne pas le QR code d'une manche (l'organisateur clique "Fin des scans" sans lui)
2. Il abandonne en cours de manche (pas de QR résultat présenté)

**Option B retenue** : élimination définitive. Un participant éliminé ne figure pas au podium final, même s'il revient pour des manches ultérieures. S'il revient et scanne le QR d'une manche suivante, l'app lui indique qu'il est hors-classement — il peut jouer pour s'amuser mais son score n'est pas collecté.

Affichage dans les tableaux intermédiaires (manches non-finales) : nom en gris `AgoraStone`, texte barré, listé en bas.  
Le bouton "Fin des scans" clôture la collecte — les non-scannés sont automatiquement marqués éliminés.

---

## 14. Classement cumulé

- Score cumulé = somme des bonnes réponses sur toutes les manches jouées
- Temps cumulé = somme des temps sur toutes les manches
- Tri : score décroissant, temps croissant en cas d'ex-æquo
- Les éliminés apparaissent en bas avec leur score partiel

---

## 15. Vibrations

```kotlin
object VibrationHelper {
    fun vibrateOnScan(context: Context)   // 150ms — scan réussi
    fun vibrateOnResult(context: Context) // pattern 0, 150, 100, 150 — QR résultat affiché
}
```

Moments :
- Organisateur : `vibrateOnScan()` à chaque scan réussi de QR résultat participant
- Participant : `vibrateOnScan()` au scan du QR organisateur
- Participant : `vibrateOnResult()` à l'apparition du QR résultat

---

## 16. Sécurisation des QR codes

Côté organisateur (scan résultats) — vérifier :
1. `sessionId` correspond à la session en cours → sinon "QR code invalide — mauvaise session"
2. `roundNumber` correspond à la manche en cours → sinon "QR code invalide — mauvaise manche"
3. `playerName` pas déjà scanné pour cette manche → sinon "Ce joueur a déjà été scanné"

Côté participant (scan QR organisateur) — vérifier :
1. Le payload se décode correctement → sinon "QR code invalide"
2. Si session en cours sur ce téléphone : confirmer si `sessionId` différent → "Nouvelle session détectée — rejoindre ?"

---

## 17. Gestion du prénom

Sauvegardé en **DataStore** après première saisie. Pré-rempli sur tous les écrans de setup.  
Texte discret sous le champ : "Ce n'est pas toi ? Modifie ton prénom."

---

## 18. Écran "Voir mes erreurs"

Accessible depuis `ParticipantRoundResultScreen`.

Pour chaque question ratée :
- Texte question (Lato AgoraWhite 16sp)
- "Ta réponse : [réponse donnée]" en AgoraWrongLight 14sp
- "Bonne réponse : [bonne réponse]" en AgoraCorrectLight 14sp
- Séparateur fin AgoraStone

---

## 19. Modifications des fichiers existants

| Fichier | Modification |
|---|---|
| `HomeScreen.kt` | Ajouter `onMultiplayerClick`, icône groupe dans header |
| `AgoraNavGraph.kt` | Ajouter toutes les nouvelles routes |
| `Screen.kt` | Ajouter les nouveaux objets Screen |
| `AgoraDatabase.kt` | Ajouter `MultiplayerSessionEntity`, migration version **3** (la DB est déjà en version 2 depuis v1.x) |
| `DatabaseModule.kt` | Ajouter `MultiplayerSessionDao` |

**Ne pas modifier** : GameViewModel, QuestionRepository, logique solo existante.

---

## 20. Migration Room

La DB passe de version 2 à version 3 :

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS multiplayer_sessions (
                sessionId TEXT NOT NULL PRIMARY KEY,
                organizerName TEXT NOT NULL,
                totalRounds INTEGER NOT NULL,
                questionsPerRound INTEGER NOT NULL,
                currentRound INTEGER NOT NULL,
                status TEXT NOT NULL,
                resultsJson TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """)
    }
}
```

---

## 21. Commandes build et commit

```bash
# Build debug
./gradlew assembleDebug

# Installer sur appareil connecté
adb install app/build/outputs/apk/debug/app-debug.apk

# Commit de fin
git add .
git commit -m "feat: mode multijoueur local via QR codes (v2.0.0)"
git push
# Le push envoie simultanément sur GitHub et Codeberg
```

---

## 22. Critères de validation

- [ ] Icône groupe dans le header de l'accueil, à gauche du (i)
- [ ] Deux cards "Organiser" / "Rejoindre" sur MultiplayerHomeScreen
- [ ] Prénom pré-rempli depuis DataStore, modifiable
- [ ] Choix 10/20/30 questions et 1/2/3 manches
- [ ] QR code manche généré et affiché fond blanc
- [ ] Participant scanne et reçoit les questions
- [ ] Questions dans ordre différent selon prénom (seed déterministe)
- [ ] Réponses dans ordre différent selon prénom
- [ ] Chrono démarre au clic "Démarrer"
- [ ] Chrono s'arrête à la dernière réponse (pas au chargement)
- [ ] Bonne réponse → vert 600ms → question suivante automatique
- [ ] Mauvaise réponse → rouge 600ms → question suivante automatique
- [ ] Images affichées depuis cache Room par catégorie, fallback placeholder
- [ ] Prefetch N+1 et N+2 pendant chaque question
- [ ] Prefetch 5 premières images pendant écran setup participant
- [ ] Score et temps : "X / N en Y min Z sec"
- [ ] QR code résultat : prénom + score + temps + erreurs
- [ ] Double vibration à l'apparition du QR résultat
- [ ] Vibration courte aux scans réussis
- [ ] Organisateur voit son score immédiatement sur OrganizerScanScreen
- [ ] Tableau mis à jour en temps réel à chaque scan
- [ ] Vérification sessionId + roundNumber (QR invalide rejeté)
- [ ] Bouton "Fin des scans" → absents éliminés
- [ ] Éliminés : gris barré dans tous les tableaux
- [ ] Podium final (style grec) avec scores et temps cumulés
- [ ] Classement général : score cumulé décroissant, temps cumulé croissant
- [ ] "Voir mes erreurs" fonctionne
- [ ] Session persistée en Room (reprise après fermeture app)
- [ ] `./gradlew assembleDebug` passe sans erreur
- [ ] Code commité et pushé via `git push`

---

*Fin de la spécification v2.0.0 — Agora Mode Multijoueur*
