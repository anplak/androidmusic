package com.anplak.androidmusic.ui

import android.app.Application
import android.os.Build
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anplak.androidmusic.data.ArtistRule
import com.anplak.androidmusic.data.FolderRule
import com.anplak.androidmusic.data.FolderRuleMode
import com.anplak.androidmusic.data.LibraryIndexPolicy
import com.anplak.androidmusic.data.LibraryIndexPolicyRepository
import com.anplak.androidmusic.data.LibraryIndexSuggestions
import com.anplak.androidmusic.data.SharedPreferencesLibraryIndexPreferences
import com.anplak.androidmusic.data.db.AppDatabase
import com.anplak.androidmusic.data.db.TrackDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class LibraryIndexUiState(
    val maxDurationMinutes: Int = (LibraryIndexPolicy.DEFAULT_MAX_INDEX_DURATION_MS / 60_000).toInt(),
    val includeFolderRules: List<FolderRule> = emptyList(),
    val excludedFolders: List<FolderRule> = emptyList(),
    val excludedArtists: List<ArtistRule> = emptyList(),
    val knownArtists: List<String> = emptyList(),
    val knownFolders: List<String> = emptyList(),
    val presetFolders: List<String> = emptyList(),
    val rulesChanged: Boolean = false
)

class LibraryIndexViewModel @JvmOverloads constructor(
    application: Application,
    private val policyRepository: LibraryIndexPolicyRepository = LibraryIndexPolicyRepository(
        SharedPreferencesLibraryIndexPreferences(application),
        AppDatabase.getInstance(application).indexFolderRuleDao(),
        AppDatabase.getInstance(application).indexArtistRuleDao()
    ),
    private val trackDao: TrackDao = AppDatabase.getInstance(application).trackDao()
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

    fun addArtistRule(name: String) {
        viewModelScope.launch {
            policyRepository.addArtistRule(name)
            refreshRules(markChanged = true)
        }
    }

    fun removeArtistRule(name: String) {
        viewModelScope.launch {
            policyRepository.removeArtistRule(name)
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
        refreshRules(markChanged = false)
        _uiState.update {
            it.copy(
                maxDurationMinutes = (policyRepository.getMaxDurationMs() / 60_000).toInt(),
                presetFolders = loadPresetRoots()
            )
        }
    }

    private suspend fun refreshRules(markChanged: Boolean) {
        val folderRules = policyRepository.getFolderRules()
        val existingPaths = folderRules.map { it.path }.toSet()
        val presetRoots = loadPresetRoots()
        val knownFolders = LibraryIndexSuggestions.mergeFolderSuggestions(
            fromTracks = LibraryIndexSuggestions.discoverFoldersFromTracks(
                trackPaths = trackDao.getTrackPaths(),
                existingRulePaths = existingPaths
            ),
            subfolders = LibraryIndexSuggestions.discoverSubfolders(
                presetRoots.map { File(it) }
            ),
            presetRoots = presetRoots,
            existingRulePaths = existingPaths
        )
        val knownArtists = buildKnownArtists(trackDao.getDistinctArtists())

        _uiState.update {
            it.copy(
                includeFolderRules = folderRules.filter { rule -> rule.mode == FolderRuleMode.INCLUDE },
                excludedFolders = folderRules.filter { rule -> rule.mode == FolderRuleMode.EXCLUDE },
                excludedArtists = policyRepository.getArtistRules(),
                knownArtists = knownArtists,
                knownFolders = knownFolders,
                rulesChanged = it.rulesChanged || markChanged
            )
        }
    }

    private fun buildKnownArtists(indexedArtists: List<String>): List<String> {
        val unknown = LibraryIndexSuggestions.UNKNOWN_ARTIST_LABEL
        return listOf(unknown) + indexedArtists.filter { !it.equals(unknown, ignoreCase = true) }
    }

private fun loadPresetRoots(): List<String> {
        val roots = mutableListOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // DIRECTORY_AUDIOBOOKS requires API 29
            roots += Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_AUDIOBOOKS)
        }
        return roots
            .filter { it.exists() && it.isDirectory }
            .map { it.absolutePath }
    }
}
