package com.kapoue.agora.ui.screens.multiplayer.participant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kapoue.agora.data.local.datastore.UserPreferencesDataStore
import com.kapoue.agora.multiplayer.MultiplayerSessionManager
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ParticipantSetupUiState(
    val name: String = "",
    val isReady: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ParticipantSetupViewModel @Inject constructor(
    private val sessionManager: MultiplayerSessionManager,
    private val dataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParticipantSetupUiState())
    val uiState: StateFlow<ParticipantSetupUiState> = _uiState

    companion object {
        val KEY_PLAYER_NAME = stringPreferencesKey("player_name")
    }

    init {
        viewModelScope.launch {
            val prefs = dataStore.dataStore.data.first()
            val savedName = prefs[KEY_PLAYER_NAME] ?: ""
            _uiState.value = _uiState.value.copy(name = savedName)
        }
    }

    fun onNameChange(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun onConfirm() {
        val name = _uiState.value.name.trim()
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Saisis ton prénom.")
            return
        }
        viewModelScope.launch {
            dataStore.dataStore.edit { it[KEY_PLAYER_NAME] = name }
            sessionManager.organizerName = name  // used as player name for participant
            _uiState.value = _uiState.value.copy(isReady = true, error = null)
        }
    }
}
