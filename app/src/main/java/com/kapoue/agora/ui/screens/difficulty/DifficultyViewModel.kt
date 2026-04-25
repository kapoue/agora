package com.kapoue.agora.ui.screens.difficulty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kapoue.agora.data.repository.QuestionRepository
import com.kapoue.agora.domain.model.Difficulty
import com.kapoue.agora.domain.model.Progress
import com.kapoue.agora.domain.model.Theme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DifficultyViewModel @Inject constructor(
    private val questionRepository: QuestionRepository
) : ViewModel() {

    private val _progressMap = MutableStateFlow<Map<Difficulty, Progress?>>(emptyMap())
    val progressMap: StateFlow<Map<Difficulty, Progress?>> = _progressMap

    fun loadProgress(theme: Theme) {
        viewModelScope.launch {
            val map = Difficulty.entries.associateWith { difficulty ->
                questionRepository.getProgress(theme, difficulty)
            }
            _progressMap.value = map
        }
    }
}
