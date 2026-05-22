package com.anplak.androidmusic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anplak.androidmusic.data.FavoritesRepositoryImpl
import com.anplak.androidmusic.data.MusicLibraryRepositoryImpl
import com.anplak.androidmusic.data.PlayHistoryRepositoryImpl
import com.anplak.androidmusic.data.PlaylistRepositoryImpl
import com.anplak.androidmusic.data.RecommendationEngine
import com.anplak.androidmusic.data.RecommendationRepository
import com.anplak.androidmusic.data.RecommendationRepositoryImpl
import com.anplak.androidmusic.data.RecommendationRow
import com.anplak.androidmusic.data.TrackStatsRepositoryImpl
import com.anplak.androidmusic.data.db.AppDatabase
import com.anplak.androidmusic.player.AutoMixGenerator
import com.anplak.androidmusic.player.SmartShuffleGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ForYouUiState {
    data object Loading : ForYouUiState
    data class Content(val rows: List<RecommendationRow>) : ForYouUiState
    data object Empty : ForYouUiState
    data class Error(val message: String) : ForYouUiState
}

class DiscoveryViewModel @JvmOverloads constructor(
    application: Application,
    private val recommendationRepository: RecommendationRepository? = null,
    private val recommendationEngine: RecommendationEngine? = null
) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository: RecommendationRepository = recommendationRepository ?: run {
        val favoritesRepository = FavoritesRepositoryImpl(db.favoriteDao())
        RecommendationRepositoryImpl(
            musicLibraryRepository = MusicLibraryRepositoryImpl(
                application.contentResolver,
                application,
                db.trackDao()
            ),
            favoritesRepository = favoritesRepository,
            playHistoryRepository = PlayHistoryRepositoryImpl(db.playHistoryDao()),
            playlistRepository = PlaylistRepositoryImpl(db.playlistDao())
        )
    }
    private val engine: RecommendationEngine = recommendationEngine ?: RecommendationEngine(
        AutoMixGenerator(SmartShuffleGenerator(
            FavoritesRepositoryImpl(db.favoriteDao()),
            TrackStatsRepositoryImpl(db.trackStatsDao())
        ))
    )

    private val _uiState = MutableStateFlow<ForYouUiState>(ForYouUiState.Loading)
    val uiState: StateFlow<ForYouUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var cachedRows: List<RecommendationRow> = emptyList()

    init {
        refresh()
    }

    fun getRow(rowId: String): RecommendationRow? = cachedRows.find { it.id == rowId }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _uiState.value = ForYouUiState.Loading
            runCatching {
                val inputs = repository.loadInputs()
                val rows = engine.buildRows(inputs)
                cachedRows = rows
                _uiState.value = when {
                    rows.isEmpty() -> ForYouUiState.Empty
                    else -> ForYouUiState.Content(rows)
                }
            }.onFailure { error ->
                _uiState.value = ForYouUiState.Error(
                    error.message ?: "Failed to load recommendations"
                )
            }
        }
    }
}
