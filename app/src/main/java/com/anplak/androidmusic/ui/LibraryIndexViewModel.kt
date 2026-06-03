package com.anplak.androidmusic.ui

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anplak.androidmusic.data.FolderRule
import com.anplak.androidmusic.data.FolderRuleMode
import com.anplak.androidmusic.data.LibraryIndexPolicy
import com.anplak.androidmusic.data.LibraryIndexPolicyRepository
import com.anplak.androidmusic.data.SharedPreferencesLibraryIndexPreferences
import com.anplak.androidmusic.data.db.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryIndexUiState(
    val maxDurationMinutes: Int = (LibraryIndexPolicy.DEFAULT_MAX_INDEX_DURATION_MS / 60_000).toInt(),
    val folderRules: List<FolderRule> = emptyList(),
    val presetFolders: List<String> = emptyList(),
    val rulesChanged: Boolean = false
)

class LibraryIndexViewModel @JvmOverloads constructor(
    application: Application,
    private val policyRepository: LibraryIndexPolicyRepository = LibraryIndexPolicyRepository(
        SharedPreferencesLibraryIndexPreferences(application),
        AppDatabase.getInstance(application).indexFolderRuleDao()
    )
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LibraryIndexUiState())
    val uiState: StateFlow<LibraryIndexUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            loadState()
        }
    }

    fun addFolderRule(path: String, mode: FolderRuleMode) {
        viewModelScope.launch {
            policyRepository.addFolderRule(path, mode)
            refreshRules(markChanged = true)
        }
    }

    fun removeFolderRule(path: String) {
        viewModelScope.launch {
            policyRepository.removeFolderRule(path)
            refreshRules(markChanged = true)
        }
    }

    fun consumeRulesChanged(): Boolean {
        val changed = _uiState.value.rulesChanged
        if (changed) {
            _uiState.update { it.copy(rulesChanged = false) }
        }
        return changed
    }

    private suspend fun loadState() {
        val rules = policyRepository.getFolderRules()
        _uiState.value = LibraryIndexUiState(
            maxDurationMinutes = (policyRepository.getMaxDurationMs() / 60_000).toInt(),
            folderRules = rules,
            presetFolders = loadPresetFolders()
        )
    }

    private suspend fun refreshRules(markChanged: Boolean) {
        val rules = policyRepository.getFolderRules()
        _uiState.update {
            it.copy(
                folderRules = rules,
                rulesChanged = it.rulesChanged || markChanged
            )
        }
    }

    private fun loadPresetFolders(): List<String> {
        return listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        )
            .filter { it.exists() && it.isDirectory }
            .map { it.absolutePath }
    }
}
