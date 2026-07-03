package com.anplak.androidmusic.data

data class ArtistRule(val name: String)

data class LibraryIndexPolicy(
    val maxDurationMs: Long = DEFAULT_MAX_INDEX_DURATION_MS,
    val folderRules: List<FolderRule> = emptyList(),
    val artistRules: List<ArtistRule> = emptyList()
) {
    companion object {
        const val DEFAULT_MAX_INDEX_DURATION_MS = 10 * 60 * 1000L
    }
}

enum class FolderRuleMode {
    INCLUDE,
    EXCLUDE
}

data class FolderRule(
    val path: String,
    val mode: FolderRuleMode
)
