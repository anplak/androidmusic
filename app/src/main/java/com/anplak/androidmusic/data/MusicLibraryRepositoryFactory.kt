package com.anplak.androidmusic.data

import android.content.Context
import com.anplak.androidmusic.data.db.AppDatabase

object MusicLibraryRepositoryFactory {

    fun create(context: Context): MusicLibraryRepositoryImpl {
        val appContext = context.applicationContext
        val database = AppDatabase.getInstance(appContext)
        val policyRepository = LibraryIndexPolicyRepository(
            preferences = SharedPreferencesLibraryIndexPreferences(appContext),
            folderRuleDao = database.indexFolderRuleDao()
        )
        return MusicLibraryRepositoryImpl(
            contentResolver = appContext.contentResolver,
            context = appContext,
            trackDao = database.trackDao(),
            policyRepository = policyRepository
        )
    }
}
