package com.kapoue.agora.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kapoue.agora.data.repository.ImageRepository
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
    private val imageRepository: ImageRepository
) : ViewModel() {

    private val _themeImages = MutableStateFlow<Map<Theme, String?>>(emptyMap())
    val themeImages: StateFlow<Map<Theme, String?>> = _themeImages

    init {
        loadThemeImages()
    }

    private fun loadThemeImages() {
        viewModelScope.launch {
            val imageMap = Theme.entries.map { theme ->
                async {
                    val url = try {
                        imageRepository.getImageUrl(theme.unsplashQuery)
                    } catch (e: Exception) {
                        null
                    }
                    theme to url
                }
            }.awaitAll().toMap()
            _themeImages.value = imageMap
        }
    }
}
