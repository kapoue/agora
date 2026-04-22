package com.kapoue.agora.ui.screens.difficulty

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import coil3.request.ImageRequest
import com.kapoue.agora.data.repository.ImageRepository
import com.kapoue.agora.data.repository.SharedImagePool
import com.kapoue.agora.data.repository.QuestionRepository
import com.kapoue.agora.domain.model.Difficulty
import com.kapoue.agora.domain.model.Progress
import com.kapoue.agora.domain.model.Theme
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DifficultyViewModel @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val imageRepository: ImageRepository,
    private val sharedImagePool: SharedImagePool,
    private val imageLoader: ImageLoader,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _progressMap = MutableStateFlow<Map<Difficulty, Progress?>>(emptyMap())
    val progressMap: StateFlow<Map<Difficulty, Progress?>> = _progressMap

    fun loadProgress(theme: Theme) {
        viewModelScope.launch {
            val map = Difficulty.entries.associateWith { difficulty ->
                questionRepository.getProgress(theme, difficulty)
            }
            _progressMap.value = map

            // Fetch 30 URLs pour le thème et précharger les 3 premières
            val urls = try {
                imageRepository.getImageUrls(theme.unsplashQuery, 30)
            } catch (e: Exception) {
                emptyList()
            }
            if (urls.isNotEmpty()) {
                sharedImagePool.setUrls(urls)
                urls.take(3).forEach { url ->
                    imageLoader.enqueue(ImageRequest.Builder(context).data(url).build())
                }
            }
        }
    }
}
