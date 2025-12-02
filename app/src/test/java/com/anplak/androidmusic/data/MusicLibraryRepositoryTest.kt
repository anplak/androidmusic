package com.anplak.androidmusic.data

import android.content.ContentResolver
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MusicLibraryRepositoryTest {
    
    private lateinit var contentResolver: ContentResolver
    private lateinit var repository: MusicLibraryRepositoryImpl
    
    private val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DURATION
    )
    
    @Before
    fun setup() {
        contentResolver = mock()
        repository = MusicLibraryRepositoryImpl(contentResolver)
    }
    
    @Test
    fun `getAllTracks returns empty list when MediaStore has no audio`() = runTest {
        val emptyCursor = createEmptyCursor()
        whenever(contentResolver.query(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(emptyCursor)
        
        val tracks = repository.getAllTracks()
        
        assertTrue(tracks.isEmpty())
    }
    
    @Test
    fun `getAllTracks returns correct track data when audio files exist`() = runTest {
        val cursor = createCursorWithTracks(
            listOf(
                TrackData(1L, "Song One", "Artist A", "Album X", 180000L),
                TrackData(2L, "Song Two", "Artist B", "Album Y", 240000L)
            )
        )
        whenever(contentResolver.query(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(cursor)
        
        val tracks = repository.getAllTracks()
        
        assertEquals(2, tracks.size)
        assertEquals("Song One", tracks[0].title)
        assertEquals("Artist A", tracks[0].artist)
        assertEquals("Album X", tracks[0].album)
        assertEquals(180000L, tracks[0].duration)
        assertEquals("Song Two", tracks[1].title)
        assertEquals("Artist B", tracks[1].artist)
    }
    
    @Test
    fun `getAllTracks handles MediaStore query exceptions gracefully`() = runTest {
        whenever(contentResolver.query(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenThrow(RuntimeException("Database error"))
        
        val tracks = repository.getAllTracks()
        
        assertTrue(tracks.isEmpty())
    }
    
    @Test
    fun `getAllTracks handles null cursor gracefully`() = runTest {
        whenever(contentResolver.query(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(null)
        
        val tracks = repository.getAllTracks()
        
        assertTrue(tracks.isEmpty())
    }
    
    // ============================================================
    // Bug Reproduction Tests - These expose the IS_MUSIC filter issue
    // ============================================================
    
    @Test
    fun `getAllTracks should not filter by IS_MUSIC to discover all audio files`() = runTest {
        // This test verifies that the query does NOT use IS_MUSIC filter
        // which would exclude valid audio files without proper metadata
        val cursor = createEmptyCursor()
        whenever(contentResolver.query(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(cursor)
        
        repository.getAllTracks()
        
        // Capture the selection argument
        val selectionCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(contentResolver).query(
            any<Uri>(),
            any(),
            selectionCaptor.capture(),
            anyOrNull(),
            anyOrNull()
        )
        
        val selection = selectionCaptor.value
        // The selection should be null (no filter) to discover ALL audio files
        // This test will FAIL with current implementation that uses IS_MUSIC != 0
        assertNull(
            "Query should not filter by IS_MUSIC to ensure all audio files are discovered. " +
            "Current selection: $selection",
            selection
        )
    }
    
    @Test
    fun `getAllTracks discovers files in Music folder regardless of IS_MUSIC flag`() = runTest {
        // Files in Music/ folder should be discovered even if IS_MUSIC=0
        // This test documents expected behavior - currently broken due to IS_MUSIC filter
        val cursor = createEmptyCursor()
        whenever(contentResolver.query(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(cursor)
        
        repository.getAllTracks()
        
        // Capture the selection argument
        val selectionCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(contentResolver).query(
            any<Uri>(),
            any(),
            selectionCaptor.capture(),
            anyOrNull(),
            anyOrNull()
        )
        
        val selection = selectionCaptor.value
        // Selection must not exclude files based on IS_MUSIC flag alone
        val excludesNonMusicFiles = selection?.contains("IS_MUSIC") == true && 
                                     selection.contains("OR").not()
        assertFalse(
            "Files in Music folder with IS_MUSIC=0 would be excluded. Selection: $selection",
            excludesNonMusicFiles
        )
    }
    
    @Test
    fun `getAllTracks discovers files in Download folder regardless of IS_MUSIC flag`() = runTest {
        // Files in Download/ folder should be discovered even if IS_MUSIC=0
        // This test documents expected behavior - currently broken due to IS_MUSIC filter
        val cursor = createEmptyCursor()
        whenever(contentResolver.query(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(cursor)
        
        repository.getAllTracks()
        
        // Capture the selection argument
        val selectionCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(contentResolver).query(
            any<Uri>(),
            any(),
            selectionCaptor.capture(),
            anyOrNull(),
            anyOrNull()
        )
        
        val selection = selectionCaptor.value
        // Selection must not exclude files based on IS_MUSIC flag alone
        val excludesNonMusicFiles = selection?.contains("IS_MUSIC") == true && 
                                     selection.contains("OR").not()
        assertFalse(
            "Files in Download folder with IS_MUSIC=0 would be excluded. Selection: $selection",
            excludesNonMusicFiles
        )
    }
    
    // ============================================================
    // Regression Tests - Ensure file discovery works for all scenarios
    // ============================================================
    
    @Test
    fun `getAllTracks returns tracks from Music folder`() = runTest {
        // Simulates files from Music/ folder being returned
        val cursor = createCursorWithTracks(
            listOf(
                TrackData(1L, "Music Folder Song", "Artist", "Album", 180000L),
                TrackData(2L, "Another Music Song", "Artist", "Album", 200000L)
            )
        )
        whenever(contentResolver.query(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(cursor)
        
        val tracks = repository.getAllTracks()
        
        assertEquals(2, tracks.size)
        assertEquals("Music Folder Song", tracks[0].title)
        assertEquals("Another Music Song", tracks[1].title)
    }
    
    @Test
    fun `getAllTracks returns tracks from Download folder`() = runTest {
        // Simulates files from Download/ folder being returned
        val cursor = createCursorWithTracks(
            listOf(
                TrackData(100L, "Downloaded Song", "Artist", "Album", 240000L),
                TrackData(101L, "Another Download", "Artist", "Album", 300000L)
            )
        )
        whenever(contentResolver.query(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(cursor)
        
        val tracks = repository.getAllTracks()
        
        assertEquals(2, tracks.size)
        assertEquals("Downloaded Song", tracks[0].title)
        assertEquals("Another Download", tracks[1].title)
    }
    
    @Test
    fun `getAllTracks handles files with missing metadata`() = runTest {
        // Files without proper ID3 tags should still be discovered
        val cursor = createCursorWithTracks(
            listOf(
                TrackData(1L, "", "", "", 120000L),  // All empty
                TrackData(2L, null, null, null, 180000L)  // All null
            )
        )
        whenever(contentResolver.query(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(cursor)
        
        val tracks = repository.getAllTracks()
        
        assertEquals(2, tracks.size)
        // Should use default values for missing metadata
        assertEquals("Unknown Title", tracks[0].title)
        assertEquals("Unknown Artist", tracks[0].artist)
        assertEquals("Unknown Album", tracks[0].album)
        assertEquals("Unknown Title", tracks[1].title)
        assertEquals("Unknown Artist", tracks[1].artist)
        assertEquals("Unknown Album", tracks[1].album)
    }
    
    @Test
    fun `getAllTracks includes short duration files`() = runTest {
        // Short files (< 10 seconds) often get IS_MUSIC=0, but should still be discovered
        val cursor = createCursorWithTracks(
            listOf(
                TrackData(1L, "Short Clip", "Artist", "Album", 5000L),   // 5 seconds
                TrackData(2L, "Ringtone", "Artist", "Album", 3000L),    // 3 seconds
                TrackData(3L, "Sound Effect", "Artist", "Album", 1000L) // 1 second
            )
        )
        whenever(contentResolver.query(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(cursor)
        
        val tracks = repository.getAllTracks()
        
        assertEquals(3, tracks.size)
        assertEquals(5000L, tracks[0].duration)
        assertEquals(3000L, tracks[1].duration)
        assertEquals(1000L, tracks[2].duration)
    }
    
    @Test
    fun `getAllTracks handles various audio formats`() = runTest {
        // Different audio formats (mp3, m4a, flac, ogg) should all be discovered
        val cursor = createCursorWithTracks(
            listOf(
                TrackData(1L, "MP3 Song", "Artist", "Album", 180000L),
                TrackData(2L, "M4A Song", "Artist", "Album", 200000L),
                TrackData(3L, "FLAC Song", "Artist", "Album", 220000L),
                TrackData(4L, "OGG Song", "Artist", "Album", 240000L),
                TrackData(5L, "WAV Song", "Artist", "Album", 260000L)
            )
        )
        whenever(contentResolver.query(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(cursor)
        
        val tracks = repository.getAllTracks()
        
        assertEquals(5, tracks.size)
    }
    
    @Test
    fun `getAllTracks queries external content URI`() = runTest {
        // Ensure we query the correct MediaStore URI for external storage
        val cursor = createEmptyCursor()
        whenever(contentResolver.query(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(cursor)
        
        repository.getAllTracks()
        
        val uriCaptor = ArgumentCaptor.forClass(Uri::class.java)
        verify(contentResolver).query(
            uriCaptor.capture(),
            any(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull()
        )
        
        assertEquals(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            uriCaptor.value
        )
    }
    
    @Test
    fun `getAllTracks handles special characters in metadata`() = runTest {
        // Files with special characters in metadata should be handled correctly
        val cursor = createCursorWithTracks(
            listOf(
                TrackData(1L, "Song with 'Quotes'", "Artist & Friends", "Album (Deluxe)", 180000L),
                TrackData(2L, "日本語タイトル", "アーティスト", "アルバム", 200000L),
                TrackData(3L, "Emoji 🎵 Song", "Artist 🎤", "Album 💿", 220000L)
            )
        )
        whenever(contentResolver.query(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(cursor)
        
        val tracks = repository.getAllTracks()
        
        assertEquals(3, tracks.size)
        assertEquals("Song with 'Quotes'", tracks[0].title)
        assertEquals("Artist & Friends", tracks[0].artist)
        assertEquals("日本語タイトル", tracks[1].title)
        assertEquals("Emoji 🎵 Song", tracks[2].title)
    }
    
    @Test
    fun `getAllTracks handles files with unknown marker in metadata`() = runTest {
        // MediaStore sometimes returns "<unknown>" for missing metadata
        val cursor = createCursorWithTracks(
            listOf(
                TrackData(1L, "<unknown>", "<unknown>", "<unknown>", 180000L)
            )
        )
        whenever(contentResolver.query(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(cursor)
        
        val tracks = repository.getAllTracks()
        
        assertEquals(1, tracks.size)
        // Should convert "<unknown>" to default values
        assertEquals("Unknown Title", tracks[0].title)
        assertEquals("Unknown Artist", tracks[0].artist)
        assertEquals("Unknown Album", tracks[0].album)
    }
    
    private fun createEmptyCursor(): Cursor {
        return MatrixCursor(projection)
    }
    
    private fun createCursorWithTracks(tracks: List<TrackData>): Cursor {
        val cursor = MatrixCursor(projection)
        tracks.forEach { track ->
            cursor.addRow(arrayOf(
                track.id,
                track.title,
                track.artist,
                track.album,
                track.duration
            ))
        }
        return cursor
    }
    
    private data class TrackData(
        val id: Long,
        val title: String?,
        val artist: String?,
        val album: String?,
        val duration: Long
    )
}
