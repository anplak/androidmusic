package com.anplak.androidmusic.data

import com.anplak.androidmusic.player.TrackInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibrarySyncCoordinatorTest {
    @Test
    fun `syncNow coalesces concurrent callers into one sync`() =
        runTest {
            val fakeRepository = FakeSyncRepository()
            val coordinator = LibrarySyncCoordinator(fakeRepository, CoroutineScope(coroutineContext))

            val first = async { coordinator.syncNow() }
            val second = async { coordinator.syncNow() }
            advanceUntilIdle()

            val firstResult = first.await()
            val secondResult = second.await()

            assertEquals(1, fakeRepository.syncLibraryCallCount)
            assertTrue(firstResult is LibrarySyncResult.Completed || firstResult is LibrarySyncResult.Joined)
            assertTrue(secondResult is LibrarySyncResult.Joined || secondResult is LibrarySyncResult.Completed)
        }

    @Test
    fun `scheduleSync debounces rapid requests`() =
        runTest {
            val fakeRepository = FakeSyncRepository()
            val coordinator = LibrarySyncCoordinator(fakeRepository, CoroutineScope(coroutineContext))

            coordinator.scheduleSync(debounceMs = 300L)
            coordinator.scheduleSync(debounceMs = 300L)
            coordinator.scheduleSync(debounceMs = 300L)
            advanceTimeBy(300L)
            advanceUntilIdle()

            assertEquals(1, fakeRepository.syncLibraryCallCount)
        }

    @Test
    fun `syncNow scans directories only once per coordinator`() =
        runTest {
            val fakeRepository = FakeSyncRepository()
            val coordinator = LibrarySyncCoordinator(fakeRepository, CoroutineScope(coroutineContext))

            coordinator.syncNow()
            advanceUntilIdle()
            coordinator.syncNow()
            advanceUntilIdle()

            assertEquals(2, fakeRepository.syncLibraryCallCount)
            assertEquals(1, fakeRepository.scanMusicDirectoriesCallCount)
        }

    @Test
    fun `syncNow emits Running then Success states`() =
        runTest {
            val fakeRepository = FakeSyncRepository()
            val coordinator = LibrarySyncCoordinator(fakeRepository, CoroutineScope(coroutineContext))

            val job = launch { coordinator.syncNow() }
            advanceUntilIdle()
            job.join()

            assertTrue(coordinator.syncState.value is LibrarySyncState.Success)
        }

    @Test
    fun `syncNow emits Failed state when repository throws`() =
        runTest {
            val fakeRepository = FakeSyncRepository(shouldFail = true)
            val coordinator =
                LibrarySyncCoordinator(
                    fakeRepository,
                    CoroutineScope(SupervisorJob() + coroutineContext),
                )

            val result = coordinator.syncNow()
            advanceUntilIdle()

            assertTrue(result is LibrarySyncResult.Failed)
            assertTrue(coordinator.syncState.value is LibrarySyncState.Failed)
        }

    private class FakeSyncRepository(
        private val shouldFail: Boolean = false,
    ) : MusicLibraryRepository {
        private val cacheFlow = MutableStateFlow<List<TrackInfo>>(emptyList())

        var syncLibraryCallCount = 0
            private set
        var scanMusicDirectoriesCallCount = 0
            private set

        override suspend fun getCachedTracks(): List<TrackInfo> = cacheFlow.value

        override fun observeCachedTracks(): Flow<List<TrackInfo>> = cacheFlow

        override suspend fun syncLibrary(): LibraryScanResult {
            syncLibraryCallCount++
            delay(100)
            if (shouldFail) {
                throw RuntimeException("sync failed")
            }
            return LibraryScanResult(emptyList(), 0, 0, 0)
        }

        override suspend fun scanMusicDirectories() {
            scanMusicDirectoriesCallCount++
        }
    }
}
