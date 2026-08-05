package io.github.ultralan.lsposed.core

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

class ModuleLogContentProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (uri != ModuleLogStore.URI || values == null) return null
        val appContext = context ?: return null
        val entry = ModuleLogEntry(
            timestamp = values.getAsLong(ModuleLogStore.COLUMN_TIMESTAMP) ?: System.currentTimeMillis(),
            source = values.getAsString(ModuleLogStore.COLUMN_SOURCE) ?: "模块",
            message = values.getAsString(ModuleLogStore.COLUMN_MESSAGE) ?: return null,
        )
        ModuleLogStore.appendLocal(appContext, entry)
        return uri
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        if (uri != ModuleLogStore.URI) return 0
        context?.let { ModuleLogStore.clear(it) }
        return 1
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
}
