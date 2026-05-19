package com.kapoue.agora.multiplayer

import com.kapoue.agora.data.local.db.dao.MultiplayerSessionDao
import com.kapoue.agora.data.local.db.dao.QuestionDao
import com.kapoue.agora.data.local.db.entity.MultiplayerSessionEntity
import com.kapoue.agora.domain.model.MultiplayerQuestion
import com.kapoue.agora.domain.model.PlayerResult
import com.kapoue.agora.domain.model.QrPayload
import com.kapoue.agora.ui.util.QrPayloadEncoder
import dagger.hilt.android.scopes.ActivityRetainedScoped
import java.util.UUID
import javax.inject.Inject

/**
 * Singleton par activité qui maintient l'état de la session multijoueur en mémoire
 * et le persiste en Room.
 */
@ActivityRetainedScoped
class MultiplayerSessionManager @Inject constructor(
    private val sessionDao: MultiplayerSessionDao,
    private val questionDao: QuestionDao
) {
    // État en mémoire
    var sessionId: String = ""
    var organizerName: String = ""
    var totalRounds: Int = 1
    var questionsPerRound: Int = 10
    var currentRound: Int = 1

    // Questions de la session courante (indexées par numéro de manche)
    private val roundQuestions: MutableMap<Int, List<MultiplayerQuestion>> = mutableMapOf()

    // Résultats cumulés
    private val allResults: MutableList<PlayerResult> = mutableListOf()
    private val eliminatedPlayers: MutableSet<String> = mutableSetOf()

    // Résultats de la manche courante (scannés par l'organisateur)
    val currentRoundResults: MutableList<PlayerResult> = mutableListOf()

    fun getQuestionsForRound(round: Int): List<MultiplayerQuestion> =
        roundQuestions[round] ?: emptyList()

    fun setQuestionsForRound(round: Int, questions: List<MultiplayerQuestion>) {
        roundQuestions[round] = questions
    }

    fun getCurrentRoundPayload(): QrPayload = QrPayload(
        sessionId = sessionId,
        roundNumber = currentRound,
        totalRounds = totalRounds,
        questionsPerRound = questionsPerRound,
        questions = getQuestionsForRound(currentRound)
    )

    fun addRoundResult(result: PlayerResult) {
        currentRoundResults.removeAll { it.playerName == result.playerName && it.roundNumber == result.roundNumber }
        currentRoundResults.add(result)
    }

    fun addRoundResult(
        playerName: String,
        score: Int,
        totalQuestions: Int,
        timeMillis: Long,
        wrongAnswers: List<WrongAnswer>
    ) {
        addRoundResult(
            PlayerResult(
                sessionId = sessionId,
                roundNumber = currentRound,
                playerName = playerName,
                score = score,
                totalQuestions = totalQuestions,
                timeMillis = timeMillis,
                wrongAnswers = wrongAnswers
            )
        )
    }

    fun eliminateAbsentPlayers() {
        // Tous les joueurs ayant participé aux manches précédentes mais pas à la manche courante
        val previousPlayers = allResults.map { it.playerName }.toSet()
        val currentPlayers = currentRoundResults.map { it.playerName }.toSet()
        val absent = previousPlayers - currentPlayers
        eliminatedPlayers.addAll(absent)
    }

    fun isEliminated(playerName: String): Boolean = playerName in eliminatedPlayers

    fun commitRoundResults() {
        allResults.addAll(currentRoundResults)
        currentRoundResults.clear()
        if (currentRound < totalRounds) currentRound++
    }

    /** Classement cumulé : score desc, temps asc. Éliminés en dernier. */
    fun getCumulativeRanking(): List<RankedPlayer> {
        val byPlayer = allResults.groupBy { it.playerName }
        val ranked = byPlayer.map { (name, results) ->
            val eliminated = name in eliminatedPlayers
            RankedPlayer(
                name = name,
                totalScore = results.sumOf { it.score },
                totalTimeMillis = results.sumOf { it.timeMillis },
                roundsPlayed = results.size,
                isEliminated = eliminated
            )
        }
        return ranked.sortedWith(
            compareBy<RankedPlayer> { it.isEliminated }
                .thenByDescending { it.totalScore }
                .thenBy { it.totalTimeMillis }
        )
    }

    suspend fun persistSession() {
        val entity = MultiplayerSessionEntity(
            sessionId = sessionId,
            organizerName = organizerName,
            totalRounds = totalRounds,
            questionsPerRound = questionsPerRound,
            currentRound = currentRound,
            status = "IN_PROGRESS",
            resultsJson = QrPayloadEncoder.encodeResultList(allResults),
            eliminatedJson = QrPayloadEncoder.encodeStringList(eliminatedPlayers.toList()),
            createdAt = System.currentTimeMillis()
        )
        sessionDao.insertSession(entity)
    }

    suspend fun updatePersistedSession() {
        val entity = sessionDao.getSession(sessionId) ?: return
        sessionDao.updateSession(
            entity.copy(
                currentRound = currentRound,
                resultsJson = QrPayloadEncoder.encodeResultList(allResults),
                eliminatedJson = QrPayloadEncoder.encodeStringList(eliminatedPlayers.toList())
            )
        )
    }

    suspend fun completeSession() {
        sessionDao.completeSession(sessionId)
        sessionDao.deleteCompletedSessions()
    }

    fun reset() {
        sessionId = UUID.randomUUID().toString()
        organizerName = ""
        totalRounds = 1
        questionsPerRound = 10
        currentRound = 1
        roundQuestions.clear()
        allResults.clear()
        eliminatedPlayers.clear()
        currentRoundResults.clear()
    }
}

data class RankedPlayer(
    val name: String,
    val totalScore: Int,
    val totalTimeMillis: Long,
    val roundsPlayed: Int,
    val isEliminated: Boolean
)
