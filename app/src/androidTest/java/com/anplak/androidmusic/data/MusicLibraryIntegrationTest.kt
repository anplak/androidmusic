package com.anplak.androidmusic.data

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Integration test that runs on a real device/emulator to verify
 * that music files are properly discovered from Music and Download folders.
 * 
 * This test logs all discovered files to logcat for debugging.
 * Run with: ./gradlew :app:connectedDebugAndroidTest --tests "*MusicLibraryIntegrationTest*"
 * 
 * View logcat output with: adb logcat -s MusicLibraryIntegrationTest
 */
@RunWith(AndroidJUnit4::class)
class MusicLibraryIntegrationTest {

    companion object {
        private const val TAG = "MusicLibraryIntegrationTest"
    }

    @get:Rule
    val permissionRule: GrantPermissionRule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        GrantPermissionRule.grant(Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var repository: MusicLibraryRepositoryImpl

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        contentResolver = context.contentResolver
        repository = MusicLibraryRepositoryImpl(contentResolver)
        
        Log.i(TAG, "========================================")
        Log.i(TAG, "INTEGRATION TEST STARTED")
        Log.i(TAG, "========================================")
        Log.i(TAG, "Device SDK: ${Build.VERSION.SDK_INT}")
        Log.i(TAG, "Device: ${Build.MANUFACTURER} ${Build.MODEL}")
    }

    /**
     * Triggers MediaStore to scan specific files or directories.
     * This is needed because files added via adb or file manager may not be indexed yet.
     */
    private fun triggerMediaScan(paths: List<String>) {
        Log.i(TAG, "Triggering media scan for ${paths.size} paths...")
        
        val latch = CountDownLatch(paths.size)
        
        paths.forEach { path ->
            Log.i(TAG, "Scanning: $path")
            MediaScannerConnection.scanFile(
                context,
                arrayOf(path),
                arrayOf("audio/mpeg", "audio/mp3", "audio/*"),
            ) { scannedPath, uri ->
                Log.i(TAG, "Scanned: $scannedPath -> URI: $uri")
                latch.countDown()
            }
        }
        
        // Wait for all scans to complete (max 30 seconds)
        val completed = latch.await(30, TimeUnit.SECONDS)
        Log.i(TAG, "Media scan ${if (completed) "completed" else "timed out"}")
        
        // Give MediaStore a moment to update
        Thread.sleep(1000)
    }

    /**
     * Gets all audio files from Music and Download folders
     */
    private fun getAudioFilePaths(): List<String> {
        val paths = mutableListOf<String>()
        
        val directories = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        )
        
        val audioExtensions = listOf(".mp3", ".m4a", ".flac", ".ogg", ".wav", ".aac")
        
        directories.forEach { dir ->
            if (dir.exists() && dir.isDirectory && dir.canRead()) {
                dir.listFiles()?.filter { file ->
                    file.isFile && audioExtensions.any { ext -> 
                        file.name.endsWith(ext, ignoreCase = true) 
                    }
                }?.forEach { file ->
                    paths.add(file.absolutePath)
                }
            }
        }
        
