package com.anplak.androidmusic.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface LibrarySyncState {
    data object Idle : LibrarySyncState
    data object Running : LibrarySyncState
    data class Success(val result: LibraryScanResult) : LibrarySyncState
    data class Failed(val error: Throwable) : LibrarySyncState
}

sealed interface LibrarySyncResult {
    data class Completed(val result: LibraryScanResult) : LibrarySyncResult
    data class Joined(val result: LibraryScanResult) : LibrarySyncResult
    data class Failed(val error: Throwable) : LibrarySyncResult
}

class LibrarySyncCoordinator(
    private val repository: MusicLibraryRepository,
    private val scope: CoroutineScope
) {
    private val syncMutex = Mutex()
    private var inFlight: Deferred<Result<LibraryScanResult>>? = null
    private var hasScannedDirectories = false
    private var debounceJob: Job? = null

    private val _syncState = MutableStateFlow<LibrarySyncState>(LibrarySyncState.Idle)
    val syncState: StateFlow<LibrarySyncState> = _syncState.asStateFlow()

    fun scheduleSync(debounceMs: Long = 300L) {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(debounceMs)
            syncNow()
        }
    }

    suspend fun syncNow(): LibrarySyncResult {
        val (deferred, joined) = syncMutex.withLock {
            inFlight?.takeIf { it.isActive }?.let { active ->
                return@withLock Pair(active, true)
            }
            _syncState.value = LibrarySyncState.Running
            val newDeferred = scope.async {
                runCatching {
                    if (!hasScannedDirectories) {
                        repository.scanMusicDirectories()
                        hasScannedDirectories = true
                    }
                    repository.syncLibrary()
                }
            }
            inFlight = newDeferred
            Pair(newDeferred, false)
        }

        val outcome = deferred.await()
        return try {
            outcome.fold(
                onSuccess = { result ->
                    _syncState.value = LibrarySyncState.Success(result)
                    if (joined) {
                        LibrarySyncResult.Joined(result)
                    } else {
                        LibrarySyncResult.Completed(result)
                    }
                },
                onFailure = { error ->
                    _syncState.value = LibrarySyncState.Failed(error)
                    LibrarySyncResult.Failed(error)
                }
            )
        } finally {
            syncMutex.withLock {
                if (inFlight === deferred && deferred.isCompleted) {
                    inFlight = null
                }
            }
        }
    }
}

object LibrarySyncCoordinatorFactory {
    @Volatile
    private var instance: LibrarySyncCoordinator? = null

    fun get(context: Context): LibrarySyncCoordinator =
        instance ?: synchronized(this) {
            instance ?: LibrarySyncCoordinator(
                repository = MusicLibraryRepositoryFactory.create(context),
                scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            ).also { instance = it }
        }

    internal fun resetForTests() {
        instance = null
    }
}
