package com.anplak.androidmusic.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.anplak.androidmusic.data.db.AppDatabase
import com.anplak.androidmusic.data.db.IndexFolderRuleEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LibraryIndexPolicyRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: LibraryIndexPolicyRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LibraryIndexPolicyRepository(
            SharedPreferencesLibraryIndexPreferences(context),
            database.indexFolderRuleDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `default max duration is ten minutes`() = runTest {
        val policy = repository.loadPolicy()
        assertEquals(LibraryIndexPolicy.DEFAULT_MAX_INDEX_DURATION_MS, policy.maxDurationMs)
    }

    @Test
    fun `folder rules persist and load`() = runTest {
        repository.addFolderRule("/storage/emulated/0/Music/Podcasts", FolderRuleMode.EXCLUDE)

        val policy = repository.loadPolicy()

        assertEquals(1, policy.folderRules.size)
        assertEquals(FolderRuleMode.EXCLUDE, policy.folderRules.first().mode)
        assertEquals("/storage/emulated/0/Music/Podcasts", policy.folderRules.first().path)
    }

    @Test
    fun `remove folder rule updates policy`() = runTest {
        database.indexFolderRuleDao().insert(
            IndexFolderRuleEntity(
                path = "/storage/emulated/0/Download",
                mode = FolderRuleMode.EXCLUDE.name
            )
        )

        repository.removeFolderRule("/storage/emulated/0/Download")

        assertEquals(0, repository.loadPolicy().folderRules.size)
    }
}
