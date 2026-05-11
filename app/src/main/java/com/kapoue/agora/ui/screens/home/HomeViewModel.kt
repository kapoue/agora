package com.kapoue.agora.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kapoue.agora.data.local.AssetQuestionLoader
import com.kapoue.agora.data.repository.QuestionRepository
import com.kapoue.agora.domain.model.Theme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val assetQuestionLoader: AssetQuestionLoader
) : ViewModel() {

    private val _themeImages = MutableStateFlow<Map<Theme, String?>>(emptyMap())
    val themeImages: StateFlow<Map<Theme, String?>> = _themeImages

    private val _seriesCounts = MutableStateFlow<Map<Theme, Int>>(emptyMap())
    val seriesCounts: StateFlow<Map<Theme, Int>> = _seriesCounts

    init {
        loadThemeData()
    }

    private fun loadThemeData() {
        viewModelScope.launch {
            val imageMap = Theme.entries.map { theme ->
                async {
                    val url = if (theme == Theme.CULTURE_GENERALE) {
                        questionRepository.getRandomImageUrl()
                    } else {
                        assetQuestionLoader.getFirstImageUrl(theme)
                    }
                    theme to url
                }
            }.awaitAll().toMap()
            _themeImages.value = imageMap

            val seriesMap = Theme.entries
                .filter { it != Theme.CULTURE_GENERALE }
                .map { theme ->
                async { theme to questionRepository.getSeriesCount(theme) }
            }.awaitAll().toMap()
            _seriesCounts.value = seriesMap
        }
    }

    fun refresh() {
        loadThemeData()
    }
}
