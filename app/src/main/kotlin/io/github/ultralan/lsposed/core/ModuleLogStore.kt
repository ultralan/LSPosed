package io.github.ultralan.lsposed.core

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ModuleLogEntry(
    val timestamp: Long,
    val source: String,
    val message: String,
) {
    fun displayText(): String =
        "${timeFormat.format(Date(timestamp))} [$source] $message"

    private companion object {
        val timeFormat = SimpleDateFormat("MM-dd HH:mm:ss", Locale.CHINA)
    }
}

object ModuleLogStore {
    const val MAX_ENTRIES = 80
    const val AUTHORITY = "${Config.APPLICATION_ID}.logs"
    const val PATH_ENTRIES = "entries"
    const val COLUMN_TIMESTAMP = "timestamp"
    const val COLUMN_SOURCE = "source"
    const val COLUMN_MESSAGE = "message"

    private const val PREFS_NAME = "module_logs"
    private const val PREF_KEY_ENTRIES = "entries"
    private const val FIELD_SEPARATOR = "\u001f"
    private const val ENTRY_SEPARATOR = "\u001e"

    val URI: Uri = Uri.Builder()
        .scheme("content")
        .authority(AUTHORITY)
        .appendPath(PATH_ENTRIES)
        .build()

    fun append(context: Context, source: String, message: String) {
        appendLocal(context, ModuleLogEntry(System.currentTimeMillis(), source, message))
    }

    fun appendLocal(context: Context, entry: ModuleLogEntry) {
        val entries = (listOf(entry) + load(context)).take(MAX_ENTRIES)
        save(context, entries)
    }

    fun appendThroughProvider(context: Context, source: String, message: String): Boolean =
        runCatching {
            context.contentResolver.insert(
                URI,
                ContentValues().apply {
                    put(COLUMN_TIMESTAMP, System.currentTimeMillis())
                    put(COLUMN_SOURCE, source)
                    put(COLUMN_MESSAGE, message)
                },
            ) != null
        }.getOrDefault(false)

    fun load(context: Context): List<ModuleLogEntry> =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREF_KEY_ENTRIES, null)
            ?.takeIf { it.isNotBlank() }
            ?.split(ENTRY_SEPARATOR)
            ?.mapNotNull { encoded -> decode(encoded) }
            ?: emptyList()

    fun clear(context: Context) {
        save(context, emptyList())
    }

    private fun save(context: Context, entries: List<ModuleLogEntry>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_KEY_ENTRIES, entries.joinToString(ENTRY_SEPARATOR) { encode(it) })
            .commit()
    }

    private fun encode(entry: ModuleLogEntry): String =
        listOf(entry.timestamp.toString(), entry.source, entry.message)
            .joinToString(FIELD_SEPARATOR) { Uri.encode(it) }

    private fun decode(encoded: String): ModuleLogEntry? {
        val parts = encoded.split(FIELD_SEPARATOR)
        if (parts.size != 3) return null
        val timestamp = Uri.decode(parts[0]).toLongOrNull() ?: return null
        return ModuleLogEntry(
            timestamp = timestamp,
            source = Uri.decode(parts[1]),
            message = Uri.decode(parts[2]),
        )
    }
}
