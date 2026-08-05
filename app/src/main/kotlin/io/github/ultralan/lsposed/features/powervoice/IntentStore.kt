package io.github.ultralan.lsposed.features.powervoice

import android.content.Context
import android.content.Intent
import io.github.ultralan.lsposed.core.Log

object IntentStore {
    fun encode(intent: Intent): String = intent.toUri(Intent.URI_INTENT_SCHEME)

    fun decode(encoded: String): Intent = Intent.parseUri(encoded, Intent.URI_INTENT_SCHEME)

    fun save(context: Context, intent: Intent) {
        save(context, VoiceAssistantProviders.defaultProvider, intent)
    }

    fun save(context: Context, provider: VoiceAssistantProvider, intent: Intent) {
        context.getSharedPreferences(PowerVoiceConfig.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(prefKey(provider), encode(intent))
            .apply()
    }

    fun load(context: Context): Intent? {
        return load(context, VoiceAssistantProviders.defaultProvider)
    }

    fun load(context: Context, provider: VoiceAssistantProvider): Intent? {
        val prefs = context.getSharedPreferences(PowerVoiceConfig.PREFS_NAME, Context.MODE_PRIVATE)
        val encoded = prefs.getString(prefKey(provider), null)
            ?: legacyEncodedIntent(prefs, provider)
            ?: return null

        return runCatching { decode(encoded) }
            .onFailure { Log.e("读取已学习 Intent 失败", it) }
            .getOrNull()
    }

    private fun prefKey(provider: VoiceAssistantProvider): String =
        "${PowerVoiceConfig.PREF_KEY_LAST_VOICE_CALL_INTENT_URI}.${provider.id}"

    private fun legacyEncodedIntent(
        prefs: android.content.SharedPreferences,
        provider: VoiceAssistantProvider,
    ): String? {
        if (provider.id != VoiceAssistantProviders.defaultProvider.id) return null
        return prefs.getString(PowerVoiceConfig.PREF_KEY_LAST_VOICE_CALL_INTENT_URI, null)
    }
}
