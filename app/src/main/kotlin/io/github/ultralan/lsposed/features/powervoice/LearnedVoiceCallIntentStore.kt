package io.github.ultralan.lsposed.features.powervoice

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import io.github.ultralan.lsposed.core.Config
import io.github.ultralan.lsposed.core.Log

object LearnedVoiceCallIntentStore {
    const val AUTHORITY = "${Config.APPLICATION_ID}.powervoice.learnedintent"
    const val PATH_LEARNED_INTENT = "learned_intent"
    const val COLUMN_PROVIDER_ID = "provider_id"
    const val COLUMN_INTENT_URI = "intent_uri"

    private const val PREF_KEY_PREFIX = "learned_voice_call_intent_uri."

    val URI: Uri = Uri.Builder()
        .scheme("content")
        .authority(AUTHORITY)
        .appendPath(PATH_LEARNED_INTENT)
        .build()

    fun uri(provider: VoiceAssistantProvider): Uri = uri(provider.id)

    fun uri(providerId: String): Uri = URI.buildUpon()
        .appendPath(providerId)
        .build()

    fun providerId(uri: Uri): String? {
        if (uri.authority != AUTHORITY) return null
        if (uri.pathSegments.firstOrNull() != PATH_LEARNED_INTENT) return null
        return uri.pathSegments.getOrNull(1)?.takeIf { it.isNotBlank() }
    }

    fun publish(context: Context, provider: VoiceAssistantProvider, intent: Intent): Boolean =
        runCatching {
            val values = ContentValues().apply {
                put(COLUMN_PROVIDER_ID, provider.id)
                put(COLUMN_INTENT_URI, IntentStore.encode(intent))
            }
            val updated = context.contentResolver.update(uri(provider), values, null, null)
            updated > 0 || context.contentResolver.insert(uri(provider), values) != null
        }.onFailure {
            Log.e("同步已学习的 ${provider.displayName} 语音通话 Intent 失败", it)
        }.getOrDefault(false)

    fun query(context: Context, provider: VoiceAssistantProvider): Intent? =
        runCatching {
            context.contentResolver.query(uri(provider), arrayOf(COLUMN_INTENT_URI), null, null, null)
                ?.use { cursor -> cursor.firstStringOrNull(COLUMN_INTENT_URI) }
                ?.let { IntentStore.decode(it) }
        }.onFailure {
            Log.e("查询已学习的 ${provider.displayName} 语音通话 Intent 失败", it)
        }.getOrNull()

    fun queryOrMigrateLocal(context: Context, provider: VoiceAssistantProvider): Intent? =
        query(context, provider)
            ?: IntentStore.load(context, provider)
                ?.also { publish(context, provider, it) }

    fun saveEncoded(context: Context, providerId: String, encodedIntent: String): Boolean =
        context.getSharedPreferences(PowerVoiceConfig.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(prefKey(providerId), encodedIntent)
            .commit()

    fun loadEncoded(context: Context, providerId: String): String? =
        context.getSharedPreferences(PowerVoiceConfig.PREFS_NAME, Context.MODE_PRIVATE)
            .getString(prefKey(providerId), null)

    fun clear(context: Context, provider: VoiceAssistantProvider): Boolean = clear(context, provider.id)

    fun clear(context: Context, providerId: String): Boolean =
        context.getSharedPreferences(PowerVoiceConfig.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(prefKey(providerId))
            .commit()

    private fun prefKey(providerId: String): String = PREF_KEY_PREFIX + providerId

    private fun Cursor.firstStringOrNull(columnName: String): String? {
        if (!moveToFirst()) return null
        val columnIndex = getColumnIndex(columnName)
        if (columnIndex < 0 || isNull(columnIndex)) return null
        return getString(columnIndex)
    }
}