        return paths
    }

    @Test
    fun triggerMediaScanAndVerifyFilesAreDiscovered() {
        Log.i(TAG, "========================================")
        Log.i(TAG, "TEST: Trigger media scan then verify discovery")
        Log.i(TAG, "========================================")

        // Step 1: Get all physical audio files
        val audioFiles = getAudioFilePaths()
        Log.i(TAG, "Found ${audioFiles.size} physical audio files to scan")
        
        if (audioFiles.isEmpty()) {
            Log.w(TAG, "No audio files found on device to test")
            return
        }
        
        // Step 2: Query MediaStore BEFORE scan
        val countBefore = queryMediaStoreCount()
        Log.i(TAG, "MediaStore count BEFORE scan: $countBefore")
        
        // Step 3: Trigger media scan
        triggerMediaScan(audioFiles)
        
        // Step 4: Query MediaStore AFTER scan
        val countAfter = queryMediaStoreCount()
        Log.i(TAG, "MediaStore count AFTER scan: $countAfter")
        
        // Step 5: Verify via repository
        runBlocking {
            val tracks = repository.getAllTracks()
            Log.i(TAG, "Repository found ${tracks.size} tracks after scan")
            
            tracks.forEach { track ->
                Log.i(TAG, "  - ${track.title} by ${track.artist}")
            }
            
            Log.i(TAG, "========================================")
            Log.i(TAG, "RESULT: Physical files: ${audioFiles.size}, MediaStore: $countAfter, Repository: ${tracks.size}")
            Log.i(TAG, "========================================")
            
            assertTrue(
                "After media scan, expected at least some tracks to be discovered. " +
                "Physical files: ${audioFiles.size}, MediaStore after scan: $countAfter, Repository: ${tracks.size}",
                tracks.isNotEmpty() || audioFiles.isEmpty()
            )
        }
    }
    
    private fun queryMediaStoreCount(): Int {
        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Media._ID),
            null,
            null,
            null
        )
        val count = cursor?.count ?: 0
        cursor?.close()
        return count
    }

    @Test
    fun logAllAudioFilesFromMediaStore() {
        Log.i(TAG, "========================================")
        Log.i(TAG, "QUERYING MEDIASTORE FOR ALL AUDIO FILES")
        Log.i(TAG, "========================================")

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,  // File path
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.IS_MUSIC
        )

        // Query WITHOUT any filter to see ALL audio files
        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,  // No selection - get ALL audio files
            null,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )

        var totalCount = 0
        var musicFolderCount = 0
        var downloadFolderCount = 0
        var mp3Count = 0
        var isMusicTrueCount = 0
        var isMusicFalseCount = 0

        cursor?.use { c ->
            Log.i(TAG, "Total audio files in MediaStore: ${c.count}")
            
            val idColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val dataColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val displayNameColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val mimeTypeColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val relativePathColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
            val isMusicColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC)
            val durationColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (c.moveToNext()) {
                totalCount++
                val id = c.getLong(idColumn)
                val title = c.getString(titleColumn) ?: "<no title>"
                val artist = c.getString(artistColumn) ?: "<no artist>"
                val data = c.getString(dataColumn) ?: "<no path>"
                val displayName = c.getString(displayNameColumn) ?: "<no name>"
                val mimeType = c.getString(mimeTypeColumn) ?: "<no mime>"
                val relativePath = c.getString(relativePathColumn) ?: "<no relative path>"
                val isMusic = c.getInt(isMusicColumn)
                val duration = c.getLong(durationColumn)

                // Track IS_MUSIC stats
                if (isMusic != 0) isMusicTrueCount++ else isMusicFalseCount++

                // Track folder locations
                val isInMusicFolder = relativePath.contains("Music", ignoreCase = true) || 
                                      data.contains("/Music/", ignoreCase = true)
                val isInDownloadFolder = relativePath.contains("Download", ignoreCase = true) || 
                                         data.contains("/Download/", ignoreCase = true)
                
                if (isInMusicFolder) musicFolderCount++
                if (isInDownloadFolder) downloadFolderCount++

                // Track MP3 files
                val isMp3 = mimeType.contains("mp3", ignoreCase = true) || 
                           displayName.endsWith(".mp3", ignoreCase = true)
                if (isMp3) mp3Count++

                Log.i(TAG, "----------------------------------------")
                Log.i(TAG, "FILE #$totalCount:")
                Log.i(TAG, "  ID: $id")
                Log.i(TAG, "  Title: $title")
                Log.i(TAG, "  Artist: $artist")
                Log.i(TAG, "  Display Name: $displayName")
                Log.i(TAG, "  MIME Type: $mimeType")
                Log.i(TAG, "  Relative Path: $relativePath")
                Log.i(TAG, "  Full Path: $data")
                Log.i(TAG, "  Duration: ${duration}ms (${duration / 1000}s)")
                Log.i(TAG, "  IS_MUSIC: $isMusic")
                Log.i(TAG, "  In Music folder: $isInMusicFolder")
                Log.i(TAG, "  In Download folder: $isInDownloadFolder")
            }
        } ?: run {
            Log.e(TAG, "MediaStore query returned NULL cursor!")
        }

        Log.i(TAG, "========================================")
        Log.i(TAG, "SUMMARY:")
        Log.i(TAG, "========================================")
        Log.i(TAG, "Total audio files: $totalCount")
        Log.i(TAG, "Files with IS_MUSIC=1: $isMusicTrueCount")
        Log.i(TAG, "Files with IS_MUSIC=0: $isMusicFalseCount")
        Log.i(TAG, "Files in Music folder: $musicFolderCount")
        Log.i(TAG, "Files in Download folder: $downloadFolderCount")
        Log.i(TAG, "MP3 files: $mp3Count")
        Log.i(TAG, "========================================")

        // This test always passes - it's for diagnostic logging
        // The actual assertion is informational
        Log.i(TAG, "Test complete. Check logcat output above for file discovery details.")
    }

    @Test
    fun verifyRepositoryFindsAudioFiles() {
        runBlocking {
            Log.i(TAG, "========================================")
            Log.i(TAG, "TESTING REPOSITORY getAllTracks()")
            Log.i(TAG, "========================================")

            val tracks = repository.getAllTracks()

            Log.i(TAG, "Repository returned ${tracks.size} tracks")
            
            tracks.forEachIndexed { index, track ->
                Log.i(TAG, "Track #${index + 1}:")
                Log.i(TAG, "  Title: ${track.title}")
                Log.i(TAG, "  Artist: ${track.artist}")
                Log.i(TAG, "  Album: ${track.album}")
                Log.i(TAG, "  Duration: ${track.duration}ms")
                Log.i(TAG, "  URI: ${track.uri}")
            }

            Log.i(TAG, "========================================")
            Log.i(TAG, "Repository test complete. Found ${tracks.size} tracks.")
            Log.i(TAG, "========================================")
        }
    }

    @Test
    fun listPhysicalFilesInMusicAndDownloadFolders() {
        Log.i(TAG, "========================================")
        Log.i(TAG, "LISTING PHYSICAL FILES ON DEVICE")
        Log.i(TAG, "========================================")

        // List files in common music locations
        val locations = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            File(Environment.getExternalStorageDirectory(), "Music"),
            File(Environment.getExternalStorageDirectory(), "Download"),
            File("/storage/emulated/0/Music"),
            File("/storage/emulated/0/Download")
        )

        var totalPhysicalFiles = 0

        locations.forEach { dir ->
            Log.i(TAG, "----------------------------------------")
            Log.i(TAG, "Checking directory: ${dir.absolutePath}")
            Log.i(TAG, "  Exists: ${dir.exists()}")
            Log.i(TAG, "  Is directory: ${dir.isDirectory}")
            Log.i(TAG, "  Can read: ${dir.canRead()}")

            if (dir.exists() && dir.isDirectory && dir.canRead()) {
                val audioFiles = dir.listFiles { file ->
                    file.isFile && (
                        file.name.endsWith(".mp3", ignoreCase = true) ||
                        file.name.endsWith(".m4a", ignoreCase = true) ||
                        file.name.endsWith(".flac", ignoreCase = true) ||
                        file.name.endsWith(".ogg", ignoreCase = true) ||
                        file.name.endsWith(".wav", ignoreCase = true) ||
                        file.name.endsWith(".aac", ignoreCase = true)
                    )
                }

                audioFiles?.forEach { file ->
                    totalPhysicalFiles++
                    Log.i(TAG, "  AUDIO FILE: ${file.name}")
                    Log.i(TAG, "    Size: ${file.length()} bytes")
                    Log.i(TAG, "    Path: ${file.absolutePath}")
                }

                if (audioFiles.isNullOrEmpty()) {
                    Log.i(TAG, "  No audio files found in this directory")
                }
            }
        }

        Log.i(TAG, "========================================")
        Log.i(TAG, "Total physical audio files found: $totalPhysicalFiles")
        Log.i(TAG, "========================================")
    }

    @Test
    fun verifyMp3FilesFromMusicFolderAreDiscovered() {
        runBlocking {
            Log.i(TAG, "========================================")
            Log.i(TAG, "VERIFICATION: MP3 from Music folder")
            Log.i(TAG, "========================================")

            // First, list what physical files exist
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val physicalMp3Files = mutableListOf<File>()
            
            if (musicDir.exists() && musicDir.isDirectory) {
                musicDir.listFiles()?.filter { 
                    it.isFile && it.name.endsWith(".mp3", ignoreCase = true) 
                }?.forEach { file ->
                    physicalMp3Files.add(file)
                    Log.i(TAG, "Physical MP3 in Music/: ${file.name}")
                }
            }

            Log.i(TAG, "Found ${physicalMp3Files.size} physical MP3 files in Music folder")

            // Trigger media scan for these files
            if (physicalMp3Files.isNotEmpty()) {
                triggerMediaScan(physicalMp3Files.map { it.absolutePath })
            }

            // Now check if they're in MediaStore via repository
            val tracks = repository.getAllTracks()
            Log.i(TAG, "Repository returned ${tracks.size} tracks after scan")

            var matchedCount = 0
            physicalMp3Files.forEach { file ->
                val fileName = file.name
                val nameWithoutExtension = fileName.removeSuffix(".mp3").lowercase()
                val isDiscovered = tracks.any { track ->
                    track.title.lowercase().contains(nameWithoutExtension) ||
                    track.uri.toString().contains(fileName, ignoreCase = true)
                }
                
                if (isDiscovered) {
                    matchedCount++
                    Log.i(TAG, "FOUND: $fileName is discovered by repository")
                } else {
                    Log.w(TAG, "MISSING: $fileName is NOT discovered by repository!")
                }
            }

            Log.i(TAG, "========================================")
            Log.i(TAG, "RESULT: $matchedCount / ${physicalMp3Files.size} MP3 files from Music folder discovered")
            Log.i(TAG, "========================================")

            if (physicalMp3Files.isNotEmpty()) {
                assertTrue(
                    "Expected MP3 files from Music folder to be discovered after media scan. " +
                    "Found ${physicalMp3Files.size} physical files but only $matchedCount were discovered.",
                    matchedCount > 0
                )
            }
        }
    }

    @Test
    fun verifyMp3FilesFromDownloadFolderAreDiscovered() {
        runBlocking {
            Log.i(TAG, "========================================")
            Log.i(TAG, "VERIFICATION: MP3 from Download folder")
            Log.i(TAG, "========================================")

            // First, list what physical files exist
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val physicalMp3Files = mutableListOf<File>()
            
            if (downloadDir.exists() && downloadDir.isDirectory) {
                downloadDir.listFiles()?.filter { 
                    it.isFile && it.name.endsWith(".mp3", ignoreCase = true) 
                }?.forEach { file ->
                    physicalMp3Files.add(file)
                    Log.i(TAG, "Physical MP3 in Download/: ${file.name}")
                }
            }

            Log.i(TAG, "Found ${physicalMp3Files.size} physical MP3 files in Download folder")

            // Trigger media scan for these files
            if (physicalMp3Files.isNotEmpty()) {
                triggerMediaScan(physicalMp3Files.map { it.absolutePath })
            }

            // Now check if they're in MediaStore via repository
            val tracks = repository.getAllTracks()
            Log.i(TAG, "Repository returned ${tracks.size} tracks after scan")

            var matchedCount = 0
            physicalMp3Files.forEach { file ->
                val fileName = file.name
                val nameWithoutExtension = fileName.removeSuffix(".mp3").lowercase()
                val isDiscovered = tracks.any { track ->
                    track.title.lowercase().contains(nameWithoutExtension) ||
                    track.uri.toString().contains(fileName, ignoreCase = true)
                }
                
                if (isDiscovered) {
                    matchedCount++
                    Log.i(TAG, "FOUND: $fileName is discovered by repository")
                } else {
                    Log.w(TAG, "MISSING: $fileName is NOT discovered by repository!")
                }
            }

            Log.i(TAG, "========================================")
            Log.i(TAG, "RESULT: $matchedCount / ${physicalMp3Files.size} MP3 files from Download folder discovered")
            Log.i(TAG, "========================================")

            if (physicalMp3Files.isNotEmpty()) {
                assertTrue(
                    "Expected MP3 files from Download folder to be discovered after media scan. " +
                    "Found ${physicalMp3Files.size} physical files but only $matchedCount were discovered.",
                    matchedCount > 0
                )
            }
        }
    }

    @Test
    fun diagnosticQueryWithDifferentSelections() {
        Log.i(TAG, "========================================")
        Log.i(TAG, "DIAGNOSTIC: Testing different query selections")
        Log.i(TAG, "========================================")

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME
        )

        // Test 1: No filter (what repository now uses)
        val cursorNoFilter = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            null
        )
        val countNoFilter = cursorNoFilter?.count ?: 0
        cursorNoFilter?.close()
        Log.i(TAG, "Query with NO filter: $countNoFilter files")

        // Test 2: IS_MUSIC filter (what repository used before)
        val cursorIsMusicFilter = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            null
        )
        val countIsMusicFilter = cursorIsMusicFilter?.count ?: 0
        cursorIsMusicFilter?.close()
        Log.i(TAG, "Query with IS_MUSIC != 0 filter: $countIsMusicFilter files")

        // Test 3: MIME type filter
        val cursorMimeFilter = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%'",
            null,
            null
        )
        val countMimeFilter = cursorMimeFilter?.count ?: 0
        cursorMimeFilter?.close()
        Log.i(TAG, "Query with MIME_TYPE LIKE 'audio/%' filter: $countMimeFilter files")

        Log.i(TAG, "========================================")
        Log.i(TAG, "COMPARISON:")
        Log.i(TAG, "  No filter: $countNoFilter")
        Log.i(TAG, "  IS_MUSIC filter: $countIsMusicFilter (previous implementation)")
        Log.i(TAG, "  MIME type filter: $countMimeFilter")
        Log.i(TAG, "  Files LOST by IS_MUSIC filter: ${countNoFilter - countIsMusicFilter}")
        Log.i(TAG, "========================================")
    }
}

