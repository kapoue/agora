package com.kapoue.agora.ui.screens.home

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kapoue.agora.data.local.AssetQuestionLoader
import com.kapoue.agora.data.repository.QuestionRepository
import com.kapoue.agora.domain.model.Theme
import com.kapoue.agora.ui.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val questionRepository: QuestionRepository,
    private val assetQuestionLoader: AssetQuestionLoader,
    private val appLogger: AppLogger
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

    fun exportLogs(): Uri? {
        return try {
            val content = appLogger.export()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(context.cacheDir, "agora_logs_$timestamp.txt")
            file.writeText(content)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            null
        }
    }
}
