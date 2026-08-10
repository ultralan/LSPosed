package io.github.ultralan.lsposed.features.powervoice

import android.content.Context
import android.database.Cursor
import android.net.Uri
import io.github.ultralan.lsposed.core.Config
import io.github.ultralan.lsposed.core.Log

object TargetProviderStore {
    const val SYSTEM_DEFAULT_ID = "system_default"
    const val AUTHORITY = "${Config.APPLICATION_ID}.powervoice.targetprovider"
    const val PATH_TARGET_PROVIDER = "target_provider"
    const val COLUMN_PROVIDER_ID = "provider_id"
    val URI: Uri = Uri.Builder()
        .scheme("content")
        .authority(AUTHORITY)
        .appendPath(PATH_TARGET_PROVIDER)
        .build()

    fun save(context: Context, providerId: String) {
        context.getSharedPreferences(PowerVoiceConfig.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PowerVoiceConfig.PREF_KEY_TARGET_PROVIDER_ID, providerId)
            .commit()
        ReadablePreferences.makeTargetProviderPreferencesReadable(context)
    }

    fun load(context: Context): String? =
        context.getSharedPreferences(PowerVoiceConfig.PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PowerVoiceConfig.PREF_KEY_TARGET_PROVIDER_ID, null)

    fun isSystemDefault(providerId: String?): Boolean = providerId == SYSTEM_DEFAULT_ID

    fun query(context: Context): String? =
        runCatching {
            context.contentResolver.query(URI, arrayOf(COLUMN_PROVIDER_ID), null, null, null)
                ?.use { cursor -> cursor.firstStringOrNull(COLUMN_PROVIDER_ID) }
        }.onFailure {
            Log.e("查询目标 Provider 失败", it)
        }.getOrNull()

    private fun Cursor.firstStringOrNull(columnName: String): String? {
        if (!moveToFirst()) return null
        val columnIndex = getColumnIndex(columnName)
        if (columnIndex < 0 || isNull(columnIndex)) return null
        return getString(columnIndex)
    }
}
