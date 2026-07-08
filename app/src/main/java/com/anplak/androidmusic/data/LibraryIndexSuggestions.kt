package com.anplak.androidmusic.data

import java.io.File

object LibraryIndexSuggestions {

    const val UNKNOWN_ARTIST_LABEL = "Unknown Artist"

    /**
     * Derives indexable folder paths from track file paths and optional on-device roots.
     * Returns parent directories sorted alphabetically, excluding paths already ruled.
     */
    fun discoverFoldersFromTracks(
        trackPaths: List<String>,
        existingRulePaths: Set<String> = emptySet()
    ): List<String> {
        val excluded = existingRulePaths.map { LibraryIndexFilter.normalizePath(it) }.toSet()
        return trackPaths
            .mapNotNull { path ->
                val parent = File(path).parent ?: return@mapNotNull null
                parent.takeIf { it.isNotBlank() }
            }
            .distinct()
            .filter { LibraryIndexFilter.normalizePath(it) !in excluded }
            .sortedBy { it.lowercase() }
    }

    /** Lists immediate subdirectories under [roots] that exist on disk. */
    fun discoverSubfolders(roots: List<File>): List<String> {
        return roots
            .filter { it.exists() && it.isDirectory }
            .flatMap { root ->
                root.listFiles()
                    ?.filter { it.isDirectory }
                    ?.map { it.absolutePath }
                    .orEmpty()
            }
            .distinct()
            .sortedBy { it.lowercase() }
    }

    fun mergeFolderSuggestions(
        fromTracks: List<String>,
        subfolders: List<String>,
        presetRoots: List<String>,
        existingRulePaths: Set<String> = emptySet()
    ): List<String> {
        val excluded = existingRulePaths.map { LibraryIndexFilter.normalizePath(it) }.toSet()
        return (fromTracks + subfolders + presetRoots)
            .distinct()
            .filter { LibraryIndexFilter.normalizePath(it) !in excluded }
            .sortedBy { it.lowercase() }
    }
}
