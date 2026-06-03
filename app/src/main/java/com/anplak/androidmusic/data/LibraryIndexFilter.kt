package com.anplak.androidmusic.data

object LibraryIndexFilter {

    fun shouldIndex(filePath: String, durationMs: Long, policy: LibraryIndexPolicy): Boolean {
        if (durationMs <= 0 || durationMs > policy.maxDurationMs) return false

        val normalized = normalizePath(filePath)
        if (normalized.isEmpty()) {
            val includes = policy.folderRules.filter { it.mode == FolderRuleMode.INCLUDE }
            return includes.isEmpty()
        }

        val excludes = policy.folderRules.filter { it.mode == FolderRuleMode.EXCLUDE }
        if (excludes.any { normalized.startsWith(normalizePath(it.path)) }) return false

        val includes = policy.folderRules.filter { it.mode == FolderRuleMode.INCLUDE }
        if (includes.isEmpty()) return true
        return includes.any { normalized.startsWith(normalizePath(it.path)) }
    }

    fun normalizePath(path: String): String {
        val trimmed = path.trim().trimEnd('/')
        if (trimmed.isEmpty()) return ""
        return trimmed.lowercase()
    }
}
