package com.kapoue.agora.ui.util

import com.kapoue.agora.domain.model.MultiplayerQuestion
import kotlin.random.Random

object MultiplayerShuffler {

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
}
