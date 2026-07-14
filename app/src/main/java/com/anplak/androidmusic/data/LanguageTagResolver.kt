package com.anplak.androidmusic.data

import android.database.Cursor

/**
 * Conservative language bucket resolver — returns null unless a tag source is explicit (NFR3).
 */
object LanguageTagResolver {
    private val KNOWN_BUCKETS = setOf(
        "en", "de", "fr", "es", "it", "pt", "nl", "pl", "ru", "ja", "ko", "zh"
    )

    /**
     * v1: no reliable MediaStore language column on all devices; returns null until tags exist.
     */
    @Suppress("UNUSED_PARAMETER")
    fun resolve(cursor: Cursor, filePath: String): String? = null

    internal fun normalize(raw: String?): String? =
        raw?.trim()?.lowercase()?.takeIf { it.isNotBlank() }

    internal fun isKnownBucket(bucket: String): Boolean = bucket in KNOWN_BUCKETS
}
