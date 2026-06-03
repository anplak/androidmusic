package com.anplak.androidmusic.data

import com.anplak.androidmusic.data.db.IndexFolderRuleDao
import com.anplak.androidmusic.data.db.IndexFolderRuleEntity

class LibraryIndexPolicyRepository(
    private val preferences: LibraryIndexPreferences,
    private val folderRuleDao: IndexFolderRuleDao
) {
    suspend fun loadPolicy(): LibraryIndexPolicy {
        return LibraryIndexPolicy(
            maxDurationMs = preferences.getMaxDurationMs(),
            folderRules = folderRuleDao.getAll().map { it.toFolderRule() }
        )
    }

    suspend fun getFolderRules(): List<FolderRule> {
        return folderRuleDao.getAll().map { it.toFolderRule() }
    }

    suspend fun addFolderRule(path: String, mode: FolderRuleMode) {
        val stored = path.trim().trimEnd('/')
        if (stored.isEmpty()) return
        folderRuleDao.insert(
            IndexFolderRuleEntity(
                path = stored,
                mode = mode.name
            )
        )
    }

    suspend fun removeFolderRule(path: String) {
        folderRuleDao.deleteByPath(path.trim().trimEnd('/'))
    }

    fun getMaxDurationMs(): Long = preferences.getMaxDurationMs()

    fun setMaxDurationMs(durationMs: Long) {
        preferences.setMaxDurationMs(durationMs)
    }

    private fun IndexFolderRuleEntity.toFolderRule(): FolderRule {
        return FolderRule(
            path = path,
            mode = FolderRuleMode.valueOf(mode)
        )
    }
}
