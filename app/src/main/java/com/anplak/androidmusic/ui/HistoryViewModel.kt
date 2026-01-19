package com.anplak.androidmusic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anplak.androidmusic.data.PlayHistoryEntry
import com.anplak.androidmusic.data.PlayHistoryRepository
import com.anplak.androidmusic.data.PlayHistoryRepositoryImpl
import com.anplak.androidmusic.data.db.AppDatabase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data class Content(
        val entries: List<PlayHistoryEntry>,
        val hasMore: Boolean = false
    ) : HistoryUiState
    data object Empty : HistoryUiState
}

class HistoryViewModel @JvmOverloads constructor(
    application: Application,
    private val playHistoryRepository: PlayHistoryRepository = PlayHistoryRepositoryImpl(
        AppDatabase.getInstance(application).playHistoryDao()
    )
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var currentEntries = mutableListOf<PlayHistoryEntry>()
    private var currentOffset = 0

    init {
        loadHistory()
        // Run cleanup on startup (90 day retention policy)
        cleanupOldHistory()
    }

    /**
     * Loads the initial page of history entries.
     */
    fun loadHistory() {
        loadJob?.cancel()
        currentEntries.clear()
        currentOffset = 0
        
        loadJob = viewModelScope.launch {
            _uiState.value = HistoryUiState.Loading
            
            playHistoryRepository.getHistory(PAGE_SIZE, 0).collect { entries ->
                if (entries.isEmpty()) {
                    _uiState.value = HistoryUiState.Empty
                } else {
                    currentEntries.clear()
                    currentEntries.addAll(entries)
                    currentOffset = entries.size
                    _uiState.value = HistoryUiState.Content(
                        entries = currentEntries.toList(),
                        hasMore = entries.size >= PAGE_SIZE
                    )
                }
            }
        }
    }

    /**
     * Loads more history entries (pagination).
     */
    fun loadMore() {
        val currentState = _uiState.value
        if (currentState !is HistoryUiState.Content || !currentState.hasMore) return

        viewModelScope.launch {
            playHistoryRepository.getHistory(PAGE_SIZE, currentOffset).collect { newEntries ->
                if (newEntries.isNotEmpty()) {
                    currentEntries.addAll(newEntries)
                    currentOffset += newEntries.size
                    _uiState.value = HistoryUiState.Content(
                        entries = currentEntries.toList(),
                        hasMore = newEntries.size >= PAGE_SIZE
                    )
                } else {
                    _uiState.value = currentState.copy(hasMore = false)
                }
            }
        }
    }

    /**
     * Refreshes the history list.
     */
    fun refresh() {
        loadHistory()
    }

    /**
     * Runs cleanup to remove history older than retention period.
     */
    private fun cleanupOldHistory() {
        viewModelScope.launch {
            playHistoryRepository.cleanupOldHistory(RETENTION_DAYS)
        }
    }

    companion object {
        private const val PAGE_SIZE = 50
        private const val RETENTION_DAYS = 90
    }
}
