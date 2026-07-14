package com.anplak.androidmusic.data

/**
 * Derives user-defined taxonomy tags from on-device folder layout (story 16).
 */
object FolderTagExtractor {
    private val GENERIC_ROOTS = setOf("music", "download", "downloads", "audio", "media")
    private val LIBRARY_ROOT_MARKERS = listOf("/Music/", "/Download/", "/Downloads/")

    fun primaryTag(filePath: String): String? = extractTags(filePath).firstOrNull()

    fun extractTags(filePath: String): List<String> {
        if (filePath.isBlank()) return emptyList()

        val relativePath = stripToLibraryRelative(filePath.replace('\\', '/'))
        val segments = relativePath.split('/')
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.contains('.') }
        if (segments.size <= 2) return emptyList()

        val withoutLeaf = when {
            segments.size >= 4 -> segments.dropLast(2)
            segments.size == 3 -> segments.dropLast(1)
            else -> segments
        }

        return withoutLeaf
            .map { it.lowercase() }
            .filter { it !in GENERIC_ROOTS }
            .distinct()
    }

    private fun stripToLibraryRelative(path: String): String {
        for (marker in LIBRARY_ROOT_MARKERS) {
            val index = path.indexOf(marker, ignoreCase = true)
            if (index >= 0) {
                return path.substring(index + 1)
            }
        }
        return path.trimStart('/')
    }
}
