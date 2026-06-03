package com.anplak.androidmusic.data

import android.database.Cursor
import android.os.Environment
import android.provider.MediaStore

object MediaStorePathResolver {

    fun resolveFilePath(cursor: Cursor, dataColumn: Int, relativePathColumn: Int, displayNameColumn: Int): String {
        val dataPath = if (dataColumn >= 0) cursor.getString(dataColumn).orEmpty() else ""
        if (dataPath.isNotBlank()) return dataPath

        val relativePath = if (relativePathColumn >= 0) cursor.getString(relativePathColumn).orEmpty() else ""
        val displayName = if (displayNameColumn >= 0) cursor.getString(displayNameColumn).orEmpty() else ""
        if (relativePath.isBlank() || displayName.isBlank()) return ""

        val storageRoot = Environment.getExternalStorageDirectory().absolutePath
        val separator = if (relativePath.endsWith("/")) "" else "/"
        return "$storageRoot/$relativePath$separator$displayName"
    }
}
