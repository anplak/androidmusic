package com.anplak.androidmusic.data

object LibraryIndexFilter {

    fun normalizeArtist(name: String): String = name.trim().lowercase()

    fun skipReason(
        filePath: String,
        durationMs: Long,
        artist: String,
        policy: LibraryIndexPolicy
    ): IndexSkipReason? {
        if (durationMs <= 0 || durationMs > policy.maxDurationMs) return IndexSkipReason.DURATION

        val normalized = normalizePath(filePath)
        if (normalized.isEmpty()) {
            val includes = policy.folderRules.filter { it.mode == FolderRuleMode.INCLUDE }
            if (includes.isNotEmpty()) return IndexSkipReason.FOLDER
        } else {
            val excludes = policy.folderRules.filter { it.mode == FolderRuleMode.EXCLUDE }
            if (excludes.any { normalized.startsWith(normalizePath(it.path)) }) {
                return IndexSkipReason.FOLDER
            }
            val includes = policy.folderRules.filter { it.mode == FolderRuleMode.INCLUDE }
            if (includes.isNotEmpty() &&
                !includes.any { normalized.startsWith(normalizePath(it.path)) }
            ) {
                return IndexSkipReason.FOLDER
            }
        }

        val normalizedArtist = normalizeArtist(artist)
        if (normalizedArtist.isNotEmpty() &&
            policy.artistRules.any { normalizeArtist(it.name) == normalizedArtist }
        ) {
            return IndexSkipReason.ARTIST
        }
        return null
    }

    fun shouldIndex(
        filePath: String,
        durationMs: Long,
        artist: String,
        policy: LibraryIndexPolicy
    ): Boolean = skipReason(filePath, durationMs, artist, policy) == null

    fun normalizePath(path: String): String {
        val trimmed = path.trim().trimEnd('/')
        if (trimmed.isEmpty()) return ""
        return trimmed.lowercase()
    }
}
