package io.github.ultralan.lsposed.features.powervoice

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

class LearnedVoiceCallIntentContentProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        val providerId = LearnedVoiceCallIntentStore.providerId(uri) ?: return null
        val encodedIntent = context?.let { LearnedVoiceCallIntentStore.loadEncoded(it, providerId) }
        return MatrixCursor(
            arrayOf(
                LearnedVoiceCallIntentStore.COLUMN_PROVIDER_ID,
                LearnedVoiceCallIntentStore.COLUMN_INTENT_URI,
            ),
        ).apply {
            if (encodedIntent != null) addRow(arrayOf(providerId, encodedIntent))
        }
    }

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val contentValues = values ?: return null
        val providerId = contentValues.getAsString(LearnedVoiceCallIntentStore.COLUMN_PROVIDER_ID)
            ?: LearnedVoiceCallIntentStore.providerId(uri)
            ?: return null
        val encodedIntent = contentValues.getAsString(LearnedVoiceCallIntentStore.COLUMN_INTENT_URI)
            ?: return null
        val saved = context?.let {
            LearnedVoiceCallIntentStore.saveEncoded(it, providerId, encodedIntent)
        } ?: false
        return if (saved) LearnedVoiceCallIntentStore.uri(providerId) else null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val providerId = LearnedVoiceCallIntentStore.providerId(uri) ?: return 0
        return if (context?.let { LearnedVoiceCallIntentStore.clear(it, providerId) } == true) 1 else 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int {
        val contentValues = values ?: return 0
        val providerId = contentValues.getAsString(LearnedVoiceCallIntentStore.COLUMN_PROVIDER_ID)
            ?: LearnedVoiceCallIntentStore.providerId(uri)
            ?: return 0
        val encodedIntent = contentValues.getAsString(LearnedVoiceCallIntentStore.COLUMN_INTENT_URI)
            ?: return 0
        val saved = context?.let {
            LearnedVoiceCallIntentStore.saveEncoded(it, providerId, encodedIntent)
        } ?: false
        return if (saved) 1 else 0
    }
}
